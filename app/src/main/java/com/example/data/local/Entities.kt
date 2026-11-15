package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.system.T9Utils
import org.json.JSONArray
import org.json.JSONObject

data class ContactPhoneNumber(
    val number: String,
    val label: String = "Mobile",
    val isDefault: Boolean = false
)

object ContactNumbersCodec {
    fun decode(json: String): List<ContactPhoneNumber> {
        if (json.isBlank() || json == "[]") return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val num = obj.optString("number").trim()
                    if (num.isNotEmpty()) {
                        add(
                            ContactPhoneNumber(
                                number = num,
                                label = obj.optString("label", "Mobile").ifBlank { "Mobile" },
                                isDefault = false
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encode(entries: List<ContactPhoneNumber>): String {
        if (entries.isEmpty()) return "[]"
        val arr = JSONArray()
        entries.forEach { entry ->
            val clean = entry.number.trim()
            if (clean.isNotEmpty()) {
                arr.put(
                    JSONObject().apply {
                        put("number", clean)
                        put("label", entry.label.ifBlank { "Mobile" })
                    }
                )
            }
        }
        return arr.toString()
    }
}

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val t9Key: String,
    val company: String = "",
    val email: String = "",
    val photoUri: String? = null,
    val callingCardUri: String? = null,
    val accountName: String = "",
    val accountType: String = "",
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val spamScore: Int = 0,
    val videoBackdropUri: String? = null,
    val vibrationPatternId: Long = 1L,
    val notes: String = "",
    val additionalNumbersJson: String = "[]",
    val systemContactKey: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun allPhoneNumbers(): List<ContactPhoneNumber> {
        val primaryNorm = T9Utils.normalizePhone(phoneNumber)
        val seen = linkedSetOf(primaryNorm)
        val result = mutableListOf(
            ContactPhoneNumber(
                number = phoneNumber,
                label = "Default",
                isDefault = true
            )
        )
        ContactNumbersCodec.decode(additionalNumbersJson).forEach { extra ->
            val norm = T9Utils.normalizePhone(extra.number)
            if (norm.isNotEmpty() && seen.add(norm)) {
                result.add(extra)
            }
        }
        return result
    }

    fun allNormalizedNumbers(): List<String> =
        allPhoneNumbers().mapNotNull { entry ->
            T9Utils.normalizePhone(entry.number).takeIf { it.isNotEmpty() }
        }.distinct()
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED,
    BLOCKED_SPAM
}

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long? = null,
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val direction: CallDirection,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val isSpam: Boolean = false,
    val spamReason: String = "",
    val recordingId: Long? = null
)

@Entity(tableName = "recordings")
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callLogId: Long? = null,
    val contactName: String,
    val phoneNumber: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val transcriptText: String = "",
    val aiHeadline: String = "",
    val aiSummaryBulletsJson: String = "[]",
    val aiActionItemsJson: String = "[]",
    val aiSentiment: String = "NEUTRAL",
    val isLocked: Boolean = true
)

@Entity(tableName = "vibration_patterns")
data class VibrationPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val timingsCsv: String,
    val amplitudesCsv: String,
    val isBuiltIn: Boolean = false
)

@Entity(tableName = "spam_rules")
data class SpamRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val label: String,
    val riskLevel: Int = 85,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
