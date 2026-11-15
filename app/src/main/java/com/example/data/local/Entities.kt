package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val updatedAt: Long = System.currentTimeMillis()
)

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
