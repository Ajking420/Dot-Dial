package com.example.data.system

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
private const val BACKUP_FOLDER_NAME = "Dot Dial Backups"

sealed interface DriveAuthorizationOutcome {
    data class Authorized(val accessToken: String) : DriveAuthorizationOutcome
    data class NeedsConsent(val pendingIntent: PendingIntent) : DriveAuthorizationOutcome
    data class Failed(val message: String) : DriveAuthorizationOutcome
}

data class DriveUserInfo(
    val displayName: String,
    val email: String
)

data class DriveBackupFile(
    val id: String,
    val name: String,
    val modifiedTime: String,
    val sizeBytes: Long
)

class GoogleDriveBackupManager(private val context: Context) {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
            .build()
    }

    suspend fun authorizeDriveAccess(): DriveAuthorizationOutcome =
        suspendCancellableCoroutine { cont ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()

            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { result ->
                    if (!cont.isActive) return@addOnSuccessListener
                    val pending = result.pendingIntent
                    val token = result.accessToken
                    when {
                        result.hasResolution() && pending != null ->
                            cont.resume(DriveAuthorizationOutcome.NeedsConsent(pending))
                        !token.isNullOrBlank() ->
                            cont.resume(DriveAuthorizationOutcome.Authorized(token))
                        else ->
                            cont.resume(
                                DriveAuthorizationOutcome.Failed(
                                    "Google Drive did not return an access token."
                                )
                            )
                    }
                }
                .addOnFailureListener { err ->
                    if (cont.isActive) {
                        cont.resume(
                            DriveAuthorizationOutcome.Failed(
                                err.localizedMessage ?: "Google Drive authorization failed."
                            )
                        )
                    }
                }
        }

    fun extractTokenFromConsentResult(intent: Intent?): Result<String> {
        if (intent == null) {
            return Result.failure(IllegalStateException("Google Drive authorization was cancelled."))
        }
        return runCatching {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(intent)
            result.accessToken
                ?: throw IllegalStateException("No Drive access token returned after consent.")
        }
    }

    suspend fun fetchUserInfo(accessToken: String): Result<DriveUserInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/about?fields=user(displayName,emailAddress)")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Drive profile lookup failed (${resp.code}): $body")
                }
                val user = JSONObject(body).optJSONObject("user") ?: JSONObject()
                DriveUserInfo(
                    displayName = user.optString("displayName", "").trim(),
                    email = user.optString("emailAddress", "").trim()
                )
            }
        }
    }

    suspend fun uploadJsonBackup(
        accessToken: String,
        fileName: String,
        jsonContent: String
    ): Result<DriveBackupFile> = withContext(Dispatchers.IO) {
        runCatching {
            val folderId = ensureBackupFolderId(accessToken)
            val existingId = findFileIdInFolder(accessToken, folderId, fileName)

            val metadata = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "application/json")
                if (existingId == null) {
                    put("parents", org.json.JSONArray(listOf(folderId)))
                }
            }

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(
                    metadata.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .addPart(
                    jsonContent.toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .build()

            val url = if (existingId == null) {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,modifiedTime,size"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=multipart&fields=id,name,modifiedTime,size"
            }

            val reqBuilder = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")

            val req = if (existingId == null) {
                reqBuilder.post(multipartBody).build()
            } else {
                reqBuilder.patch(multipartBody).build()
            }

            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Drive upload failed (${resp.code}): $body")
                }
                val obj = JSONObject(body)
                DriveBackupFile(
                    id = obj.getString("id"),
                    name = obj.optString("name", fileName),
                    modifiedTime = obj.optString("modifiedTime", ""),
                    sizeBytes = obj.optString("size", "0").toLongOrNull() ?: 0L
                )
            }
        }
    }

    suspend fun listBackups(accessToken: String): Result<List<DriveBackupFile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val folderId = findExistingFolderId(accessToken) ?: return@runCatching emptyList()
                val query = "'$folderId' in parents and trashed = false"
                val encodedQ = URLEncoder.encode(query, "UTF-8")
                val req = Request.Builder()
                    .url(
                        "https://www.googleapis.com/drive/v3/files?q=$encodedQ" +
                            "&orderBy=modifiedTime%20desc&fields=files(id,name,modifiedTime,size)"
                    )
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Drive list failed (${resp.code}): $body")
                    }
                    val arr = JSONObject(body).optJSONArray("files") ?: return@runCatching emptyList()
                    buildList {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            add(
                                DriveBackupFile(
                                    id = obj.getString("id"),
                                    name = obj.optString("name", "backup.json"),
                                    modifiedTime = obj.optString("modifiedTime", ""),
                                    sizeBytes = obj.optString("size", "0").toLongOrNull() ?: 0L
                                )
                            )
                        }
                    }
                }
            }
        }

    suspend fun downloadBackupContent(accessToken: String, fileId: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Drive download failed (${resp.code}): $body")
                    }
                    body
                }
            }
        }

    private fun findExistingFolderId(accessToken: String): String? {
        val q = "name = '$BACKUP_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val encodedQ = URLEncoder.encode(q, "UTF-8")
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=$encodedQ&fields=files(id,name)")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string().orEmpty()
            val files = JSONObject(body).optJSONArray("files") ?: return null
            return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
        }
    }

    private fun ensureBackupFolderId(accessToken: String): String {
        findExistingFolderId(accessToken)?.let { return it }
        val payload = JSONObject().apply {
            put("name", BACKUP_FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }.toString()
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id")
            .header("Authorization", "Bearer $accessToken")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Unable to create '$BACKUP_FOLDER_NAME' folder (${resp.code}): $body")
            }
            return JSONObject(body).getString("id")
        }
    }

    private fun findFileIdInFolder(accessToken: String, folderId: String, fileName: String): String? {
        val safeName = fileName.replace("'", "\\'")
        val q = "name = '$safeName' and '$folderId' in parents and trashed = false"
        val encodedQ = URLEncoder.encode(q, "UTF-8")
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=$encodedQ&fields=files(id)")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string().orEmpty()
            val files = JSONObject(body).optJSONArray("files") ?: return null
            return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
        }
    }

    companion object {
        const val DRIVE_FOLDER_DISPLAY_NAME = BACKUP_FOLDER_NAME
        const val FILE_CONTACTS_AND_HISTORY = "dot_dial_contacts_history.json"
        const val FILE_SETTINGS_ONLY = "dot_dial_settings.json"
        const val FILE_FULL_BACKUP = "dot_dial_full_backup.json"
    }
}
