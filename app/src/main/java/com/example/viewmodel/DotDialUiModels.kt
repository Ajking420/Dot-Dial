package com.example.viewmodel

import android.app.PendingIntent
import com.example.data.ai.SpamRiskAssessment
import com.example.data.local.CallDirection
import com.example.data.local.CallLogEntity
import com.example.data.local.CallRecordingEntity
import com.example.data.local.ContactEntity
import com.example.data.local.SpamRuleEntity
import com.example.data.local.VibrationPatternEntity
import com.example.data.preferences.DialerSettings
import com.example.data.system.DriveBackupFile
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable data object Dialer : AppRoute
    @Serializable data object History : AppRoute
    @Serializable data object Contacts : AppRoute
    @Serializable data object Vault : AppRoute
    @Serializable data object Analytics : AppRoute
    @Serializable data object Settings : AppRoute
}

enum class HistoryFilter(val label: String) {
    ALL("ALL"),
    INCOMING("INCOMING"),
    OUTGOING("OUTGOING"),
    MISSED("MISSED"),
    SPAM("SPAM")
}

data class PersonCallGroup(
    val groupKey: String,
    val displayName: String,
    val phoneNumber: String,
    val matchedContact: ContactEntity?,
    val latestCall: CallLogEntity,
    val calls: List<CallLogEntity>,
    val callCount: Int,
    val hasSpam: Boolean
)

enum class AnalyticsPeriod(val label: String, val days: Int?) {
    SEVEN_DAYS("7D", 7),
    THIRTY_DAYS("30D", 30),
    ALL_TIME("ALL", null)
}

sealed interface AiControlUiState {
    data object Idle : AiControlUiState
    data class Processing(val prompt: String) : AiControlUiState
    data class Success(val message: String, val executedAction: String) : AiControlUiState
    data class Error(val message: String) : AiControlUiState
}

data class DailyCallMetric(
    val dayLabel: String,
    val totalMinutes: Float,
    val callCount: Int
)

data class ContactDurationMetric(
    val name: String,
    val phoneNumber: String,
    val totalSeconds: Int,
    val callCount: Int
)

data class CallAnalyticsSummary(
    val totalCalls: Int = 0,
    val totalDurationSeconds: Int = 0,
    val averageDurationSeconds: Int = 0,
    val incomingCount: Int = 0,
    val outgoingCount: Int = 0,
    val missedCount: Int = 0,
    val spamBlockedCount: Int = 0,
    val dailySeries: List<DailyCallMetric> = emptyList(),
    val topContacts: List<ContactDurationMetric> = emptyList()
)

sealed interface RecordingSummaryUiState {
    data object Idle : RecordingSummaryUiState
    data class Loading(val recordingId: Long) : RecordingSummaryUiState
    data class Error(val recordingId: Long, val message: String) : RecordingSummaryUiState
}

sealed interface SpamCheckUiState {
    data object Idle : SpamCheckUiState
    data object Checking : SpamCheckUiState
    data class Result(val phoneNumber: String, val assessment: SpamRiskAssessment) : SpamCheckUiState
    data class Error(val message: String) : SpamCheckUiState
}

data class NavigationRequest(
    val route: AppRoute,
    val nonce: Long = System.currentTimeMillis()
)

data class GoogleDriveUiState(
    val isAuthorizing: Boolean = false,
    val isBusy: Boolean = false,
    val connectedEmail: String = "",
    val displayName: String = "",
    val pendingConsentIntent: PendingIntent? = null,
    val availableBackups: List<DriveBackupFile> = emptyList(),
    val statusDetail: String = "",
    val errorMessage: String? = null
)

data class GoogleAuthUiState(
    val isSigningIn: Boolean = false,
    val errorMessage: String? = null
)

data class DotDialSnapshot(
    val settings: DialerSettings = DialerSettings(),
    val contacts: List<ContactEntity> = emptyList(),
    val callLogs: List<CallLogEntity> = emptyList(),
    val recordings: List<CallRecordingEntity> = emptyList(),
    val vibrationPatterns: List<VibrationPatternEntity> = emptyList(),
    val spamRules: List<SpamRuleEntity> = emptyList(),
    val dialerQuery: String = "",
    val contactSearchQuery: String = "",
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.SEVEN_DAYS
) {
    val totalDirections: Map<CallDirection, Int>
        get() = callLogs.groupingBy { it.direction }.eachCount()
}
