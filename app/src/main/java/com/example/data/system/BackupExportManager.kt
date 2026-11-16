package com.example.data.system

import android.content.Context
import android.net.Uri
import com.example.data.local.CallDirection
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.preferences.DialerPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RestoreSummary(
    val restoredContacts: Int,
    val restoredCallLogs: Int,
    val restoredSettingsCount: Int = 0
)

class BackupExportManager(
    private val context: Context,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao,
    private val preferences: DialerPreferences
) {
    suspend fun buildBackupJsonString(
        includeData: Boolean = true,
        includeSettings: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val contactsArr = JSONArray()
        val logsArr = JSONArray()

        if (includeData) {
            val contacts = contactDao.getAllOnce()
            val logs = callLogDao.getAllOnce()

            contacts.forEach { c ->
                contactsArr.put(
                    JSONObject().apply {
                        put("name", c.name)
                        put("phoneNumber", c.phoneNumber)
                        put("company", c.company)
                        put("email", c.email)
                        put("photoUri", c.photoUri.orEmpty())
                        put("callingCardUri", c.callingCardUri.orEmpty())
                        put("videoBackdropUri", c.videoBackdropUri.orEmpty())
                        put("backdropPhotoUri", c.backdropPhotoUri.orEmpty())
                        put("callerBackdropMode", c.callerBackdropMode)
                        put("vibrationPatternId", c.vibrationPatternId)
                        put("isFavorite", c.isFavorite)
                        put("isBlocked", c.isBlocked)
                        put("notes", c.notes)
                        put("accountName", c.accountName)
                        put("additionalNumbersJson", c.additionalNumbersJson)
                    }
                )
            }

            logs.forEach { l ->
                logsArr.put(
                    JSONObject().apply {
                        put("displayName", l.displayName)
                        put("phoneNumber", l.phoneNumber)
                        put("direction", l.direction.name)
                        put("timestamp", l.timestamp)
                        put("durationSeconds", l.durationSeconds)
                        put("isSpam", l.isSpam)
                        put("spamReason", l.spamReason)
                    }
                )
            }
        }

        val root = JSONObject().apply {
            put("version", 3)
            put("app", "Dot Dial")
            put("exportedAt", System.currentTimeMillis())
            put("includesData", includeData)
            put("includesSettings", includeSettings)
            if (includeData) {
                put("contacts", contactsArr)
                put("callLogs", logsArr)
            }
            if (includeSettings) {
                val current = preferences.currentSettingsOnce()
                put("settings", preferences.encodeSettingsJson(current))
            }
        }
        root.toString(2)
    }

    suspend fun restoreFromJsonString(
        raw: String,
        restoreData: Boolean = true,
        restoreSettings: Boolean = true
    ): RestoreSummary = withContext(Dispatchers.IO) {
        val root = JSONObject(raw)
        var contactsRestored = 0
        var logsRestored = 0

        if (restoreData) {
            val contactsArr = root.optJSONArray("contacts") ?: JSONArray()
            val logsArr = root.optJSONArray("callLogs") ?: JSONArray()

            for (i in 0 until contactsArr.length()) {
                val obj = contactsArr.optJSONObject(i) ?: continue
                val name = obj.optString("name").trim()
                val phone = obj.optString("phoneNumber").trim()
                if (name.isEmpty() || phone.isEmpty()) continue
                val normalized = T9Utils.normalizePhone(phone)
                val existing = contactDao.findByNormalizedNumber(normalized)
                contactDao.upsert(
                    ContactEntity(
                        id = existing?.id ?: 0L,
                        name = name,
                        phoneNumber = phone,
                        normalizedNumber = normalized,
                        t9Key = T9Utils.toT9Key(name),
                        company = obj.optString("company"),
                        email = obj.optString("email"),
                        photoUri = obj.optString("photoUri").takeIf { it.isNotBlank() },
                        callingCardUri = obj.optString("callingCardUri").takeIf { it.isNotBlank() },
                        videoBackdropUri = obj.optString("videoBackdropUri").takeIf { it.isNotBlank() },
                        backdropPhotoUri = obj.optString("backdropPhotoUri").takeIf { it.isNotBlank() },
                        callerBackdropMode = obj.optString("callerBackdropMode", ""),
                        vibrationPatternId = obj.optLong("vibrationPatternId", 1L),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isBlocked = obj.optBoolean("isBlocked", false),
                        notes = obj.optString("notes"),
                        accountName = obj.optString("accountName"),
                        additionalNumbersJson = obj.optString("additionalNumbersJson", "[]").ifBlank { "[]" }
                    )
                )
                contactsRestored++
            }

            val newLogs = mutableListOf<CallLogEntity>()
            for (i in 0 until logsArr.length()) {
                val obj = logsArr.optJSONObject(i) ?: continue
                val phone = obj.optString("phoneNumber").trim()
                if (phone.isEmpty()) continue
                val normalized = T9Utils.normalizePhone(phone)
                val direction = runCatching {
                    CallDirection.valueOf(obj.optString("direction", CallDirection.OUTGOING.name))
                }.getOrDefault(CallDirection.OUTGOING)
                newLogs.add(
                    CallLogEntity(
                        displayName = obj.optString("displayName", phone),
                        phoneNumber = phone,
                        normalizedNumber = normalized,
                        direction = direction,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        durationSeconds = obj.optInt("durationSeconds", 0),
                        isSpam = obj.optBoolean("isSpam", false),
                        spamReason = obj.optString("spamReason")
                    )
                )
                logsRestored++
            }
            if (newLogs.isNotEmpty()) {
                callLogDao.insertAll(newLogs)
            }
        }

        val restoredSettingsCount = if (restoreSettings) {
            val settingsObj = root.optJSONObject("settings")
                ?: root.takeIf { it.has("themePreset") || it.has("aiAssistantEnabled") }
            if (settingsObj != null) preferences.applySettingsJson(settingsObj) else 0
        } else {
            0
        }

        RestoreSummary(
            restoredContacts = contactsRestored,
            restoredCallLogs = logsRestored,
            restoredSettingsCount = restoredSettingsCount
        )
    }

    suspend fun exportBackupJson(
        targetUri: Uri,
        includeData: Boolean = true,
        includeSettings: Boolean = true
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildBackupJsonString(includeData = includeData, includeSettings = includeSettings)
            val root = JSONObject(json)
            val contactsCount = root.optJSONArray("contacts")?.length() ?: 0
            val logsCount = root.optJSONArray("callLogs")?.length() ?: 0
            val settingsCount = root.optJSONObject("settings")?.length() ?: 0

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Unable to open backup destination.")

            RestoreSummary(contactsCount, logsCount, settingsCount)
        }
    }

    suspend fun exportSettingsJson(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildBackupJsonString(includeData = false, includeSettings = true)
            val settingsCount = JSONObject(json).optJSONObject("settings")?.length() ?: 0
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Unable to open settings export file.")
            settingsCount
        }
    }

    suspend fun exportContactsVcf(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val contacts = contactDao.getAllOnce()
            val vcf = buildString {
                contacts.forEach { c ->
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    appendLine("FN:${c.name}")
                    c.allPhoneNumbers().forEach { entry ->
                        appendLine("TEL;TYPE=${entry.label.uppercase()}:${entry.number}")
                    }
                    if (c.email.isNotBlank()) appendLine("EMAIL:${c.email}")
                    if (c.company.isNotBlank()) appendLine("ORG:${c.company}")
                    appendLine("END:VCARD")
                }
            }
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(vcf.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Unable to open VCF destination.")
            contacts.size
        }
    }

    suspend fun restoreFromBackupJson(
        sourceUri: Uri,
        restoreData: Boolean = true,
        restoreSettings: Boolean = true
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.bufferedReader().readText()
            } ?: throw IllegalStateException("Unable to read backup file.")

            restoreFromJsonString(
                raw = raw,
                restoreData = restoreData,
                restoreSettings = restoreSettings
            )
        }
    }
}
