package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DotDialContainer
import com.example.auth.GoogleSignInOutcome
import com.example.auth.GoogleSignedInProfile
import com.example.data.ai.AiCommandIntent
import com.example.data.ai.AssistantReplyLanguage
import com.example.data.local.CallDirection
import com.example.data.local.CallLogEntity
import com.example.data.local.CallRecordingEntity
import com.example.data.local.ContactEntity
import com.example.data.local.ContactNumbersCodec
import com.example.data.local.ContactPhoneNumber
import com.example.data.local.SpamRuleEntity
import com.example.data.local.VibrationPatternEntity
import com.example.data.preferences.AiAssistantLanguageMode
import com.example.data.preferences.AiAssistantTriggerMode
import com.example.data.preferences.AudioOutputMode
import com.example.data.preferences.AutoRecordMode
import com.example.data.preferences.CallerBackdropMode
import com.example.data.preferences.ColorPlateStyle
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.DialpadSoundStyle
import com.example.data.preferences.NewContactStorageMode
import com.example.data.preferences.UiBackgroundMode
import com.example.data.preferences.VoiceChangerPreset
import com.example.data.preferences.VoiceChangerSettingsMode
import com.example.data.system.ActiveAudioRoute
import com.example.data.system.DeviceNameHelper
import com.example.data.system.DriveAuthorizationOutcome
import com.example.data.system.GoogleDriveBackupManager
import com.example.data.system.T9Utils
import com.example.telecom.AiCallPhase
import com.example.telecom.CallDisplayMode
import com.example.telecom.CallLifecycleState
import com.example.telecom.CallNotificationManager
import com.example.telecom.CallSessionBridge
import com.example.telecom.CallSessionState
import com.example.ui.theme.DialerFontPreset
import com.example.ui.theme.DialerThemePreset
import com.example.ui.theme.FontStyleMode
import com.example.voice.FastCallVoiceAction
import com.example.voice.VoiceCommandController
import com.example.voice.VoiceUiState
import com.example.widget.DotDialContactWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "DotDialViewModel"

class DotDialViewModel(application: Application) : AndroidViewModel(application) {

    private val container = DotDialContainer(application)
    private val db = container.database
    private val prefs = container.preferences
    private val vibEngine = container.vibrationEngine
    private val recorder = container.audioRecorder
    private val gemini = container.geminiAi
    private val voice = container.voiceController
    private val aiAssistantEngine = container.aiAssistantEngine
    private val contactsSync = container.contactsSync
    private val spamEngine = container.spamEngine
    private val uiSounds = container.uiSounds
    private val audioRoutes = container.audioRouteManager
    private val ringtonePlayer = container.ringtonePlayer
    private val backupManager = container.backupManager
    private val driveBackup = container.driveBackupManager
    private val signInManager = container.signInManager
    private val voiceChangerEngine = container.voiceChangerEngine
    private var dtmfStopJob: Job? = null
    private var aiAutoAnswerJob: Job? = null
    private var aiConversationJob: Job? = null
    private var cachedDriveToken: String? = null
    private var pendingDriveAction: (() -> Unit)? = null

    private val _dialerQuery = MutableStateFlow("")
    val dialerQuery: StateFlow<String> = _dialerQuery.asStateFlow()

    private val _isDialpadVisible = MutableStateFlow(true)
    val isDialpadVisible: StateFlow<Boolean> = _isDialpadVisible.asStateFlow()

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

    private val _analyticsPeriod = MutableStateFlow(AnalyticsPeriod.SEVEN_DAYS)
    val analyticsPeriod: StateFlow<AnalyticsPeriod> = _analyticsPeriod.asStateFlow()

    private val _aiCommandState = MutableStateFlow<AiControlUiState>(AiControlUiState.Idle)
    val aiCommandState: StateFlow<AiControlUiState> = _aiCommandState.asStateFlow()

    private val _summaryState = MutableStateFlow<RecordingSummaryUiState>(RecordingSummaryUiState.Idle)
    val summaryState: StateFlow<RecordingSummaryUiState> = _summaryState.asStateFlow()

    private val _spamCheckState = MutableStateFlow<SpamCheckUiState>(SpamCheckUiState.Idle)
    val spamCheckState: StateFlow<SpamCheckUiState> = _spamCheckState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _navigationRequest = MutableStateFlow<NavigationRequest?>(null)
    val navigationRequest: StateFlow<NavigationRequest?> = _navigationRequest.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _discoveredGoogleAccounts = MutableStateFlow<List<String>>(emptyList())
    val discoveredGoogleAccounts: StateFlow<List<String>> = _discoveredGoogleAccounts.asStateFlow()

    private val _driveUiState = MutableStateFlow(GoogleDriveUiState())
    val driveUiState: StateFlow<GoogleDriveUiState> = _driveUiState.asStateFlow()

    private val _googleAuthState = MutableStateFlow(GoogleAuthUiState())
    val googleAuthState: StateFlow<GoogleAuthUiState> = _googleAuthState.asStateFlow()

    val settings: StateFlow<DialerSettings> = prefs.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DialerSettings()
    )

    val firstLaunchSignInReady: StateFlow<Boolean> = prefs.settingsFlow
        .map { !it.hasPromptedGoogleSignIn && it.signedInGoogleEmail.isBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val contacts: StateFlow<List<ContactEntity>> = db.contactDao().observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val callLogs: StateFlow<List<CallLogEntity>> = db.callLogDao().observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val filteredHistory: StateFlow<List<CallLogEntity>> = combine(
        callLogs,
        _historyFilter
    ) { logs, filter ->
        when (filter) {
            HistoryFilter.ALL -> logs
            HistoryFilter.INCOMING -> logs.filter { it.direction == CallDirection.INCOMING }
            HistoryFilter.OUTGOING -> logs.filter { it.direction == CallDirection.OUTGOING }
            HistoryFilter.MISSED -> logs.filter { it.direction == CallDirection.MISSED }
            HistoryFilter.SPAM -> logs.filter { it.isSpam || it.direction == CallDirection.BLOCKED_SPAM }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedHistory: StateFlow<List<PersonCallGroup>> = combine(
        filteredHistory,
        contacts
    ) { logs, contactList ->
        val lookup = T9Utils.buildNumberLookup(contactList)
        val buckets = linkedMapOf<String, MutableList<CallLogEntity>>()
        logs.forEach { log ->
            val matched = lookup[log.normalizedNumber]
            val key = matched?.id?.let { "contact_$it" }
                ?: log.normalizedNumber.ifBlank { log.phoneNumber.trim().ifBlank { "log_${log.id}" } }
            buckets.getOrPut(key) { mutableListOf() }.add(log)
        }
        buckets.map { (key, entries) ->
            val sorted = entries.sortedByDescending { it.timestamp }
            val latest = sorted.first()
            val matched = lookup[latest.normalizedNumber]
            PersonCallGroup(
                groupKey = key,
                displayName = matched?.name ?: latest.displayName.ifBlank { latest.phoneNumber },
                phoneNumber = matched?.phoneNumber ?: latest.phoneNumber,
                matchedContact = matched,
                latestCall = latest,
                calls = sorted,
                callCount = sorted.size,
                hasSpam = sorted.any { it.isSpam || it.direction == CallDirection.BLOCKED_SPAM }
            )
        }.sortedByDescending { it.latestCall.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordings: StateFlow<List<CallRecordingEntity>> = db.recordingDao().observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val vibrationPatterns: StateFlow<List<VibrationPatternEntity>> =
        db.vibrationPatternDao().observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val spamRules: StateFlow<List<SpamRuleEntity>> = db.spamRuleDao().observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val callSession: StateFlow<CallSessionState> = CallSessionBridge.session
    val voiceState: StateFlow<VoiceUiState> = voice.state
    val playingRecordingId: StateFlow<Long?> = recorder.playingRecordingId
    val bluetoothConnected: StateFlow<Boolean> = audioRoutes.bluetoothConnected

    val predictiveMatches: StateFlow<List<ContactEntity>> = combine(
        contacts,
        _dialerQuery
    ) { list, query ->
        if (query.isBlank()) emptyList()
        else list.filter { !it.isBlocked && T9Utils.matchesQuery(it, query) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredContacts: StateFlow<List<ContactEntity>> = combine(
        contacts,
        _contactSearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { T9Utils.matchesQuery(it, query) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analyticsSummary: StateFlow<CallAnalyticsSummary> = combine(
        callLogs,
        _analyticsPeriod
    ) { logs, period ->
        computeAnalytics(logs, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallAnalyticsSummary())

    val isDefaultDialer: StateFlow<Boolean> = callSession.map {
        CallSessionBridge.isDefaultDialer(getApplication())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var callTimerJob: Job? = null
    private var ringTimeoutJob: Job? = null
    private var currentCallLogId: Long? = null

    init {
        viewModelScope.launch {
            signInManager.attemptSilentSignIn()?.let { profile ->
                applySignedInProfile(profile, silent = true)
            }
        }

        viewModelScope.launch {
            _discoveredGoogleAccounts.value = contactsSync.discoverGoogleAccounts()
            if (contactsSync.hasContactsPermission() || contactsSync.hasCallLogPermission()) {
                syncDeviceContactsAndLogs(silent = true)
            }
        }

        viewModelScope.launch {
            audioRoutes.bluetoothConnected.collect { bt ->
                CallSessionBridge.updateBluetoothConnected(bt)
                val s = callSession.value
                if (s.lifecycle == CallLifecycleState.ACTIVE &&
                    settings.value.audioOutputMode == AudioOutputMode.AUTO_BT_EAR
                ) {
                    val targetRoute = if (bt) ActiveAudioRoute.BLUETOOTH else ActiveAudioRoute.EAR_RECEIVER
                    applyCallAudioRoute(targetRoute)
                }
            }
        }

        // Observe system/in-app call lifecycle transitions
        viewModelScope.launch {
            callSession.collect { session ->
                when (session.lifecycle) {
                    CallLifecycleState.INCOMING_RINGING -> {
                        if (session.isSystemTelecomCall && session.phoneNumber.isNotBlank()) {
                            val contact = findMatchingContact(session.phoneNumber)
                            val verdict = spamEngine.evaluateNumber(session.phoneNumber, session.displayName)
                            val curSettings = settings.value
                            val backdrop = CallSessionBridge.resolveBackdropSpec(contact, curSettings)
                            CallSessionBridge.enrichWithContact(
                                contact = contact,
                                isSpam = verdict.isSpam,
                                spamScore = verdict.riskScore,
                                spamReason = verdict.reason,
                                backdrop = backdrop,
                                defaultVibrationId = curSettings.activeVibrationPatternId
                            )
                            if (!session.isRingMuted) {
                                ringtonePlayer.startRinging(curSettings.customRingtoneUri)
                                playVibrationById(
                                    contact?.vibrationPatternId ?: curSettings.activeVibrationPatternId,
                                    repeat = true
                                )
                            }
                            scheduleAiAutoAnswerIfNeeded(isUnknownCaller = contact == null)
                        }
                        if (settings.value.voiceCommandControlEnabled &&
                            !settings.value.aiAssistantEnabled &&
                            !voice.state.value.isContinuousCallMode
                        ) {
                            voice.startHandsFreeCallLoop { spoken ->
                                executeAiOrVoiceCommand(spoken)
                            }
                        }
                    }

                    CallLifecycleState.DIALING -> {
                        // Never cycle mic SpeechRecognizer during outgoing dialing
                        voice.stopListening()
                        if (session.phoneNumber.isNotBlank() &&
                            (session.contactId == null || session.displayName.isBlank() || session.displayName == session.phoneNumber)
                        ) {
                            val contact = findMatchingContact(session.phoneNumber)
                            val verdict = spamEngine.evaluateNumber(session.phoneNumber, contact?.name.orEmpty())
                            val curSettings = settings.value
                            val backdrop = CallSessionBridge.resolveBackdropSpec(contact, curSettings)
                            CallSessionBridge.enrichWithContact(
                                contact = contact,
                                isSpam = verdict.isSpam,
                                spamScore = verdict.riskScore,
                                spamReason = verdict.reason,
                                backdrop = backdrop,
                                defaultVibrationId = curSettings.activeVibrationPatternId
                            )
                        }
                    }

                    CallLifecycleState.ACTIVE -> {
                        // Stop SpeechRecognizer immediately when call is received/connected to prevent mic cycling & call audio issues
                        voice.stopListening()
                        ringtonePlayer.stop()
                        vibEngine.stop()
                        ringTimeoutJob?.cancel()
                        ringTimeoutJob = null
                        if (callTimerJob == null) startElapsedTimer()
                        if (session.phoneNumber.isNotBlank() &&
                            (session.contactId == null || session.displayName.isBlank() || session.displayName == session.phoneNumber)
                        ) {
                            val contact = findMatchingContact(session.phoneNumber)
                            val verdict = spamEngine.evaluateNumber(session.phoneNumber, contact?.name.orEmpty())
                            val curSettings = settings.value
                            val backdrop = CallSessionBridge.resolveBackdropSpec(contact, curSettings)
                            CallSessionBridge.enrichWithContact(
                                contact = contact,
                                isSpam = verdict.isSpam,
                                spamScore = verdict.riskScore,
                                spamReason = verdict.reason,
                                backdrop = backdrop,
                                defaultVibrationId = curSettings.activeVibrationPatternId
                            )
                        }
                        CallNotificationManager.showOrUpdateCallNotification(
                            context = getApplication(),
                            callerName = session.displayName,
                            phoneNumber = session.phoneNumber,
                            isRinging = false,
                            isSpeakerOn = session.isSpeakerOn,
                            callStartMs = session.callStartedAtMs
                        )
                        syncActiveVoiceChangerState(session, settings.value)
                    }

                    CallLifecycleState.IDLE -> {
                        ringtonePlayer.stop()
                        vibEngine.stop()
                        ringTimeoutJob?.cancel()
                        ringTimeoutJob = null
                        aiAutoAnswerJob?.cancel()
                        aiAutoAnswerJob = null
                        aiConversationJob?.cancel()
                        aiConversationJob = null
                        aiAssistantEngine.stopAll()
                        voiceChangerEngine.stopLiveDsp()
                        stopElapsedTimer()
                        voice.stopListening()
                        CallNotificationManager.cancel(getApplication())
                    }
                }
            }
        }
    }

    fun playUiClick() {
        uiSounds.playUiClick(settings.value.uiSoundsEnabled)
    }

    fun pulseUiHaptic() {
        if (settings.value.uiVibrationEnabled) {
            vibEngine.pulseKeypadTick()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showMessage(message: String) {
        _statusMessage.value = message
    }

    fun clearNavigationRequest() {
        _navigationRequest.value = null
    }

    fun appendDialDigit(digit: String) {
        pulseUiHaptic()
        uiSounds.playDialTone(digit, settings.value.dialpadSoundStyle)
        _dialerQuery.value += digit
    }

    fun backspaceDialDigit() {
        pulseUiHaptic()
        playUiClick()
        val current = _dialerQuery.value
        if (current.isNotEmpty()) {
            _dialerQuery.value = current.dropLast(1)
        }
    }

    fun clearDialQuery() {
        playUiClick()
        _dialerQuery.value = ""
    }

    fun toggleDialpadVisible() {
        playUiClick()
        _isDialpadVisible.value = !_isDialpadVisible.value
    }

    fun setDialpadVisible(visible: Boolean) {
        _isDialpadVisible.value = visible
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        playUiClick()
        _historyFilter.value = filter
    }

    fun setContactSearchQuery(query: String) {
        _contactSearchQuery.value = query
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        playUiClick()
        _analyticsPeriod.value = period
    }

    fun syncDeviceContactsAndLogs(silent: Boolean = false) {
        viewModelScope.launch {
            val cur = settings.value
            val result = contactsSync.syncFromDevice(
                allowedGoogleAccounts = cur.syncedGoogleAccounts,
                googleSyncEnabled = cur.googleSyncEnabled
            )
            result.onSuccess { sync ->
                _discoveredGoogleAccounts.value = sync.discoveredGoogleAccounts
                DotDialContactWidgetProvider.refreshAll(getApplication())
                if (!silent) {
                    _statusMessage.value =
                        "Synced ${sync.importedContacts} contacts & ${sync.importedCallLogs} call logs."
                }
            }.onFailure { err ->
                if (!silent) {
                    _statusMessage.value = err.localizedMessage ?: "Unable to sync device contacts."
                }
            }
        }
    }

    fun addGoogleSyncAccount(email: String) {
        val clean = email.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            prefs.addGoogleSyncAccount(clean)
            syncDeviceContactsAndLogs(silent = false)
        }
    }

    fun removeGoogleSyncAccount(email: String) {
        viewModelScope.launch {
            prefs.removeGoogleSyncAccount(email)
            db.contactDao().deleteByAccountName(email)
            DotDialContactWidgetProvider.refreshAll(getApplication())
            _statusMessage.value = "Removed Google account sync for $email."
        }
    }

    fun setGoogleSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setGoogleSyncEnabled(enabled)
        }
    }

    // Multi-user Google Account Sign-In / Switcher (Credential Manager + Firebase Auth)
    fun signInWithGoogle(activityContext: Context) {
        if (_googleAuthState.value.isSigningIn) return
        viewModelScope.launch {
            prefs.setHasPromptedGoogleSignIn(true)
            _googleAuthState.value = GoogleAuthUiState(isSigningIn = true, errorMessage = null)
            when (val outcome = signInManager.signInWithGoogle(activityContext)) {
                is GoogleSignInOutcome.Success -> {
                    applySignedInProfile(outcome.profile, silent = false)
                    _googleAuthState.value = GoogleAuthUiState(isSigningIn = false, errorMessage = null)
                }
                is GoogleSignInOutcome.Cancelled -> {
                    _googleAuthState.value = GoogleAuthUiState(isSigningIn = false, errorMessage = null)
                }
                is GoogleSignInOutcome.Failed -> {
                    _googleAuthState.value = GoogleAuthUiState(
                        isSigningIn = false,
                        errorMessage = outcome.message
                    )
                    _statusMessage.value = outcome.message
                }
            }
        }
    }

    private suspend fun applySignedInProfile(
        profile: GoogleSignedInProfile,
        silent: Boolean
    ) {
        val prev = settings.value
        val emailChanged = prev.signedInGoogleEmail.isNotBlank() &&
            !prev.signedInGoogleEmail.equals(profile.email, ignoreCase = true)

        if (emailChanged) {
            cachedDriveToken = null
            prefs.setDriveConnectionInfo("")
            _driveUiState.value = GoogleDriveUiState()
        }

        prefs.setSignedInGoogleProfile(
            email = profile.email,
            displayName = profile.displayName,
            photoUri = profile.photoUri
        )

        val ownerShouldAutoUpdate = prev.aiAssistantOwnerName.isBlank() ||
            prev.aiAssistantOwnerName == prev.signedInGoogleName
        if (ownerShouldAutoUpdate && profile.displayName.isNotBlank()) {
            prefs.setAiAssistantOwnerName(profile.displayName)
        }

        if (profile.email.isNotBlank() && prev.googleSyncEnabled) {
            prefs.addGoogleSyncAccount(profile.email)
            syncDeviceContactsAndLogs(silent = true)
        }

        if (!silent) {
            val label = profile.displayName.ifBlank { profile.email }
            _statusMessage.value = "Signed in as $label. Drive backup & AI profile updated."
        }
    }

    fun signOutGoogleAccount() {
        viewModelScope.launch {
            val prevName = settings.value.signedInGoogleName
            signInManager.signOut()
            cachedDriveToken = null
            prefs.setSignedInGoogleProfile("", "", "")
            prefs.setDriveConnectionInfo("")
            if (prevName.isNotBlank() && settings.value.aiAssistantOwnerName == prevName) {
                prefs.setAiAssistantOwnerName("")
            }
            _driveUiState.value = GoogleDriveUiState()
            _googleAuthState.value = GoogleAuthUiState()
            _statusMessage.value = "Signed out of Google account."
        }
    }

    fun dismissGoogleSignInPrompt() {
        viewModelScope.launch {
            prefs.setHasPromptedGoogleSignIn(true)
        }
    }

    fun exportBackupJson(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportBackupJson(uri, includeData = true, includeSettings = true).onSuccess { res ->
                _statusMessage.value =
                    "Exported ${res.restoredContacts} contacts, ${res.restoredCallLogs} call logs & settings."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Backup export failed."
            }
        }
    }

    fun exportSettingsJson(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportSettingsJson(uri).onSuccess { count ->
                _statusMessage.value = "Exported $count app settings preferences (.json)."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Settings export failed."
            }
        }
    }

    fun exportContactsVcf(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportContactsVcf(uri).onSuccess { count ->
                _statusMessage.value = "Exported $count contacts as vCard (.vcf)."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "vCard export failed."
            }
        }
    }

    fun restoreBackupJson(uri: Uri) {
        viewModelScope.launch {
            backupManager.restoreFromBackupJson(uri, restoreData = true, restoreSettings = true).onSuccess { res ->
                DotDialContactWidgetProvider.refreshAll(getApplication())
                val settingsPart = if (res.restoredSettingsCount > 0) " & ${res.restoredSettingsCount} settings" else ""
                _statusMessage.value =
                    "Restored ${res.restoredContacts} contacts, ${res.restoredCallLogs} logs$settingsPart."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Backup restore failed."
            }
        }
    }

    fun restoreSettingsJson(uri: Uri) {
        viewModelScope.launch {
            backupManager.restoreFromBackupJson(uri, restoreData = false, restoreSettings = true).onSuccess { res ->
                _statusMessage.value = if (res.restoredSettingsCount > 0) {
                    "Restored ${res.restoredSettingsCount} app settings preferences."
                } else {
                    "No valid app settings found in selected file."
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Settings restore failed."
            }
        }
    }

    // Google Drive Direct Backup & Restore
    fun consumeDrivePendingConsent() {
        _driveUiState.value = _driveUiState.value.copy(pendingConsentIntent = null)
    }

    fun connectGoogleDrive(afterAuth: (() -> Unit)? = null) {
        pendingDriveAction = afterAuth
        viewModelScope.launch {
            _driveUiState.value = _driveUiState.value.copy(
                isAuthorizing = true,
                errorMessage = null
            )
            when (val outcome = driveBackup.authorizeDriveAccess()) {
                is DriveAuthorizationOutcome.Authorized -> {
                    onDriveTokenAcquired(outcome.accessToken)
                }
                is DriveAuthorizationOutcome.NeedsConsent -> {
                    _driveUiState.value = _driveUiState.value.copy(
                        isAuthorizing = false,
                        pendingConsentIntent = outcome.pendingIntent
                    )
                }
                is DriveAuthorizationOutcome.Failed -> {
                    pendingDriveAction = null
                    _driveUiState.value = _driveUiState.value.copy(
                        isAuthorizing = false,
                        errorMessage = outcome.message
                    )
                    _statusMessage.value = outcome.message
                }
            }
        }
    }

    fun onDriveConsentActivityResult(data: Intent?) {
        driveBackup.extractTokenFromConsentResult(data).onSuccess { token ->
            viewModelScope.launch {
                onDriveTokenAcquired(token)
            }
        }.onFailure { err ->
            pendingDriveAction = null
            val msg = err.localizedMessage ?: "Google Drive consent failed."
            _driveUiState.value = _driveUiState.value.copy(
                isAuthorizing = false,
                errorMessage = msg
            )
            _statusMessage.value = msg
        }
    }

    private suspend fun onDriveTokenAcquired(token: String) {
        cachedDriveToken = token
        val userInfo = driveBackup.fetchUserInfo(token).getOrNull()
        val email = userInfo?.email?.ifBlank { settings.value.driveConnectedEmail }
            ?: settings.value.driveConnectedEmail
        val display = userInfo?.displayName.orEmpty()
        if (email.isNotBlank()) {
            prefs.setDriveConnectionInfo(email)
        }
        val files = driveBackup.listBackups(token).getOrDefault(emptyList())
        _driveUiState.value = _driveUiState.value.copy(
            isAuthorizing = false,
            connectedEmail = email.ifBlank { "Google Drive Connected" },
            displayName = display,
            availableBackups = files,
            errorMessage = null,
            statusDetail = "Connected to '${GoogleDriveBackupManager.DRIVE_FOLDER_DISPLAY_NAME}'"
        )
        val pending = pendingDriveAction
        pendingDriveAction = null
        if (pending != null) {
            pending.invoke()
        } else {
            _statusMessage.value = "Google Drive connected (${email.ifBlank { "Ready" }})."
        }
    }

    fun refreshDriveBackups() {
        val token = cachedDriveToken
        if (token.isNullOrBlank()) {
            connectGoogleDrive { refreshDriveBackups() }
            return
        }
        viewModelScope.launch {
            _driveUiState.value = _driveUiState.value.copy(isBusy = true, errorMessage = null)
            driveBackup.listBackups(token).onSuccess { list ->
                _driveUiState.value = _driveUiState.value.copy(
                    isBusy = false,
                    availableBackups = list,
                    statusDetail = "Found ${list.size} backup file(s) in '${GoogleDriveBackupManager.DRIVE_FOLDER_DISPLAY_NAME}'"
                )
            }.onFailure { err ->
                cachedDriveToken = null
                _driveUiState.value = _driveUiState.value.copy(
                    isBusy = false,
                    errorMessage = err.localizedMessage ?: "Unable to refresh Google Drive backups."
                )
            }
        }
    }

    fun backupToGoogleDrive(includeData: Boolean, includeSettings: Boolean) {
        val token = cachedDriveToken
        if (token.isNullOrBlank()) {
            connectGoogleDrive { backupToGoogleDrive(includeData, includeSettings) }
            return
        }
        viewModelScope.launch {
            _driveUiState.value = _driveUiState.value.copy(isBusy = true, errorMessage = null)
            val fileName = when {
                includeData && includeSettings -> GoogleDriveBackupManager.FILE_FULL_BACKUP
                includeData -> GoogleDriveBackupManager.FILE_CONTACTS_AND_HISTORY
                else -> GoogleDriveBackupManager.FILE_SETTINGS_ONLY
            }
            val payload = backupManager.buildBackupJsonString(
                includeData = includeData,
                includeSettings = includeSettings
            )
            driveBackup.uploadJsonBackup(token, fileName, payload).onSuccess { uploaded ->
                val now = System.currentTimeMillis()
                prefs.setDriveConnectionInfo(_driveUiState.value.connectedEmail, lastBackupAtMs = now)
                val updatedFiles = driveBackup.listBackups(token).getOrDefault(_driveUiState.value.availableBackups)
                val label = when {
                    includeData && includeSettings -> "Contacts, Call History & All Settings"
                    includeData -> "Contacts & Call History"
                    else -> "All App Settings"
                }
                _driveUiState.value = _driveUiState.value.copy(
                    isBusy = false,
                    availableBackups = updatedFiles,
                    statusDetail = "Backed up $label to '${GoogleDriveBackupManager.DRIVE_FOLDER_DISPLAY_NAME}/${uploaded.name}'"
                )
                _statusMessage.value = "Saved $label to Google Drive."
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Google Drive backup failed."
                if (msg.contains("401")) cachedDriveToken = null
                _driveUiState.value = _driveUiState.value.copy(isBusy = false, errorMessage = msg)
                _statusMessage.value = msg
            }
        }
    }

    fun restoreFromGoogleDrive(
        fileId: String,
        restoreData: Boolean = true,
        restoreSettings: Boolean = true
    ) {
        val token = cachedDriveToken
        if (token.isNullOrBlank()) {
            connectGoogleDrive { restoreFromGoogleDrive(fileId, restoreData, restoreSettings) }
            return
        }
        viewModelScope.launch {
            _driveUiState.value = _driveUiState.value.copy(isBusy = true, errorMessage = null)
            driveBackup.downloadBackupContent(token, fileId).onSuccess { rawJson ->
                val summary = backupManager.restoreFromJsonString(
                    raw = rawJson,
                    restoreData = restoreData,
                    restoreSettings = restoreSettings
                )
                DotDialContactWidgetProvider.refreshAll(getApplication())
                val msg = buildString {
                    append("Restored from Drive: ")
                    val parts = mutableListOf<String>()
                    if (restoreData) {
                        parts.add("${summary.restoredContacts} contacts")
                        parts.add("${summary.restoredCallLogs} calls")
                    }
                    if (restoreSettings && summary.restoredSettingsCount > 0) {
                        parts.add("${summary.restoredSettingsCount} settings")
                    }
                    append(parts.joinToString(", ").ifBlank { "complete" })
                }
                _driveUiState.value = _driveUiState.value.copy(isBusy = false, statusDetail = msg)
                _statusMessage.value = msg
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Google Drive restore failed."
                _driveUiState.value = _driveUiState.value.copy(isBusy = false, errorMessage = msg)
                _statusMessage.value = msg
            }
        }
    }

    fun disconnectGoogleDrive() {
        cachedDriveToken = null
        viewModelScope.launch {
            prefs.setDriveConnectionInfo("")
            _driveUiState.value = GoogleDriveUiState()
            _statusMessage.value = "Disconnected Google Drive session."
        }
    }

    private suspend fun findMatchingContact(phoneNumber: String): ContactEntity? {
        val normalized = T9Utils.normalizePhone(phoneNumber)
        if (normalized.isBlank()) return null
        val lookup = T9Utils.buildNumberLookup(contacts.value)
        lookup[normalized]?.let { return it }
        db.contactDao().findByNormalizedNumber(normalized)?.let { return it }
        val tail = if (normalized.length > 10) normalized.takeLast(10) else normalized
        if (tail.length >= 7) {
            contacts.value.firstOrNull { c ->
                c.allNormalizedNumbers().any { n ->
                    n == normalized || (n.length >= 7 && n.takeLast(10) == tail)
                }
            }?.let { return it }
        }
        return null
    }

    fun placeCall(rawNumber: String, explicitContact: ContactEntity? = null) {
        val cleaned = rawNumber.trim()
        if (cleaned.isEmpty()) {
            _statusMessage.value = "Enter a phone number or T9 contact first."
            return
        }

        viewModelScope.launch {
            playUiClick()
            val normalized = T9Utils.normalizePhone(cleaned)
            val contact = explicitContact ?: findMatchingContact(cleaned)
            val verdict = spamEngine.evaluateNumber(cleaned, contact?.name.orEmpty())
            val curSettings = settings.value
            val backdrop = CallSessionBridge.resolveBackdropSpec(contact, curSettings)
            val initialRoute = audioRoutes.resolvePreferredRoute(curSettings.audioOutputMode)
            audioRoutes.applyRoute(initialRoute)

            val logId = db.callLogDao().insert(
                CallLogEntity(
                    contactId = contact?.id,
                    displayName = contact?.name ?: cleaned,
                    phoneNumber = cleaned,
                    normalizedNumber = normalized,
                    direction = CallDirection.OUTGOING,
                    durationSeconds = 0,
                    isSpam = verdict.isSpam,
                    spamReason = if (verdict.isSpam) verdict.reason else ""
                )
            )
            currentCallLogId = logId

            val placedOnSystem = CallSessionBridge.placeSystemCallIfSupported(getApplication(), cleaned)
            if (placedOnSystem && CallSessionBridge.isDefaultDialer(getApplication())) {
                CallSessionBridge.enrichWithContact(
                    contact = contact,
                    isSpam = verdict.isSpam,
                    spamScore = verdict.riskScore,
                    spamReason = verdict.reason,
                    backdrop = backdrop,
                    defaultVibrationId = curSettings.activeVibrationPatternId
                )
            } else {
                CallSessionBridge.startInAppCallSession(
                    displayName = contact?.name ?: cleaned,
                    phoneNumber = cleaned,
                    contact = contact,
                    isIncoming = false,
                    compactBannerWhenIncoming = false,
                    isSpam = verdict.isSpam,
                    spamScore = verdict.riskScore,
                    spamReason = verdict.reason,
                    backdrop = backdrop,
                    defaultVibrationId = curSettings.activeVibrationPatternId,
                    initialAudioRoute = initialRoute,
                    bluetoothConnected = audioRoutes.bluetoothConnected.value
                )
                startElapsedTimer()
                maybeTriggerAutoRecord(contact == null)
            }
            _dialerQuery.value = ""
        }
    }

    fun placeVideoCall(rawNumber: String, explicitContact: ContactEntity? = null) {
        val cleaned = rawNumber.trim()
        if (cleaned.isEmpty()) {
            _statusMessage.value = "Phone number unavailable for video call."
            return
        }
        viewModelScope.launch {
            playUiClick()
            val normalized = T9Utils.normalizePhone(cleaned)
            val contact = explicitContact ?: findMatchingContact(cleaned)
            val verdict = spamEngine.evaluateNumber(cleaned, contact?.name.orEmpty())
            val curSettings = settings.value
            val backdrop = CallSessionBridge.resolveBackdropSpec(contact, curSettings)

            val logId = db.callLogDao().insert(
                CallLogEntity(
                    contactId = contact?.id,
                    displayName = contact?.name ?: cleaned,
                    phoneNumber = cleaned,
                    normalizedNumber = normalized,
                    direction = CallDirection.OUTGOING,
                    durationSeconds = 0,
                    isSpam = verdict.isSpam,
                    spamReason = if (verdict.isSpam) verdict.reason else ""
                )
            )
            currentCallLogId = logId

            val placedVideo = CallSessionBridge.placeVideoCallIfSupported(getApplication(), cleaned)
            if (placedVideo) {
                CallSessionBridge.enrichWithContact(
                    contact = contact,
                    isSpam = verdict.isSpam,
                    spamScore = verdict.riskScore,
                    spamReason = verdict.reason,
                    backdrop = backdrop,
                    defaultVibrationId = curSettings.activeVibrationPatternId
                )
            } else {
                _statusMessage.value = "Carrier video calling unavailable or CALL_PHONE permission missing."
            }
        }
    }

    fun simulateIncomingCall(spamTest: Boolean = false, compactPopup: Boolean = true) {
        viewModelScope.launch {
            val curSettings = settings.value
            val contactList = contacts.value
            val chosenContact = if (!spamTest) contactList.firstOrNull { !it.isBlocked } else null
            val number = when {
                spamTest -> "+18005550199"
                chosenContact != null -> chosenContact.phoneNumber
                else -> "+1 415 890 3310"
            }
            val displayName = when {
                spamTest -> "Telemarketer Alert"
                chosenContact != null -> chosenContact.name
                else -> "Phone (3a) Caller"
            }

            val verdict = spamEngine.evaluateNumber(number, displayName)
            val vibId = chosenContact?.vibrationPatternId ?: curSettings.activeVibrationPatternId
            val preferredRoute = audioRoutes.resolvePreferredRoute(curSettings.audioOutputMode)
            val backdrop = CallSessionBridge.resolveBackdropSpec(chosenContact, curSettings)

            CallSessionBridge.startInAppCallSession(
                displayName = displayName,
                phoneNumber = number,
                contact = chosenContact,
                isIncoming = true,
                compactBannerWhenIncoming = compactPopup,
                isSpam = verdict.isSpam || spamTest,
                spamScore = if (spamTest) maxOf(verdict.riskScore, 91) else verdict.riskScore,
                spamReason = if (spamTest && verdict.reason.isBlank()) "Automated Robocall Signature" else verdict.reason,
                backdrop = backdrop,
                defaultVibrationId = vibId,
                initialAudioRoute = preferredRoute,
                bluetoothConnected = audioRoutes.bluetoothConnected.value
            )
            ringtonePlayer.startRinging(curSettings.customRingtoneUri)
            playVibrationById(vibId, repeat = true)

            // Auto-expire unattended ring after 30 seconds
            ringTimeoutJob?.cancel()
            ringTimeoutJob = viewModelScope.launch {
                delay(30_000L)
                if (callSession.value.lifecycle == CallLifecycleState.INCOMING_RINGING) {
                    endActiveCall()
                }
            }

            scheduleAiAutoAnswerIfNeeded(isUnknownCaller = chosenContact == null)
        }
    }

    fun answerActiveCall(toStatusBar: Boolean = false) {
        voice.stopListening()
        ringtonePlayer.stop()
        vibEngine.stop()
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
        aiAutoAnswerJob?.cancel()
        aiAutoAnswerJob = null
        CallSessionBridge.updateAiCountdown(0)

        val curSettings = settings.value
        val route = audioRoutes.resolvePreferredRoute(curSettings.audioOutputMode)
        audioRoutes.applyRoute(route)
        CallSessionBridge.setAudioRoute(getApplication(), route)

        if (CallSessionBridge.answerCall(acceptToStatusBar = toStatusBar)) {
            startElapsedTimer()
            val s = callSession.value
            maybeTriggerAutoRecord(s.contactId == null)
            CallNotificationManager.showOrUpdateCallNotification(
                context = getApplication(),
                callerName = s.displayName,
                phoneNumber = s.phoneNumber,
                isRinging = false,
                isSpeakerOn = s.isSpeakerOn,
                callStartMs = s.callStartedAtMs
            )
        }
    }

    fun swipeUpMuteIncomingCall() {
        ringtonePlayer.stop()
        vibEngine.stop()
        if (CallSessionBridge.swipeUpMuteIncoming()) {
            val s = callSession.value
            CallNotificationManager.showOrUpdateCallNotification(
                context = getApplication(),
                callerName = s.displayName,
                phoneNumber = s.phoneNumber,
                isRinging = true,
                isSpeakerOn = false,
                callStartMs = 0L
            )
        }
    }

    fun toggleIncomingRingMuteInPlace() {
        val s = callSession.value
        if (s.lifecycle != CallLifecycleState.INCOMING_RINGING) return
        val nextMuted = !s.isRingMuted
        CallSessionBridge.setRingMutedInPlace(nextMuted)
        if (nextMuted) {
            ringtonePlayer.stop()
            vibEngine.stop()
        } else {
            val cur = settings.value
            ringtonePlayer.startRinging(cur.customRingtoneUri)
            playVibrationById(s.vibrationPatternId, repeat = true)
        }
    }

    fun openFullCallScreen() {
        CallSessionBridge.setDisplayMode(CallDisplayMode.FULL_SCREEN)
    }

    fun collapseCallToStatusBar() {
        CallSessionBridge.setDisplayMode(CallDisplayMode.STATUS_BAR_PILL)
    }

    fun endActiveCall() {
        ringtonePlayer.stop()
        vibEngine.stop()
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
        aiAutoAnswerJob?.cancel()
        aiAutoAnswerJob = null
        aiConversationJob?.cancel()
        aiConversationJob = null
        aiAssistantEngine.stopAll()
        voiceChangerEngine.stopLiveDsp()
        stopElapsedTimer()
        voice.stopListening()
        CallNotificationManager.cancel(getApplication())

        var savedFile: String? = null
        var recordedDuration = callSession.value.elapsedSeconds

        if (recorder.isRecording.value) {
            val (file, recDur) = recorder.stopRecording()
            savedFile = file?.absolutePath
            if (recDur > recordedDuration) recordedDuration = recDur
        }
        CallSessionBridge.updateRecordingState(false, savedFile)

        // End means END immediately
        val endedSnapshot = CallSessionBridge.endCall() ?: return

        viewModelScope.launch {
            val normalized = T9Utils.normalizePhone(endedSnapshot.phoneNumber)
            val logId = currentCallLogId ?: db.callLogDao().insert(
                CallLogEntity(
                    contactId = endedSnapshot.contactId,
                    displayName = endedSnapshot.displayName,
                    phoneNumber = endedSnapshot.phoneNumber,
                    normalizedNumber = normalized,
                    direction = endedSnapshot.lastDirection,
                    durationSeconds = endedSnapshot.elapsedSeconds,
                    isSpam = endedSnapshot.isSpamWarning,
                    spamReason = endedSnapshot.spamReason
                )
            )
            if (currentCallLogId != null && endedSnapshot.elapsedSeconds > 0) {
                db.callLogDao().insert(
                    CallLogEntity(
                        id = logId,
                        contactId = endedSnapshot.contactId,
                        displayName = endedSnapshot.displayName,
                        phoneNumber = endedSnapshot.phoneNumber,
                        normalizedNumber = normalized,
                        direction = endedSnapshot.lastDirection,
                        durationSeconds = endedSnapshot.elapsedSeconds,
                        isSpam = endedSnapshot.isSpamWarning,
                        spamReason = endedSnapshot.spamReason
                    )
                )
            }
            currentCallLogId = null

            val aiTranscriptBlock = if (endedSnapshot.aiTranscript.isNotEmpty()) {
                buildString {
                    if (endedSnapshot.aiCallerPurposeTag.isNotBlank()) {
                        appendLine("AI Call Screening Purpose: ${endedSnapshot.aiCallerPurposeTag}")
                    }
                    endedSnapshot.aiTranscript.forEach { turn ->
                        appendLine("${turn.speaker} (${turn.languageCode}): ${turn.text}")
                    }
                }.trim()
            } else {
                ""
            }
            val notes = endedSnapshot.miniNotepadText.trim()
            val combinedNotes = listOf(aiTranscriptBlock, notes)
                .filter { it.isNotBlank() }
                .joinToString("\n\n")

            if (!savedFile.isNullOrBlank() ||
                combinedNotes.isNotEmpty() ||
                endedSnapshot.isAiAssistantActive ||
                settings.value.autoSummarizeEnabled
            ) {
                val recId = db.recordingDao().insert(
                    CallRecordingEntity(
                        callLogId = logId,
                        contactName = endedSnapshot.displayName,
                        phoneNumber = endedSnapshot.phoneNumber,
                        filePath = savedFile.orEmpty(),
                        durationSeconds = recordedDuration,
                        transcriptText = combinedNotes,
                        isLocked = settings.value.vaultPin.isNotBlank()
                    )
                )
                db.callLogDao().attachRecording(logId, recId)
                if ((endedSnapshot.isAiAssistantActive && combinedNotes.isNotBlank()) ||
                    (settings.value.autoSummarizeEnabled && endedSnapshot.elapsedSeconds > 0)
                ) {
                    summarizeRecording(recId, combinedNotes.ifBlank { null })
                }
            }
        }
    }

    // Gemini AI Call Assistant Pipeline
    private fun resolveOwnerDisplayName(curSettings: DialerSettings): String {
        val explicit = curSettings.aiAssistantOwnerName.trim()
        if (explicit.isNotEmpty()) return explicit
        val signedInName = curSettings.signedInGoogleName.trim()
        if (signedInName.isNotEmpty()) return signedInName
        val driveName = _driveUiState.value.displayName.trim()
        if (driveName.isNotEmpty()) return driveName
        val accountEmail = curSettings.signedInGoogleEmail
            .ifBlank { curSettings.driveConnectedEmail }
            .ifBlank { curSettings.syncedGoogleAccounts.firstOrNull().orEmpty() }
            .ifBlank { _discoveredGoogleAccounts.value.firstOrNull().orEmpty() }
        if (accountEmail.contains("@")) {
            val local = accountEmail.substringBefore("@").replace('.', ' ').replace('_', ' ').trim()
            if (local.isNotEmpty()) {
                return local.split(" ").filter { it.isNotBlank() }
                    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
            }
        }
        return DeviceNameHelper.resolveDeviceShortName(getApplication()) + " Owner"
    }

    private fun scheduleAiAutoAnswerIfNeeded(isUnknownCaller: Boolean) {
        val cur = settings.value
        if (!cur.aiAssistantEnabled) return
        val delaySec = when (cur.aiAssistantTriggerMode) {
            AiAssistantTriggerMode.ALL_INCOMING -> 2
            AiAssistantTriggerMode.AFTER_DELAY -> cur.aiAssistantDelaySeconds
            AiAssistantTriggerMode.UNKNOWN_ONLY -> if (isUnknownCaller) cur.aiAssistantDelaySeconds else return
            AiAssistantTriggerMode.BUSY_MODE_ONLY -> if (cur.aiBusyModeEnabled) 2 else return
        }

        aiAutoAnswerJob?.cancel()
        aiAutoAnswerJob = viewModelScope.launch {
            for (sec in delaySec downTo 1) {
                if (callSession.value.lifecycle != CallLifecycleState.INCOMING_RINGING) {
                    CallSessionBridge.updateAiCountdown(0)
                    return@launch
                }
                CallSessionBridge.updateAiCountdown(sec)
                delay(1000L)
            }
            CallSessionBridge.updateAiCountdown(0)
            if (callSession.value.lifecycle == CallLifecycleState.INCOMING_RINGING) {
                startAiAssistantForCall()
            }
        }
    }

    fun cancelAiAutoAnswerCountdown() {
        aiAutoAnswerJob?.cancel()
        aiAutoAnswerJob = null
        CallSessionBridge.clearAiAssistantState()
    }

    fun startAiAssistantForCall() {
        val session = callSession.value
        if (session.lifecycle == CallLifecycleState.IDLE) return

        ringtonePlayer.stop()
        vibEngine.stop()
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
        aiAutoAnswerJob?.cancel()
        aiAutoAnswerJob = null
        voice.stopListening()

        val curSettings = settings.value
        val ownerName = resolveOwnerDisplayName(curSettings)

        if (session.lifecycle == CallLifecycleState.INCOMING_RINGING) {
            CallSessionBridge.answerCall(acceptToStatusBar = false)
            startElapsedTimer()
        } else {
            CallSessionBridge.setDisplayMode(CallDisplayMode.FULL_SCREEN)
        }

        // Route over Speakerphone so caller hears TTS audio over acoustic coupling
        applyCallAudioRoute(ActiveAudioRoute.SPEAKER)

        if (curSettings.aiAssistantAutoRecord &&
            recorder.hasRecordPermission() &&
            !recorder.isRecording.value
        ) {
            recorder.startRecording(playConsentBeep = !curSettings.silentCallRecording)
                .onSuccess { file -> CallSessionBridge.updateRecordingState(true, file.absolutePath) }
        }

        val (greeting, initialLang) = aiAssistantEngine.buildInitialGreeting(
            ownerName = ownerName,
            languageMode = curSettings.aiAssistantLanguageMode
        )

        CallSessionBridge.updateAiAssistantState(
            active = true,
            phase = AiCallPhase.GREETING,
            languageCode = initialLang.code,
            ownerName = ownerName
        )
        CallSessionBridge.appendAiTranscriptTurn("AI", greeting, initialLang.code)

        val (aiPitch, aiRate) = resolveAiVoicePitchAndRate(curSettings)
        aiAssistantEngine.speak(
            text = greeting,
            language = initialLang,
            isPreview = false,
            pitch = aiPitch,
            speechRate = aiRate
        ) {
            val latest = callSession.value
            if (latest.lifecycle == CallLifecycleState.ACTIVE &&
                latest.isAiAssistantActive &&
                latest.aiPhase != AiCallPhase.HANDED_OVER
            ) {
                listenNextAiAssistantTurn(initialLang)
            }
        }
    }

    private fun listenNextAiAssistantTurn(languageHint: AssistantReplyLanguage) {
        val latest = callSession.value
        if (latest.lifecycle != CallLifecycleState.ACTIVE ||
            !latest.isAiAssistantActive ||
            latest.aiPhase == AiCallPhase.HANDED_OVER
        ) {
            return
        }

        val curSettings = settings.value
        CallSessionBridge.updateAiAssistantState(
            active = true,
            phase = AiCallPhase.LISTENING,
            languageCode = languageHint.code
        )
        aiAssistantEngine.listenForCallerTurn(
            languageHint = languageHint,
            languageMode = curSettings.aiAssistantLanguageMode,
            onSpoken = { callerText ->
                processAiCallerUtterance(callerText)
            },
            onSilenceOrError = {
                // Keep listening cleanly if call stays active without spamming
                aiConversationJob?.cancel()
                aiConversationJob = viewModelScope.launch {
                    delay(1200L)
                    val s = callSession.value
                    if (s.lifecycle == CallLifecycleState.ACTIVE &&
                        s.isAiAssistantActive &&
                        s.aiPhase == AiCallPhase.LISTENING &&
                        s.aiTranscript.size < 14
                    ) {
                        listenNextAiAssistantTurn(languageHint)
                    }
                }
            }
        )
    }

    fun sendSimulatedCallerReplyToAi(callerUtterance: String) {
        val clean = callerUtterance.trim()
        if (clean.isEmpty()) return
        aiAssistantEngine.stopAll()
        processAiCallerUtterance(clean)
    }

    private fun processAiCallerUtterance(callerSpeech: String) {
        val session = callSession.value
        if (session.lifecycle != CallLifecycleState.ACTIVE ||
            !session.isAiAssistantActive ||
            session.aiPhase == AiCallPhase.HANDED_OVER
        ) {
            return
        }

        val curLang = AssistantReplyLanguage.fromCode(session.aiActiveLanguageCode)
        CallSessionBridge.appendAiTranscriptTurn("Caller", callerSpeech, curLang.code)
        CallSessionBridge.updateAiAssistantState(
            active = true,
            phase = AiCallPhase.THINKING,
            languageCode = curLang.code
        )

        val curSettings = settings.value
        val ownerName = session.aiOwnerName.ifBlank { resolveOwnerDisplayName(curSettings) }
        val historyLines = callSession.value.aiTranscript.map { "${it.speaker}: ${it.text}" }

        aiConversationJob?.cancel()
        aiConversationJob = viewModelScope.launch {
            val result = gemini.generateCallAssistantReply(
                ownerName = ownerName,
                callerName = session.displayName,
                phoneNumber = session.phoneNumber,
                languageMode = curSettings.aiAssistantLanguageMode,
                historyLines = historyLines,
                latestCallerSpeech = callerSpeech
            )

            result.onSuccess { turn ->
                val cur = callSession.value
                if (cur.lifecycle != CallLifecycleState.ACTIVE ||
                    !cur.isAiAssistantActive ||
                    cur.aiPhase == AiCallPhase.HANDED_OVER
                ) {
                    return@onSuccess
                }
                CallSessionBridge.updateAiAssistantState(
                    active = true,
                    phase = AiCallPhase.SPEAKING,
                    languageCode = turn.language.code,
                    ownerName = ownerName,
                    purposeTag = turn.callerPurposeTag
                )
                CallSessionBridge.appendAiTranscriptTurn("AI", turn.replyText, turn.language.code)
                val (aiPitch, aiRate) = resolveAiVoicePitchAndRate(curSettings)
                aiAssistantEngine.speak(
                    text = turn.replyText,
                    language = turn.language,
                    isPreview = false,
                    pitch = aiPitch,
                    speechRate = aiRate
                ) {
                    val afterSpeak = callSession.value
                    if (afterSpeak.lifecycle == CallLifecycleState.ACTIVE &&
                        afterSpeak.isAiAssistantActive &&
                        afterSpeak.aiPhase != AiCallPhase.HANDED_OVER
                    ) {
                        if (turn.shouldEndCall) {
                            viewModelScope.launch {
                                delay(900L)
                                endActiveCall()
                            }
                        } else {
                            listenNextAiAssistantTurn(turn.language)
                        }
                    }
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Gemini AI Assistant reply failed."
                val cur = callSession.value
                if (cur.lifecycle == CallLifecycleState.ACTIVE &&
                    cur.isAiAssistantActive &&
                    cur.aiPhase != AiCallPhase.HANDED_OVER
                ) {
                    listenNextAiAssistantTurn(curLang)
                }
            }
        }
    }

    fun takeOverFromAiAssistant() {
        aiAutoAnswerJob?.cancel()
        aiAutoAnswerJob = null
        aiConversationJob?.cancel()
        aiConversationJob = null
        aiAssistantEngine.stopAll()
        CallSessionBridge.updateAiAssistantState(
            active = true,
            phase = AiCallPhase.HANDED_OVER
        )
        val preferred = audioRoutes.resolvePreferredRoute(settings.value.audioOutputMode)
        applyCallAudioRoute(preferred)
        syncActiveVoiceChangerState(callSession.value, settings.value)
        _statusMessage.value = "You took over the call from AI Assistant."
    }

    fun previewAiAssistantVoice(language: AssistantReplyLanguage) {
        val cur = settings.value
        val owner = resolveOwnerDisplayName(cur)
        val (pitch, rate) = resolveAiVoicePitchAndRate(cur)
        aiAssistantEngine.previewSampleVoice(
            ownerName = owner,
            language = language,
            pitch = pitch,
            speechRate = rate
        ) { msg ->
            _statusMessage.value = msg
        }
    }

    private fun resolveAiVoicePitchAndRate(curSettings: DialerSettings): Pair<Float, Float> {
        val mode = curSettings.voiceChangerMode
        val preset = curSettings.voiceChangerPreset
        return if ((mode == VoiceChangerSettingsMode.LIVE_DSP_AND_AI_SYNC ||
                mode == VoiceChangerSettingsMode.AI_PERSONA_ONLY) &&
            preset != VoiceChangerPreset.NORMAL
        ) {
            preset.ttsPitch to preset.ttsRate
        } else {
            1.0f to 0.96f
        }
    }

    private fun syncActiveVoiceChangerState(
        session: CallSessionState,
        curSettings: DialerSettings,
        overridePreset: VoiceChangerPreset? = null
    ) {
        val activePreset = overridePreset ?: curSettings.voiceChangerPreset
        val mode = curSettings.voiceChangerMode
        val modeAllowsDsp = mode == VoiceChangerSettingsMode.LIVE_DSP_AND_AI_SYNC ||
            mode == VoiceChangerSettingsMode.LIVE_DSP_ONLY
        val shouldRunDsp = session.lifecycle == CallLifecycleState.ACTIVE &&
            !session.isMuted &&
            (!session.isAiAssistantActive || session.aiPhase == AiCallPhase.HANDED_OVER) &&
            modeAllowsDsp &&
            activePreset != VoiceChangerPreset.NORMAL

        val running = if (shouldRunDsp) {
            voiceChangerEngine.updatePreset(activePreset)
            voiceChangerEngine.startLiveDsp(activePreset)
        } else {
            voiceChangerEngine.updatePreset(activePreset)
            voiceChangerEngine.stopLiveDsp()
            false
        }
        CallSessionBridge.updateVoiceChangerState(activePreset, running)
    }

    fun setVoiceChangerMode(mode: VoiceChangerSettingsMode) {
        playUiClick()
        viewModelScope.launch {
            prefs.setVoiceChangerMode(mode)
            val updated = settings.value.copy(voiceChangerMode = mode)
            syncActiveVoiceChangerState(callSession.value, updated)
        }
    }

    fun setVoiceChangerPreset(preset: VoiceChangerPreset) {
        playUiClick()
        viewModelScope.launch {
            prefs.setVoiceChangerPreset(preset)
            val updated = settings.value.copy(voiceChangerPreset = preset)
            syncActiveVoiceChangerState(callSession.value, updated, overridePreset = preset)
            if (callSession.value.lifecycle == CallLifecycleState.ACTIVE) {
                _statusMessage.value = if (preset == VoiceChangerPreset.NORMAL) {
                    "Voice FX reset to Natural."
                } else {
                    "Voice FX active: ${preset.label}"
                }
            }
        }
    }

    fun previewVoiceChangerPreset(preset: VoiceChangerPreset) {
        playUiClick()
        voiceChangerEngine.previewPreset(preset)
        _statusMessage.value = "Previewing Voice FX: ${preset.label}"
    }

    private fun maybeTriggerAutoRecord(isUnknownCaller: Boolean) {
        val mode = settings.value.autoRecordMode
        val shouldRecord = when (mode) {
            AutoRecordMode.OFF -> false
            AutoRecordMode.UNKNOWN_ONLY -> isUnknownCaller
            AutoRecordMode.ALL_CALLS -> true
        }
        if (shouldRecord && recorder.hasRecordPermission() && !recorder.isRecording.value) {
            toggleCallRecording()
        }
    }

    fun toggleCallRecording() {
        if (recorder.isRecording.value) {
            val (file, _) = recorder.stopRecording()
            CallSessionBridge.updateRecordingState(false, file?.absolutePath)
            _statusMessage.value = "Privacy recording saved locally."
        } else {
            val playBeep = !settings.value.silentCallRecording
            val result = recorder.startRecording(playBeep)
            result.onSuccess { file ->
                CallSessionBridge.updateRecordingState(true, file.absolutePath)
                if (!settings.value.silentCallRecording) {
                    _statusMessage.value = "Local recording started."
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Microphone permission needed to record."
            }
        }
    }

    fun toggleMute() {
        val next = !callSession.value.isMuted
        CallSessionBridge.setMuted(getApplication(), next)
        syncActiveVoiceChangerState(callSession.value.copy(isMuted = next), settings.value)
    }

    fun toggleSpeaker() {
        val nextRoute = if (callSession.value.activeAudioRoute == ActiveAudioRoute.SPEAKER) {
            if (audioRoutes.bluetoothConnected.value) ActiveAudioRoute.BLUETOOTH else ActiveAudioRoute.EAR_RECEIVER
        } else {
            ActiveAudioRoute.SPEAKER
        }
        applyCallAudioRoute(nextRoute)
    }

    fun cycleCallAudioRoute() {
        val bt = audioRoutes.bluetoothConnected.value
        val cur = callSession.value.activeAudioRoute
        val next = when (cur) {
            ActiveAudioRoute.BLUETOOTH -> ActiveAudioRoute.EAR_RECEIVER
            ActiveAudioRoute.EAR_RECEIVER -> ActiveAudioRoute.SPEAKER
            ActiveAudioRoute.SPEAKER -> if (bt) ActiveAudioRoute.BLUETOOTH else ActiveAudioRoute.EAR_RECEIVER
        }
        applyCallAudioRoute(next)
    }

    fun applyCallAudioRoute(route: ActiveAudioRoute) {
        audioRoutes.applyRoute(route)
        CallSessionBridge.setAudioRoute(getApplication(), route)
        val s = callSession.value
        if (s.lifecycle == CallLifecycleState.ACTIVE) {
            CallNotificationManager.showOrUpdateCallNotification(
                context = getApplication(),
                callerName = s.displayName,
                phoneNumber = s.phoneNumber,
                isRinging = false,
                isSpeakerOn = route == ActiveAudioRoute.SPEAKER,
                callStartMs = s.callStartedAtMs
            )
        }
    }

    fun updateCallNotes(notes: String) {
        CallSessionBridge.updateMiniNotepad(notes)
    }

    fun sendInCallDtmf(digit: String) {
        vibEngine.pulseKeypadTick()
        val toneStyle = settings.value.dialpadSoundStyle.let {
            if (it == DialpadSoundStyle.OFF) DialpadSoundStyle.CLASSIC_DTMF else it
        }
        uiSounds.playDialTone(digit, toneStyle)
        CallSessionBridge.sendInCallDtmf(digit)
        dtmfStopJob?.cancel()
        dtmfStopJob = viewModelScope.launch {
            delay(150L)
            CallSessionBridge.stopSystemDtmfTone()
        }
    }

    fun clearInCallDtmf() {
        playUiClick()
        CallSessionBridge.clearInCallDtmfDigits()
    }

    fun summarizeRecording(recordingId: Long, customTranscript: String? = null) {
        viewModelScope.launch {
            val entry = db.recordingDao().getById(recordingId) ?: return@launch
            val textToSummarize = customTranscript?.trim()?.takeIf { it.isNotBlank() }
                ?: entry.transcriptText.ifBlank {
                    "Call with ${entry.contactName} (${entry.phoneNumber}) lasting ${entry.durationSeconds} seconds."
                }

            _summaryState.value = RecordingSummaryUiState.Loading(recordingId)
            val result = gemini.summarizeConversation(
                contactName = entry.contactName,
                phoneNumber = entry.phoneNumber,
                durationSeconds = entry.durationSeconds,
                transcriptOrNotes = textToSummarize
            )

            result.onSuccess { summary ->
                val bulletsJson = JSONArray(summary.bulletPoints).toString()
                val actionsJson = JSONArray(summary.actionItems).toString()
                db.recordingDao().update(
                    entry.copy(
                        transcriptText = textToSummarize,
                        aiHeadline = summary.headline,
                        aiSummaryBulletsJson = bulletsJson,
                        aiActionItemsJson = actionsJson,
                        aiSentiment = summary.sentiment
                    )
                )
                _summaryState.value = RecordingSummaryUiState.Idle
                _statusMessage.value = "Gemini summary saved to Vault."
            }.onFailure { err ->
                _summaryState.value = RecordingSummaryUiState.Error(
                    recordingId = recordingId,
                    message = err.localizedMessage ?: "Gemini summarization failed."
                )
            }
        }
    }

    fun startVoiceControlListening() {
        voice.startListening { spoken ->
            executeAiOrVoiceCommand(spoken)
        }
    }

    fun stopVoiceControlListening() {
        voice.stopListening()
    }

    fun executeAiOrVoiceCommand(rawCommand: String) {
        val command = rawCommand.trim()
        if (command.isEmpty()) return

        val fastAction = VoiceCommandController.detectFastCallAction(command)
        val session = callSession.value
        when (fastAction) {
            FastCallVoiceAction.RECEIVE_CALL -> {
                if (session.lifecycle == CallLifecycleState.INCOMING_RINGING) {
                    val toBar = session.displayMode == CallDisplayMode.COMPACT_BANNER ||
                        session.displayMode == CallDisplayMode.RINGING_MUTED_STATUS
                    answerActiveCall(toStatusBar = toBar)
                    _aiCommandState.value = AiControlUiState.Success(
                        message = "Call answered via voice command.",
                        executedAction = "ANSWER_CALL"
                    )
                    return
                }
            }
            FastCallVoiceAction.END_CALL -> {
                if (session.lifecycle != CallLifecycleState.IDLE) {
                    endActiveCall()
                    _aiCommandState.value = AiControlUiState.Success(
                        message = "Call ended via voice command.",
                        executedAction = "END_CALL"
                    )
                    return
                }
            }
            FastCallVoiceAction.MUTE_CALL -> {
                if (session.lifecycle == CallLifecycleState.INCOMING_RINGING) {
                    swipeUpMuteIncomingCall()
                    _aiCommandState.value = AiControlUiState.Success("Incoming ring muted.", "MUTE_RING")
                    return
                } else if (session.lifecycle == CallLifecycleState.ACTIVE) {
                    toggleMute()
                    _aiCommandState.value = AiControlUiState.Success("Microphone mute toggled.", "MUTE_MIC")
                    return
                }
            }
            FastCallVoiceAction.TOGGLE_SPEAKER_ON -> {
                if (session.lifecycle == CallLifecycleState.ACTIVE) {
                    applyCallAudioRoute(ActiveAudioRoute.SPEAKER)
                    _aiCommandState.value = AiControlUiState.Success("Speaker enabled.", "SPEAKER_ON")
                    return
                }
            }
            FastCallVoiceAction.TOGGLE_SPEAKER_OFF -> {
                if (session.lifecycle == CallLifecycleState.ACTIVE) {
                    applyCallAudioRoute(
                        if (audioRoutes.bluetoothConnected.value) ActiveAudioRoute.BLUETOOTH
                        else ActiveAudioRoute.EAR_RECEIVER
                    )
                    _aiCommandState.value = AiControlUiState.Success("Speaker disabled.", "SPEAKER_OFF")
                    return
                }
            }
            FastCallVoiceAction.START_RECORDING -> {
                if (session.lifecycle == CallLifecycleState.ACTIVE) {
                    if (!recorder.isRecording.value) toggleCallRecording()
                    _aiCommandState.value = AiControlUiState.Success("Private recording activated.", "RECORD")
                    return
                }
            }
            FastCallVoiceAction.NONE -> Unit
        }

        // Full Gemini AI command parsing for natural-language dialer control
        viewModelScope.launch {
            _aiCommandState.value = AiControlUiState.Processing(command)
            val result = gemini.parseFullControlCommand(
                commandText = command,
                hasIncomingCall = session.lifecycle == CallLifecycleState.INCOMING_RINGING,
                hasActiveCall = session.lifecycle == CallLifecycleState.ACTIVE,
                knownContacts = contacts.value.map { "${it.name} (${it.phoneNumber})" }
            )

            result.onSuccess { parsed ->
                applyGeminiParsedAction(
                    parsed.intent,
                    parsed.targetQuery,
                    parsed.themeName,
                    parsed.tabName,
                    parsed.confirmationMessage
                )
            }.onFailure { err ->
                _aiCommandState.value = AiControlUiState.Error(
                    err.localizedMessage ?: "AI command failed."
                )
            }
        }
    }

    private suspend fun applyGeminiParsedAction(
        intent: AiCommandIntent,
        targetQuery: String,
        themeName: String,
        tabName: String,
        confirmation: String
    ) {
        when (intent) {
            AiCommandIntent.ANSWER_CALL -> answerActiveCall(toStatusBar = false)
            AiCommandIntent.END_CALL -> endActiveCall()
            AiCommandIntent.PLACE_CALL -> {
                val match = contacts.value.firstOrNull {
                    it.name.contains(targetQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(targetQuery)
                }
                val num = match?.phoneNumber ?: targetQuery.filter { it.isDigit() || it == '+' }
                if (num.isNotBlank()) {
                    placeCall(num, match)
                } else {
                    _aiCommandState.value = AiControlUiState.Error("Couldn't find a number for \"$targetQuery\".")
                    return
                }
            }
            AiCommandIntent.TOGGLE_SPEAKER -> toggleSpeaker()
            AiCommandIntent.TOGGLE_MUTE -> toggleMute()
            AiCommandIntent.TOGGLE_RECORD -> toggleCallRecording()
            AiCommandIntent.SUMMARIZE_LAST_CALL -> {
                val latestRec = db.recordingDao().getLatest()
                if (latestRec != null) {
                    summarizeRecording(latestRec.id)
                    _navigationRequest.value = NavigationRequest(AppRoute.Vault)
                } else {
                    val latestLog = db.callLogDao().getLatest()
                    if (latestLog != null) {
                        val recId = db.recordingDao().insert(
                            CallRecordingEntity(
                                callLogId = latestLog.id,
                                contactName = latestLog.displayName,
                                phoneNumber = latestLog.phoneNumber,
                                filePath = "",
                                durationSeconds = latestLog.durationSeconds,
                                transcriptText = "Call with ${latestLog.displayName} (${latestLog.phoneNumber}) for ${latestLog.durationSeconds}s.",
                                isLocked = false
                            )
                        )
                        summarizeRecording(recId)
                        _navigationRequest.value = NavigationRequest(AppRoute.Vault)
                    } else {
                        _aiCommandState.value = AiControlUiState.Error("No recent call found to summarize.")
                        return
                    }
                }
            }
            AiCommandIntent.BLOCK_NUMBER -> {
                val clean = T9Utils.normalizePhone(targetQuery)
                if (clean.isNotEmpty()) {
                    db.contactDao().setBlockedByNumber(clean, true)
                    db.spamRuleDao().insert(
                        SpamRuleEntity(
                            pattern = clean,
                            label = "Blocked via Gemini AI Control",
                            riskLevel = 95
                        )
                    )
                }
            }
            AiCommandIntent.SET_THEME -> prefs.setThemePreset(DialerThemePreset.fromId(themeName))
            AiCommandIntent.NAVIGATE_TAB -> {
                val route = when (tabName.uppercase(Locale.US)) {
                    "HISTORY" -> AppRoute.History
                    "CONTACTS" -> AppRoute.Contacts
                    "VAULT" -> AppRoute.Vault
                    "ANALYTICS" -> AppRoute.Analytics
                    "SETTINGS", "STUDIO" -> AppRoute.Settings
                    else -> AppRoute.Dialer
                }
                _navigationRequest.value = NavigationRequest(route)
            }
            AiCommandIntent.SIMULATE_INCOMING -> simulateIncomingCall(spamTest = false)
            AiCommandIntent.ANALYTICS_INSIGHT -> _navigationRequest.value = NavigationRequest(AppRoute.Analytics)
            AiCommandIntent.UNKNOWN -> Unit
        }

        _aiCommandState.value = AiControlUiState.Success(
            message = confirmation,
            executedAction = intent.name
        )
    }

    fun resetAiState() {
        _aiCommandState.value = AiControlUiState.Idle
    }

    // Contact Management & New Contact Storage Preferences
    fun setNewContactStorageMode(mode: NewContactStorageMode) {
        playUiClick()
        viewModelScope.launch { prefs.setNewContactStorageMode(mode) }
    }

    fun setPreferredGoogleContactAccount(email: String) {
        playUiClick()
        viewModelScope.launch { prefs.setPreferredGoogleContactAccount(email) }
    }

    fun resolveTargetGoogleContactAccount(cur: DialerSettings): String =
        cur.preferredGoogleContactAccount
            .ifBlank { cur.signedInGoogleEmail }
            .ifBlank { cur.syncedGoogleAccounts.firstOrNull().orEmpty() }
            .ifBlank { _discoveredGoogleAccounts.value.firstOrNull().orEmpty() }
            .trim()

    fun saveContact(
        existingId: Long? = null,
        name: String,
        phoneNumber: String,
        company: String = "",
        email: String = "",
        notes: String = "",
        isFavorite: Boolean = false,
        photoUri: String? = null,
        callingCardUri: String? = null,
        videoBackdropUri: String? = null,
        vibrationPatternId: Long = 1L,
        additionalNumbersJson: String? = null,
        syncToSystem: Boolean = true
    ) {
        if (name.isBlank() || phoneNumber.isBlank()) {
            _statusMessage.value = "Name and phone number are required."
            return
        }
        viewModelScope.launch {
            val cur = settings.value
            val normalized = T9Utils.normalizePhone(phoneNumber)
            val existing = existingId?.let { db.contactDao().findById(it) }
            val resolvedExtras = additionalNumbersJson ?: existing?.additionalNumbersJson ?: "[]"

            val targetGoogleAccount = if (existing == null && cur.newContactStorageMode == NewContactStorageMode.GOOGLE_ACCOUNT) {
                resolveTargetGoogleContactAccount(cur).takeIf { it.isNotBlank() }
            } else null

            val resolvedAccountName = existing?.accountName
                ?: (targetGoogleAccount ?: "Device")
            val resolvedAccountType = existing?.accountType
                ?: if (targetGoogleAccount != null) "com.google" else "local"

            db.contactDao().upsert(
                ContactEntity(
                    id = existing?.id ?: 0L,
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    normalizedNumber = normalized,
                    t9Key = T9Utils.toT9Key(name),
                    company = company.trim(),
                    email = email.trim(),
                    photoUri = photoUri ?: existing?.photoUri,
                    callingCardUri = callingCardUri ?: existing?.callingCardUri,
                    accountName = resolvedAccountName,
                    accountType = resolvedAccountType,
                    isFavorite = isFavorite,
                    isBlocked = existing?.isBlocked ?: false,
                    spamScore = existing?.spamScore ?: 0,
                    videoBackdropUri = videoBackdropUri ?: existing?.videoBackdropUri,
                    backdropPhotoUri = existing?.backdropPhotoUri,
                    callerBackdropMode = existing?.callerBackdropMode ?: "",
                    vibrationPatternId = vibrationPatternId,
                    notes = notes.trim(),
                    additionalNumbersJson = resolvedExtras,
                    systemContactKey = existing?.systemContactKey.orEmpty()
                )
            )
            if (syncToSystem && existing == null) {
                contactsSync.exportContactToSystemIfPermitted(
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    email = email.trim(),
                    googleAccount = targetGoogleAccount
                )
            }
            DotDialContactWidgetProvider.refreshAll(getApplication())
            _statusMessage.value = when {
                existing != null -> "Contact updated."
                targetGoogleAccount != null -> "Contact saved to Google ($targetGoogleAccount)."
                else -> "Contact saved to Device Storage."
            }
        }
    }

    fun setDefaultContactNumber(contact: ContactEntity, newDefault: ContactPhoneNumber) {
        viewModelScope.launch {
            val newNorm = T9Utils.normalizePhone(newDefault.number)
            if (newNorm.isEmpty() || newNorm == contact.normalizedNumber) return@launch
            val rebuiltExtras = contact.allPhoneNumbers()
                .filter { T9Utils.normalizePhone(it.number) != newNorm }
                .map { entry ->
                    if (entry.isDefault) ContactPhoneNumber(entry.number, "Mobile")
                    else entry
                }
            db.contactDao().update(
                contact.copy(
                    phoneNumber = newDefault.number,
                    normalizedNumber = newNorm,
                    additionalNumbersJson = ContactNumbersCodec.encode(rebuiltExtras),
                    updatedAt = System.currentTimeMillis()
                )
            )
            DotDialContactWidgetProvider.refreshAll(getApplication())
            _statusMessage.value = "Default number updated to ${newDefault.number}."
        }
    }

    fun toggleFavorite(contact: ContactEntity) {
        viewModelScope.launch {
            db.contactDao().update(contact.copy(isFavorite = !contact.isFavorite))
            DotDialContactWidgetProvider.refreshAll(getApplication())
        }
    }

    fun toggleBlocked(contact: ContactEntity) {
        viewModelScope.launch {
            db.contactDao().update(contact.copy(isBlocked = !contact.isBlocked))
            DotDialContactWidgetProvider.refreshAll(getApplication())
        }
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            db.contactDao().deleteById(contactId)
            DotDialContactWidgetProvider.refreshAll(getApplication())
            _statusMessage.value = "Contact removed."
        }
    }

    fun assignContactPhoto(contact: ContactEntity, uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            db.contactDao().update(contact.copy(photoUri = uri?.toString()))
            DotDialContactWidgetProvider.refreshAll(getApplication())
            _statusMessage.value = if (uri != null) "Photo updated for ${contact.name}." else "Photo cleared."
        }
    }

    fun assignContactCallingCard(contact: ContactEntity, uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            val nextMode = if (uri != null) {
                CallerBackdropMode.CALLING_CARD.name
            } else if (contact.callerBackdropMode == CallerBackdropMode.CALLING_CARD.name) {
                CallerBackdropMode.NONE.name
            } else {
                contact.callerBackdropMode
            }
            db.contactDao().update(
                contact.copy(
                    callingCardUri = uri?.toString(),
                    callerBackdropMode = nextMode
                )
            )
            _statusMessage.value = if (uri != null) {
                "Calling card enabled exclusively for ${contact.name}."
            } else {
                "Calling card cleared."
            }
        }
    }

    fun assignContactVideoBackdrop(contact: ContactEntity, uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            val nextMode = if (uri != null) {
                CallerBackdropMode.VIDEO.name
            } else if (contact.callerBackdropMode == CallerBackdropMode.VIDEO.name) {
                CallerBackdropMode.NONE.name
            } else {
                contact.callerBackdropMode
            }
            db.contactDao().update(
                contact.copy(
                    videoBackdropUri = uri?.toString(),
                    callerBackdropMode = nextMode
                )
            )
            _statusMessage.value = if (uri != null) {
                "Video backdrop enabled exclusively for ${contact.name}."
            } else {
                "Video backdrop cleared."
            }
        }
    }

    fun assignContactBackdropPhoto(contact: ContactEntity, uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            val nextMode = if (uri != null) {
                CallerBackdropMode.PHOTO.name
            } else if (contact.callerBackdropMode == CallerBackdropMode.PHOTO.name) {
                CallerBackdropMode.NONE.name
            } else {
                contact.callerBackdropMode
            }
            db.contactDao().update(
                contact.copy(
                    backdropPhotoUri = uri?.toString(),
                    callerBackdropMode = nextMode
                )
            )
            _statusMessage.value = if (uri != null) {
                "In-call gallery photo enabled exclusively for ${contact.name}."
            } else {
                "In-call gallery photo cleared."
            }
        }
    }

    fun assignContactBackdropMode(contact: ContactEntity, mode: CallerBackdropMode) {
        viewModelScope.launch {
            db.contactDao().update(contact.copy(callerBackdropMode = mode.name))
            _statusMessage.value = if (mode == CallerBackdropMode.NONE) {
                "Custom caller backdrop turned OFF for ${contact.name}."
            } else {
                "${mode.label} enabled for ${contact.name}."
            }
        }
    }

    fun assignContactVibrationPattern(contact: ContactEntity, patternId: Long) {
        viewModelScope.launch {
            db.contactDao().update(contact.copy(vibrationPatternId = patternId))
            _statusMessage.value = "Vibration glyph updated for ${contact.name}."
        }
    }

    fun deleteCallLog(id: Long) {
        viewModelScope.launch {
            db.callLogDao().deleteById(id)
        }
    }

    fun clearCallHistory() {
        viewModelScope.launch {
            db.callLogDao().clearAll()
            _statusMessage.value = "Call history cleared."
        }
    }

    // Vault & Recordings
    fun unlockVault(pinAttempt: String): Boolean {
        val savedPin = settings.value.vaultPin
        val ok = savedPin.isBlank() || savedPin == pinAttempt
        _isVaultUnlocked.value = ok
        return ok
    }

    fun lockVault() {
        recorder.stopPlayback()
        _isVaultUnlocked.value = false
    }

    fun setVaultPin(pin: String) {
        viewModelScope.launch {
            prefs.setVaultPin(pin.filter { it.isDigit() }.take(4))
            _statusMessage.value = if (pin.isBlank()) "Vault PIN disabled." else "4-digit Vault PIN enabled."
        }
    }

    fun toggleRecordingPlayback(entry: CallRecordingEntity) {
        recorder.togglePlayback(entry.id, entry.filePath) { err ->
            _statusMessage.value = err
        }
    }

    fun deleteRecording(entry: CallRecordingEntity) {
        viewModelScope.launch {
            if (playingRecordingId.value == entry.id) {
                recorder.stopPlayback()
            }
            if (entry.filePath.isNotBlank()) {
                recorder.shredFile(entry.filePath)
            }
            db.recordingDao().deleteById(entry.id)
            _statusMessage.value = "Recording permanently shredded."
        }
    }

    // Settings & Customization Studio
    fun setThemePreset(preset: DialerThemePreset) {
        playUiClick()
        viewModelScope.launch { prefs.setThemePreset(preset) }
    }

    fun setAutoDayNightThemeEnabled(enabled: Boolean) {
        playUiClick()
        viewModelScope.launch { prefs.setAutoDayNightThemeEnabled(enabled) }
    }

    fun setAmoledBlack(enabled: Boolean) {
        playUiClick()
        viewModelScope.launch { prefs.setAmoledBlack(enabled) }
    }

    fun setCustomThemeColors(bgHex: Long, surfaceHex: Long, accentHex: Long, textHex: Long) {
        viewModelScope.launch { prefs.setCustomThemeColors(bgHex, surfaceHex, accentHex, textHex) }
    }

    fun setFontPreset(preset: DialerFontPreset) {
        playUiClick()
        viewModelScope.launch { prefs.setFontPreset(preset) }
    }

    fun setFontStyleMode(mode: FontStyleMode) {
        playUiClick()
        viewModelScope.launch { prefs.setFontStyleMode(mode) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { prefs.setFontScale(scale) }
    }

    fun setCoolJazzGlow(enabled: Boolean) {
        viewModelScope.launch { prefs.setCoolJazzGlow(enabled) }
    }

    fun importCustomFontUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val outFile = File(getApplication<Application>().filesDir, "custom_user_font.ttf")
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    outFile.absolutePath
                }.getOrNull()
            }
            if (savedPath != null) {
                prefs.setCustomFontPath(savedPath)
                _statusMessage.value = "Custom font installed & activated."
            } else {
                _statusMessage.value = "Unable to import selected font file."
            }
        }
    }

    fun setDotDensity(density: Float) {
        viewModelScope.launch { prefs.setDotDensity(density) }
    }

    fun setUiSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setUiSoundsEnabled(enabled) }
    }

    fun setUiVibrationEnabled(enabled: Boolean) {
        if (enabled) {
            vibEngine.pulseKeypadTick()
        }
        viewModelScope.launch { prefs.setUiVibrationEnabled(enabled) }
    }

    fun setDialpadSoundStyle(style: DialpadSoundStyle) {
        if (style != DialpadSoundStyle.OFF) {
            uiSounds.playDialTone("5", style)
        }
        viewModelScope.launch { prefs.setDialpadSoundStyle(style) }
    }

    fun setPreferredFps(fps: Int) {
        playUiClick()
        viewModelScope.launch { prefs.setPreferredFps(fps) }
    }

    fun setAdaptiveRefreshRate(enabled: Boolean) {
        playUiClick()
        viewModelScope.launch { prefs.setAdaptiveRefreshRate(enabled) }
    }

    fun setUltraSmoothMode(enabled: Boolean) {
        playUiClick()
        viewModelScope.launch { prefs.setUltraSmoothMode(enabled) }
    }

    fun setShowContactQuickActions(enabled: Boolean) {
        playUiClick()
        viewModelScope.launch { prefs.setShowContactQuickActions(enabled) }
    }

    fun setUiBackgroundMode(mode: UiBackgroundMode) {
        playUiClick()
        viewModelScope.launch { prefs.setUiBackgroundMode(mode) }
    }

    fun setDotMatrixEnabled(enabled: Boolean) {
        playUiClick()
        viewModelScope.launch { prefs.setDotMatrixEnabled(enabled) }
    }

    fun setDotMatrixBgHex(hex: Long) {
        playUiClick()
        viewModelScope.launch { prefs.setDotMatrixBgHex(hex) }
    }

    fun setColorPlateStyle(style: ColorPlateStyle) {
        playUiClick()
        viewModelScope.launch { prefs.setColorPlateStyle(style) }
    }

    fun setSolidBgHex(hex: Long) {
        playUiClick()
        viewModelScope.launch { prefs.setSolidBgHex(hex) }
    }

    fun setGradientColors(startHex: Long, endHex: Long) {
        playUiClick()
        viewModelScope.launch { prefs.setGradientColors(startHex, endHex) }
    }

    fun setWallpaperUri(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                withContext(Dispatchers.IO) {
                    File(getApplication<Application>().filesDir, "ui_wallpaper.jpg").delete()
                }
                prefs.setWallpaperPath(null)
                _statusMessage.value = "Wallpaper cleared."
                return@launch
            }
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val app = getApplication<Application>()
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, boundsOpts)
                    }
                    val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                    var sample = 1
                    while (maxDim / sample > 1280) {
                        sample *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val decoded = app.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, decodeOpts)
                    } ?: return@runCatching null

                    val outFile = File(app.filesDir, "ui_wallpaper.jpg")
                    outFile.outputStream().use { out ->
                        decoded.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    }
                    decoded.recycle()
                    outFile.absolutePath
                }.getOrNull()
            }
            if (savedPath != null) {
                prefs.setWallpaperPath(savedPath)
                _statusMessage.value = "UI Wallpaper applied."
            } else {
                _statusMessage.value = "Could not load selected wallpaper."
            }
        }
    }

    fun setWallpaperDimStrength(dim: Float) {
        viewModelScope.launch { prefs.setWallpaperDimStrength(dim) }
    }

    fun setAudioOutputMode(mode: AudioOutputMode) {
        playUiClick()
        viewModelScope.launch { prefs.setAudioOutputMode(mode) }
    }

    fun setAutoRecordMode(mode: AutoRecordMode) {
        playUiClick()
        viewModelScope.launch { prefs.setAutoRecordMode(mode) }
    }

    fun setSilentCallRecording(enabled: Boolean) {
        viewModelScope.launch { prefs.setSilentCallRecording(enabled) }
    }

    fun setAutoSummarizeEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoSummarizeEnabled(enabled) }
    }

    fun setDefaultBackdropMode(mode: CallerBackdropMode) {
        playUiClick()
        viewModelScope.launch { prefs.setDefaultBackdropMode(mode) }
    }

    fun setDefaultVideoBackdrop(uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            prefs.setDefaultVideoBackdropUri(uri?.toString())
            _statusMessage.value = if (uri != null) {
                "Video backdrop enabled as active caller mode."
            } else {
                "Video backdrop cleared."
            }
        }
    }

    fun setDefaultPhotoBackdrop(uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            prefs.setDefaultPhotoBackdropUri(uri?.toString())
            _statusMessage.value = if (uri != null) {
                "Gallery photo enabled as active caller mode."
            } else {
                "Gallery photo backdrop cleared."
            }
        }
    }

    fun setDefaultCallingCardBackdrop(uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            prefs.setDefaultCallingCardUri(uri?.toString())
            _statusMessage.value = if (uri != null) {
                "Calling Card enabled as active caller mode."
            } else {
                "Calling Card cleared."
            }
        }
    }

    fun setCustomRingtoneUri(uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            prefs.setCustomRingtoneUri(uri?.toString())
            _statusMessage.value = if (uri != null) "Custom ringtone saved." else "Ringtone reset to system default."
        }
    }

    fun setVideoRedTintStrength(strength: Float) {
        viewModelScope.launch { prefs.setVideoRedTintStrength(strength) }
    }

    fun setVoiceCommandControl(enabled: Boolean) {
        viewModelScope.launch { prefs.setVoiceCommandControl(enabled) }
    }

    fun setAiAssistantEnabled(enabled: Boolean) {
        playUiClick()
        viewModelScope.launch { prefs.setAiAssistantEnabled(enabled) }
    }

    fun setAiAssistantTriggerMode(mode: AiAssistantTriggerMode) {
        playUiClick()
        viewModelScope.launch { prefs.setAiAssistantTriggerMode(mode) }
    }

    fun setAiAssistantDelaySeconds(seconds: Int) {
        playUiClick()
        viewModelScope.launch { prefs.setAiAssistantDelaySeconds(seconds) }
    }

    fun setAiBusyModeEnabled(enabled: Boolean) {
        playUiClick()
        viewModelScope.launch { prefs.setAiBusyModeEnabled(enabled) }
    }

    fun setAiAssistantLanguageMode(mode: AiAssistantLanguageMode) {
        playUiClick()
        viewModelScope.launch { prefs.setAiAssistantLanguageMode(mode) }
    }

    fun setAiAssistantOwnerName(name: String) {
        viewModelScope.launch { prefs.setAiAssistantOwnerName(name) }
    }

    fun setAiAssistantAutoRecord(enabled: Boolean) {
        playUiClick()
        viewModelScope.launch { prefs.setAiAssistantAutoRecord(enabled) }
    }

    fun selectActiveVibrationPattern(id: Long) {
        viewModelScope.launch { prefs.setActiveVibrationPattern(id) }
    }

    fun testVibrationPattern(pattern: VibrationPatternEntity) {
        vibEngine.playGlyphPattern(pattern, repeat = false)
    }

    fun saveCustomVibrationPattern(name: String, timingsMs: List<Long>) {
        if (name.isBlank() || timingsMs.isEmpty()) {
            _statusMessage.value = "Tap at least one rhythm beat and provide a pattern name."
            return
        }
        viewModelScope.launch {
            val timingsCsv = timingsMs.joinToString(",")
            val amplitudesCsv = timingsMs.mapIndexed { idx, _ ->
                if (idx % 2 == 0) "0" else "240"
            }.joinToString(",")
            val id = db.vibrationPatternDao().insert(
                VibrationPatternEntity(
                    name = name.trim().uppercase(Locale.US),
                    description = "Custom tapped glyph pattern",
                    timingsCsv = timingsCsv,
                    amplitudesCsv = amplitudesCsv,
                    isBuiltIn = false
                )
            )
            prefs.setActiveVibrationPattern(id)
            _statusMessage.value = "Custom vibration glyph \"$name\" saved."
        }
    }

    fun deleteCustomVibrationPattern(id: Long) {
        viewModelScope.launch {
            db.vibrationPatternDao().deleteCustom(id)
        }
    }

    // Spam Rules & Gemini Real-Time Risk Check
    fun addSpamRule(pattern: String, label: String, riskLevel: Int = 85) {
        if (pattern.isBlank()) return
        viewModelScope.launch {
            db.spamRuleDao().insert(
                SpamRuleEntity(
                    pattern = pattern.trim(),
                    label = label.ifBlank { "Custom Spam Prefix" },
                    riskLevel = riskLevel.coerceIn(10, 100)
                )
            )
            _statusMessage.value = "Spam rule added."
        }
    }

    fun toggleSpamRule(rule: SpamRuleEntity) {
        viewModelScope.launch {
            db.spamRuleDao().update(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteSpamRule(id: Long) {
        viewModelScope.launch {
            db.spamRuleDao().deleteById(id)
        }
    }

    fun checkNumberWithGemini(phoneNumber: String) {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _spamCheckState.value = SpamCheckUiState.Checking
            val burstCount = callLogs.value.count {
                it.normalizedNumber.take(6) == T9Utils.normalizePhone(clean).take(6)
            }
            val result = gemini.assessCallerSpamRisk(
                phoneNumber = clean,
                callerLabel = "",
                recentBurstCount = burstCount
            )
            result.onSuccess { assessment ->
                _spamCheckState.value = SpamCheckUiState.Result(clean, assessment)
            }.onFailure { err ->
                _spamCheckState.value = SpamCheckUiState.Error(
                    err.localizedMessage ?: "Real-time Gemini spam check failed."
                )
            }
        }
    }

    private fun playVibrationById(patternId: Long, repeat: Boolean) {
        viewModelScope.launch {
            val pattern = db.vibrationPatternDao().getById(patternId)
                ?: vibrationPatterns.value.firstOrNull()
            if (pattern != null) {
                vibEngine.playGlyphPattern(pattern, repeat = repeat)
            }
        }
    }

    private fun startElapsedTimer() {
        if (callTimerJob != null) return
        callTimerJob = viewModelScope.launch {
            var sec = callSession.value.elapsedSeconds
            while (true) {
                delay(1000L)
                sec++
                CallSessionBridge.updateElapsed(sec)
            }
        }
    }

    private fun stopElapsedTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
    }

    private fun persistUriPermission(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }.onFailure { Log.w(TAG, "Persistable URI grant optional/unavailable: ${it.message}") }
    }

    private fun computeAnalytics(
        logs: List<CallLogEntity>,
        period: AnalyticsPeriod
    ): CallAnalyticsSummary {
        val now = System.currentTimeMillis()
        val cutoff = period.days?.let { now - it * 86_400_000L } ?: 0L
        val scoped = logs.filter { it.timestamp >= cutoff }

        val totalDur = scoped.sumOf { it.durationSeconds }
        val connectedCalls = scoped.count { it.durationSeconds > 0 }
        val avgDur = if (connectedCalls > 0) totalDur / connectedCalls else 0

        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val cal = Calendar.getInstance()
        val dailyList = (6 downTo 0).map { offset ->
            cal.timeInMillis = now - offset * 86_400_000L
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            val end = start + 86_400_000L
            val dayLogs = logs.filter { it.timestamp in start until end }
            DailyCallMetric(
                dayLabel = dayFmt.format(start).uppercase(Locale.US).take(2),
                totalMinutes = dayLogs.sumOf { it.durationSeconds } / 60f,
                callCount = dayLogs.size
            )
        }

        val topContacts = scoped
            .groupBy { it.normalizedNumber.ifBlank { it.displayName } }
            .map { (_, entries) ->
                val head = entries.first()
                ContactDurationMetric(
                    name = head.displayName,
                    phoneNumber = head.phoneNumber,
                    totalSeconds = entries.sumOf { it.durationSeconds },
                    callCount = entries.size
                )
            }
            .sortedByDescending { it.totalSeconds }
            .take(5)

        return CallAnalyticsSummary(
            totalCalls = scoped.size,
            totalDurationSeconds = totalDur,
            averageDurationSeconds = avgDur,
            incomingCount = scoped.count { it.direction == CallDirection.INCOMING },
            outgoingCount = scoped.count { it.direction == CallDirection.OUTGOING },
            missedCount = scoped.count { it.direction == CallDirection.MISSED },
            spamBlockedCount = scoped.count { it.isSpam || it.direction == CallDirection.BLOCKED_SPAM },
            dailySeries = dailyList,
            topContacts = topContacts
        )
    }

    override fun onCleared() {
        ringtonePlayer.stop()
        vibEngine.stop()
        voice.stopListening()
        aiAssistantEngine.release()
        voiceChangerEngine.release()
        recorder.stopPlayback()
        uiSounds.release()
        audioRoutes.release()
        super.onCleared()
    }
}
