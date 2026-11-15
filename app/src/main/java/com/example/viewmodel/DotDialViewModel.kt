package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DotDialContainer
import com.example.data.ai.AiCommandIntent
import com.example.data.local.CallDirection
import com.example.data.local.CallLogEntity
import com.example.data.local.CallRecordingEntity
import com.example.data.local.ContactEntity
import com.example.data.local.SpamRuleEntity
import com.example.data.local.VibrationPatternEntity
import com.example.data.preferences.AudioOutputMode
import com.example.data.preferences.AutoRecordMode
import com.example.data.preferences.DialerSettings
import com.example.data.system.ActiveAudioRoute
import com.example.data.system.T9Utils
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
    private val contactsSync = container.contactsSync
    private val spamEngine = container.spamEngine
    private val uiSounds = container.uiSounds
    private val audioRoutes = container.audioRouteManager
    private val ringtonePlayer = container.ringtonePlayer
    private val backupManager = container.backupManager

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

    val settings: StateFlow<DialerSettings> = prefs.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DialerSettings()
    )

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
            HistoryFilter.MISSED -> logs.filter { it.direction == CallDirection.MISSED }
            HistoryFilter.SPAM -> logs.filter { it.isSpam || it.direction == CallDirection.BLOCKED_SPAM }
        }
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
                            val normalized = T9Utils.normalizePhone(session.phoneNumber)
                            val contact = db.contactDao().findByNormalizedNumber(normalized)
                            val verdict = spamEngine.evaluateNumber(session.phoneNumber, session.displayName)
                            val curSettings = settings.value
                            CallSessionBridge.enrichWithContact(
                                contact = contact,
                                isSpam = verdict.isSpam,
                                spamScore = verdict.riskScore,
                                spamReason = verdict.reason,
                                defaultVideoUri = curSettings.defaultVideoBackdropUri,
                                defaultVibrationId = curSettings.activeVibrationPatternId
                            )
                            if (!session.isRingMuted) {
                                ringtonePlayer.startRinging(curSettings.customRingtoneUri)
                                playVibrationById(
                                    contact?.vibrationPatternId ?: curSettings.activeVibrationPatternId,
                                    repeat = true
                                )
                            }
                        }
                        if (settings.value.voiceCommandControlEnabled && !voice.state.value.isContinuousCallMode) {
                            voice.startHandsFreeCallLoop { spoken ->
                                executeAiOrVoiceCommand(spoken)
                            }
                        }
                    }

                    CallLifecycleState.ACTIVE -> {
                        ringtonePlayer.stop()
                        vibEngine.stop()
                        ringTimeoutJob?.cancel()
                        ringTimeoutJob = null
                        if (callTimerJob == null) startElapsedTimer()
                        CallNotificationManager.showOrUpdateCallNotification(
                            context = getApplication(),
                            callerName = session.displayName,
                            phoneNumber = session.phoneNumber,
                            isRinging = false,
                            isSpeakerOn = session.isSpeakerOn,
                            callStartMs = session.callStartedAtMs
                        )
                        if (settings.value.voiceCommandControlEnabled && !voice.state.value.isContinuousCallMode) {
                            voice.startHandsFreeCallLoop { spoken ->
                                executeAiOrVoiceCommand(spoken)
                            }
                        }
                    }

                    CallLifecycleState.IDLE -> {
                        ringtonePlayer.stop()
                        vibEngine.stop()
                        ringTimeoutJob?.cancel()
                        ringTimeoutJob = null
                        stopElapsedTimer()
                        voice.stopListening()
                        CallNotificationManager.cancel(getApplication())
                    }

                    CallLifecycleState.DIALING -> Unit
                }
            }
        }
    }

    fun playUiClick() {
        uiSounds.playUiClick(settings.value.uiSoundsEnabled)
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
        vibEngine.pulseKeypadTick()
        uiSounds.playDialTone(digit, settings.value.uiSoundsEnabled)
        _dialerQuery.value += digit
    }

    fun backspaceDialDigit() {
        vibEngine.pulseKeypadTick()
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

    fun exportBackupJson(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportBackupJson(uri).onSuccess { res ->
                _statusMessage.value =
                    "Exported ${res.restoredContacts} contacts & ${res.restoredCallLogs} call history entries."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Backup export failed."
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
            backupManager.restoreFromBackupJson(uri).onSuccess { res ->
                DotDialContactWidgetProvider.refreshAll(getApplication())
                _statusMessage.value =
                    "Restored ${res.restoredContacts} contacts & ${res.restoredCallLogs} call history entries."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Backup restore failed."
            }
        }
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
            val contact = explicitContact ?: db.contactDao().findByNormalizedNumber(normalized)
            val verdict = spamEngine.evaluateNumber(cleaned, contact?.name.orEmpty())
            val curSettings = settings.value
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
            if (!placedOnSystem || !CallSessionBridge.isDefaultDialer(getApplication())) {
                CallSessionBridge.startInAppCallSession(
                    displayName = contact?.name ?: cleaned,
                    phoneNumber = cleaned,
                    contact = contact,
                    isIncoming = false,
                    compactBannerWhenIncoming = false,
                    isSpam = verdict.isSpam,
                    spamScore = verdict.riskScore,
                    spamReason = verdict.reason,
                    defaultVideoUri = curSettings.defaultVideoBackdropUri,
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

            CallSessionBridge.startInAppCallSession(
                displayName = displayName,
                phoneNumber = number,
                contact = chosenContact,
                isIncoming = true,
                compactBannerWhenIncoming = compactPopup,
                isSpam = verdict.isSpam || spamTest,
                spamScore = if (spamTest) maxOf(verdict.riskScore, 91) else verdict.riskScore,
                spamReason = if (spamTest && verdict.reason.isBlank()) "Automated Robocall Signature" else verdict.reason,
                defaultVideoUri = curSettings.defaultVideoBackdropUri,
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
        }
    }

    fun answerActiveCall(toStatusBar: Boolean = false) {
        ringtonePlayer.stop()
        vibEngine.stop()
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null

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

            val notes = endedSnapshot.miniNotepadText.trim()
            if (!savedFile.isNullOrBlank() || notes.isNotEmpty() || settings.value.autoSummarizeEnabled) {
                val recId = db.recordingDao().insert(
                    CallRecordingEntity(
                        callLogId = logId,
                        contactName = endedSnapshot.displayName,
                        phoneNumber = endedSnapshot.phoneNumber,
                        filePath = savedFile.orEmpty(),
                        durationSeconds = recordedDuration,
                        transcriptText = notes,
                        isLocked = settings.value.vaultPin.isNotBlank()
                    )
                )
                db.callLogDao().attachRecording(logId, recId)
                if (settings.value.autoSummarizeEnabled && endedSnapshot.elapsedSeconds > 0) {
                    summarizeRecording(recId, notes.ifBlank { null })
                }
            }
        }
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

    // Contact Management
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
        syncToSystem: Boolean = true
    ) {
        if (name.isBlank() || phoneNumber.isBlank()) {
            _statusMessage.value = "Name and phone number are required."
            return
        }
        viewModelScope.launch {
            val normalized = T9Utils.normalizePhone(phoneNumber)
            val existing = existingId?.let { db.contactDao().findById(it) }
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
                    accountName = existing?.accountName.orEmpty(),
                    accountType = existing?.accountType.orEmpty(),
                    isFavorite = isFavorite,
                    isBlocked = existing?.isBlocked ?: false,
                    spamScore = existing?.spamScore ?: 0,
                    videoBackdropUri = videoBackdropUri ?: existing?.videoBackdropUri,
                    vibrationPatternId = vibrationPatternId,
                    notes = notes.trim()
                )
            )
            if (syncToSystem && existing == null) {
                contactsSync.exportContactToSystemIfPermitted(
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    email = email.trim(),
                    googleAccount = settings.value.syncedGoogleAccounts.firstOrNull()
                )
            }
            DotDialContactWidgetProvider.refreshAll(getApplication())
            _statusMessage.value = "Contact saved."
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
            db.contactDao().update(contact.copy(callingCardUri = uri?.toString()))
            _statusMessage.value = if (uri != null) "Calling card updated for ${contact.name}." else "Calling card cleared."
        }
    }

    fun assignContactVideoBackdrop(contact: ContactEntity, uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            db.contactDao().update(contact.copy(videoBackdropUri = uri?.toString()))
            _statusMessage.value = if (uri != null) {
                "Video backdrop assigned to ${contact.name}."
            } else {
                "Video backdrop cleared."
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

    fun setDefaultVideoBackdrop(uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) persistUriPermission(uri)
            prefs.setDefaultVideoBackdropUri(uri?.toString())
            _statusMessage.value = if (uri != null) "Default video backdrop updated." else "Reset to Nothing Glyph."
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
        recorder.stopPlayback()
        uiSounds.release()
        audioRoutes.release()
        super.onCleared()
    }
}
