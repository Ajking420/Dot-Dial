package com.example.data.system

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.CallDirection
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DeviceContactsSync"

data class SyncResult(
    val importedContacts: Int,
    val importedCallLogs: Int,
    val discoveredGoogleAccounts: List<String>
)

class DeviceContactsSync(
    private val context: Context,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao
) {
    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun hasCallLogPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun discoverGoogleAccounts(): List<String> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()
        val accounts = linkedSetOf<String>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts.ACCOUNT_NAME,
                    ContactsContract.RawContacts.ACCOUNT_TYPE
                ),
                "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
                arrayOf("com.google"),
                null
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                while (cursor.moveToNext()) {
                    val acc = cursor.getString(nameIdx)?.trim().orEmpty()
                    if (acc.isNotEmpty()) accounts.add(acc)
                }
            }
        }
        accounts.toList()
    }

    suspend fun syncFromDevice(
        allowedGoogleAccounts: Set<String> = emptySet(),
        googleSyncEnabled: Boolean = true
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            var contactsCount = 0
            var logsCount = 0
            val discoveredAccounts = linkedSetOf<String>()

            if (hasContactsPermission()) {
                val emailsByContactId = mutableMapOf<Long, String>()
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Email.ADDRESS
                    ),
                    null,
                    null,
                    null
                )?.use { emailCursor ->
                    val cidIdx = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                    val addrIdx = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    while (emailCursor.moveToNext()) {
                        val cid = emailCursor.getLong(cidIdx)
                        val addr = emailCursor.getString(addrIdx)?.trim().orEmpty()
                        if (addr.isNotEmpty() && !emailsByContactId.containsKey(cid)) {
                            emailsByContactId[cid] = addr
                        }
                    }
                }

                val accountByRawContact = mutableMapOf<Long, Pair<String, String>>()
                context.contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(
                        ContactsContract.RawContacts._ID,
                        ContactsContract.RawContacts.ACCOUNT_NAME,
                        ContactsContract.RawContacts.ACCOUNT_TYPE
                    ),
                    null,
                    null,
                    null
                )?.use { rawCursor ->
                    val idIdx = rawCursor.getColumnIndex(ContactsContract.RawContacts._ID)
                    val accNameIdx = rawCursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                    val accTypeIdx = rawCursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                    while (rawCursor.moveToNext()) {
                        val rawId = rawCursor.getLong(idIdx)
                        val accName = rawCursor.getString(accNameIdx).orEmpty()
                        val accType = rawCursor.getString(accTypeIdx).orEmpty()
                        if (accType == "com.google" && accName.isNotBlank()) {
                            discoveredAccounts.add(accName)
                        }
                        accountByRawContact[rawId] = accName to accType
                    }
                }

                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                )
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )?.use { cursor ->
                    val cidIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val rawIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val starIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    val seenNumbers = mutableSetOf<String>()

                    while (cursor.moveToNext()) {
                        val cid = cursor.getLong(cidIdx)
                        val rawId = cursor.getLong(rawIdx)
                        val (accName, accType) = accountByRawContact[rawId] ?: ("" to "")

                        if (accType == "com.google") {
                            if (!googleSyncEnabled) continue
                            if (allowedGoogleAccounts.isNotEmpty() && !allowedGoogleAccounts.contains(accName)) {
                                continue
                            }
                        }

                        val name = cursor.getString(nameIdx)?.trim().orEmpty()
                        val rawNumber = cursor.getString(numIdx)?.trim().orEmpty()
                        val starred = starIdx >= 0 && cursor.getInt(starIdx) == 1
                        val photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                        val normalized = T9Utils.normalizePhone(rawNumber)

                        if (name.isNotEmpty() && normalized.isNotEmpty() && seenNumbers.add(normalized)) {
                            val existing = contactDao.findByNormalizedNumber(normalized)
                            if (existing == null) {
                                contactDao.upsert(
                                    ContactEntity(
                                        name = name,
                                        phoneNumber = rawNumber,
                                        normalizedNumber = normalized,
                                        t9Key = T9Utils.toT9Key(name),
                                        email = emailsByContactId[cid].orEmpty(),
                                        photoUri = photoUri,
                                        accountName = accName,
                                        accountType = accType,
                                        isFavorite = starred
                                    )
                                )
                                contactsCount++
                            } else if (existing.photoUri == null && photoUri != null) {
                                contactDao.update(existing.copy(photoUri = photoUri))
                            }
                        }
                    }
                }
            }

            if (hasCallLogPermission()) {
                val projection = arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                )
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                    val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                    val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                    var limit = 0
                    while (cursor.moveToNext() && limit < 60) {
                        val rawNumber = cursor.getString(numIdx).orEmpty()
                        if (rawNumber.isBlank()) continue
                        val cachedName = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: rawNumber
                        val type = cursor.getInt(typeIdx)
                        val date = cursor.getLong(dateIdx)
                        val duration = cursor.getInt(durIdx)
                        val direction = when (type) {
                            CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
                            CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                            CallLog.Calls.BLOCKED_TYPE, CallLog.Calls.REJECTED_TYPE -> CallDirection.BLOCKED_SPAM
                            else -> CallDirection.OUTGOING
                        }
                        val normalized = T9Utils.normalizePhone(rawNumber)
                        val contact = contactDao.findByNormalizedNumber(normalized)
                        callLogDao.insert(
                            CallLogEntity(
                                contactId = contact?.id,
                                displayName = contact?.name ?: cachedName,
                                phoneNumber = rawNumber,
                                normalizedNumber = normalized,
                                direction = direction,
                                timestamp = date,
                                durationSeconds = duration,
                                isSpam = direction == CallDirection.BLOCKED_SPAM
                            )
                        )
                        logsCount++
                        limit++
                    }
                }
            }

            Result.success(SyncResult(contactsCount, logsCount, discoveredAccounts.toList()))
        } catch (e: Exception) {
            Log.e(TAG, "Device contacts/call log sync failed", e)
            Result.failure(e)
        }
    }

    suspend fun exportContactToSystemIfPermitted(
        name: String,
        phoneNumber: String,
        email: String = "",
        googleAccount: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext false
        }
        return@withContext runCatching {
            val ops = arrayListOf(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(
                        ContactsContract.RawContacts.ACCOUNT_TYPE,
                        if (!googleAccount.isNullOrBlank()) "com.google" else null
                    )
                    .withValue(
                        ContactsContract.RawContacts.ACCOUNT_NAME,
                        googleAccount?.takeIf { it.isNotBlank() }
                    )
                    .build(),
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        name
                    )
                    .build(),
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )
            if (email.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .build()
                )
            }
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        }.getOrDefault(false)
    }
}
