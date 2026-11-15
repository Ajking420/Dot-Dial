package com.example.data.system

import android.content.Context
import android.net.Uri
import com.example.data.local.CallDirection
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RestoreSummary(
    val restoredContacts: Int,
    val restoredCallLogs: Int
)

class BackupExportManager(
    private val context: Context,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao
) {
    suspend fun exportBackupJson(targetUri: Uri): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val contacts = contactDao.getAllOnce()
            val logs = callLogDao.getAllOnce()

            val contactsArr = JSONArray()
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
                        put("vibrationPatternId", c.vibrationPatternId)
                        put("isFavorite", c.isFavorite)
                        put("isBlocked", c.isBlocked)
                        put("notes", c.notes)
                        put("accountName", c.accountName)
                    }
                )
            }

            val logsArr = JSONArray()
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

            val root = JSONObject().apply {
                put("version", 2)
                put("exportedAt", System.currentTimeMillis())
                put("contacts", contactsArr)
                put("callLogs", logsArr)
            }

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Unable to open backup destination.")

            RestoreSummary(contacts.size, logs.size)
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
                    appendLine("TEL;TYPE=CELL:${c.phoneNumber}")
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

    suspend fun restoreFromBackupJson(sourceUri: Uri): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.bufferedReader().readText()
            } ?: throw IllegalStateException("Unable to read backup file.")

            val root = JSONObject(raw)
            val contactsArr = root.optJSONArray("contacts") ?: JSONArray()
            val logsArr = root.optJSONArray("callLogs") ?: JSONArray()

            var contactsRestored = 0
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
                        vibrationPatternId = obj.optLong("vibrationPatternId", 1L),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isBlocked = obj.optBoolean("isBlocked", false),
                        notes = obj.optString("notes"),
                        accountName = obj.optString("accountName")
                    )
                )
                contactsRestored++
            }

            var logsRestored = 0
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

            RestoreSummary(contactsRestored, logsRestored)
        }
    }
}
