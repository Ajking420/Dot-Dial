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
import com.example.data.local.ContactNumbersCodec
import com.example.data.local.ContactPhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DeviceContactsSync"

data class SyncResult(
    val importedContacts: Int,
    val importedCallLogs: Int,
    val discoveredGoogleAccounts: List<String>
)

private data class RawPhoneCandidate(
    val rawNumber: String,
    val normalized: String,
    val label: String,
    val isPrimary: Boolean,
    val isMobile: Boolean
)

private class GroupedSystemContact(
    val contactId: Long,
    var name: String,
    var starred: Boolean,
    var photoUri: String?,
    var accountName: String,
    var accountType: String,
    val numbers: MutableList<RawPhoneCandidate> = mutableListOf()
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
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL,
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                )
                val groupedByPerson = linkedMapOf<Long, GroupedSystemContact>()

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
                    val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                    val priIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)
                    val supPriIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)
                    val starIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

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
                        val normalized = T9Utils.normalizePhone(rawNumber)
                        if (name.isEmpty() || normalized.isEmpty()) continue

                        val phoneType = if (typeIdx >= 0) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        val customLabel = if (labelIdx >= 0) cursor.getString(labelIdx) else null
                        val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                            context.resources,
                            phoneType,
                            customLabel
                        ).toString().ifBlank { "Mobile" }

                        val isPrimary = (priIdx >= 0 && cursor.getInt(priIdx) == 1) ||
                            (supPriIdx >= 0 && cursor.getInt(supPriIdx) == 1)
                        val starred = starIdx >= 0 && cursor.getInt(starIdx) == 1
                        val photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null

                        val group = groupedByPerson.getOrPut(cid) {
                            GroupedSystemContact(
                                contactId = cid,
                                name = name,
                                starred = starred,
                                photoUri = photoUri,
                                accountName = accName,
                                accountType = accType
                            )
                        }
                        if (starred) group.starred = true
                        if (group.photoUri.isNullOrBlank() && !photoUri.isNullOrBlank()) {
                            group.photoUri = photoUri
                        }
                        if (group.accountName.isBlank() && accName.isNotBlank()) {
                            group.accountName = accName
                            group.accountType = accType
                        }

                        if (group.numbers.none { it.normalized == normalized }) {
                            group.numbers.add(
                                RawPhoneCandidate(
                                    rawNumber = rawNumber,
                                    normalized = normalized,
                                    label = typeLabel,
                                    isPrimary = isPrimary,
                                    isMobile = phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                                )
                            )
                        }
                    }
                }

                // Persist 1 ContactEntity per person, collapsing any legacy split rows
                groupedByPerson.values.forEach { person ->
                    if (person.numbers.isEmpty()) return@forEach
                    val sysKey = "cid_${person.contactId}"
                    val primary = person.numbers.firstOrNull { it.isPrimary }
                        ?: person.numbers.firstOrNull { it.isMobile }
                        ?: person.numbers.first()

                    val extras = person.numbers
                        .filter { it.normalized != primary.normalized }
                        .map { ContactPhoneNumber(number = it.rawNumber, label = it.label) }
                    val extrasJson = ContactNumbersCodec.encode(extras)

                    val existingByKey = contactDao.findBySystemContactKey(sysKey)
                    val matchingExistingRows = person.numbers.mapNotNull { cand ->
                        contactDao.findByNormalizedNumber(cand.normalized)
                    }
                    val keeper = existingByKey ?: matchingExistingRows.firstOrNull()

                    if (keeper == null) {
                        contactDao.upsert(
                            ContactEntity(
                                name = person.name,
                                phoneNumber = primary.rawNumber,
                                normalizedNumber = primary.normalized,
                                t9Key = T9Utils.toT9Key(person.name),
                                email = emailsByContactId[person.contactId].orEmpty(),
                                photoUri = person.photoUri,
                                accountName = person.accountName,
                                accountType = person.accountType,
                                isFavorite = person.starred,
                                additionalNumbersJson = extrasJson,
                                systemContactKey = sysKey
                            )
                        )
                        contactsCount++
                    } else {
                        // Delete any leftover individual-number rows from earlier syncs
                        matchingExistingRows.forEach { dup ->
                            if (dup.id != keeper.id) {
                                contactDao.deleteById(dup.id)
                            }
                        }
                        val mergedPhoto = keeper.photoUri ?: person.photoUri
                        val mergedEmail = keeper.email.ifBlank { emailsByContactId[person.contactId].orEmpty() }
                        contactDao.update(
                            keeper.copy(
                                name = person.name,
                                phoneNumber = if (keeper.systemContactKey == sysKey) keeper.phoneNumber else primary.rawNumber,
                                normalizedNumber = if (keeper.systemContactKey == sysKey) keeper.normalizedNumber else primary.normalized,
                                t9Key = T9Utils.toT9Key(person.name),
                                email = mergedEmail,
                                photoUri = mergedPhoto,
                                accountName = keeper.accountName.ifBlank { person.accountName },
                                accountType = keeper.accountType.ifBlank { person.accountType },
                                isFavorite = keeper.isFavorite || person.starred,
                                additionalNumbersJson = extrasJson,
                                systemContactKey = sysKey
                            )
                        )
                    }
                }
            }

            if (hasCallLogPermission()) {
                val allContacts = contactDao.getAllOnce()
                val contactLookup = T9Utils.buildNumberLookup(allContacts)

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
                        val existingLog = callLogDao.findMatchingCallLog(
                            normalized = normalized,
                            minTs = date - 3000L,
                            maxTs = date + 3000L
                        )
                        if (existingLog == null) {
                            val contact = contactLookup[normalized]
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
                        }
                        limit++
                    }
                }
            }

            callLogDao.deduplicateLogs()

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
