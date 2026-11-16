package com.example.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.core.content.ContextCompat
import com.example.data.local.CallDirection
import com.example.data.local.ContactEntity
import com.example.data.preferences.CallerBackdropMode
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.VoiceChangerPreset
import com.example.data.system.ActiveAudioRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CallLifecycleState {
    IDLE,
    INCOMING_RINGING,
    DIALING,
    ACTIVE
}

enum class CallDisplayMode {
    COMPACT_BANNER,
    STATUS_BAR_PILL,
    RINGING_MUTED_STATUS,
    FULL_SCREEN
}

enum class AiCallPhase {
    IDLE,
    RING_COUNTDOWN,
    GREETING,
    SPEAKING,
    LISTENING,
    THINKING,
    HANDED_OVER
}

data class AiTranscriptTurn(
    val speaker: String,
    val text: String,
    val languageCode: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class ResolvedCallerBackdrop(
    val mode: CallerBackdropMode = CallerBackdropMode.NONE,
    val videoUri: String? = null,
    val photoUri: String? = null,
    val callingCardUri: String? = null
)

data class CallSessionState(
    val lifecycle: CallLifecycleState = CallLifecycleState.IDLE,
    val displayMode: CallDisplayMode = CallDisplayMode.FULL_SCREEN,
    val displayName: String = "",
    val phoneNumber: String = "",
    val contactId: Long? = null,
    val contactPhotoUri: String? = null,
    val activeBackdropMode: CallerBackdropMode = CallerBackdropMode.NONE,
    val photoBackdropUri: String? = null,
    val callingCardUri: String? = null,
    val videoBackdropUri: String? = null,
    val vibrationPatternId: Long = 1L,
    val isSpamWarning: Boolean = false,
    val spamReason: String = "",
    val spamScore: Int = 0,
    val isMuted: Boolean = false,
    val isRingMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val activeAudioRoute: ActiveAudioRoute = ActiveAudioRoute.EAR_RECEIVER,
    val isBluetoothConnected: Boolean = false,
    val isRecording: Boolean = false,
    val recordingFilePath: String? = null,
    val elapsedSeconds: Int = 0,
    val callStartedAtMs: Long = 0L,
    val isSystemTelecomCall: Boolean = false,
    val lastDirection: CallDirection = CallDirection.OUTGOING,
    val miniNotepadText: String = "",
    val dtmfDigits: String = "",
    val isAiAssistantActive: Boolean = false,
    val aiPhase: AiCallPhase = AiCallPhase.IDLE,
    val aiActiveLanguageCode: String = "EN",
    val aiOwnerName: String = "",
    val aiCountdownSecondsLeft: Int = 0,
    val aiCallerPurposeTag: String = "",
    val aiTranscript: List<AiTranscriptTurn> = emptyList(),
    val voiceChangerPreset: VoiceChangerPreset = VoiceChangerPreset.NORMAL,
    val isVoiceChangerDspRunning: Boolean = false
)

object CallSessionBridge {
    private val _session = MutableStateFlow(CallSessionState())
    val session: StateFlow<CallSessionState> = _session.asStateFlow()

    private var activeSystemCall: Call? = null
    private var inCallServiceRef: DotInCallService? = null

    fun attachService(service: DotInCallService?) {
        inCallServiceRef = service
    }

    fun onSystemCallAdded(call: Call) {
        activeSystemCall = call
        val handle = call.details?.handle?.schemeSpecificPart.orEmpty()
        val callerName = call.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: handle
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details?.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            call.state == Call.STATE_RINGING
        }

        val mappedState = when (call.state) {
            Call.STATE_RINGING -> CallLifecycleState.INCOMING_RINGING
            Call.STATE_ACTIVE -> CallLifecycleState.ACTIVE
            Call.STATE_DIALING, Call.STATE_CONNECTING -> CallLifecycleState.DIALING
            else -> if (isIncoming) CallLifecycleState.INCOMING_RINGING else CallLifecycleState.DIALING
        }

        _session.value = CallSessionState(
            lifecycle = mappedState,
            displayMode = if (isIncoming) CallDisplayMode.COMPACT_BANNER else CallDisplayMode.FULL_SCREEN,
            displayName = callerName,
            phoneNumber = handle,
            callStartedAtMs = if (mappedState == CallLifecycleState.ACTIVE) System.currentTimeMillis() else 0L,
            isSystemTelecomCall = true,
            lastDirection = if (isIncoming) CallDirection.INCOMING else CallDirection.OUTGOING
        )
    }

    fun onSystemCallStateChanged(call: Call, state: Int) {
        if (activeSystemCall != call) return
        when (state) {
            Call.STATE_RINGING -> _session.update { it.copy(lifecycle = CallLifecycleState.INCOMING_RINGING) }
            Call.STATE_DIALING, Call.STATE_CONNECTING -> _session.update { it.copy(lifecycle = CallLifecycleState.DIALING) }
            Call.STATE_ACTIVE -> _session.update {
                it.copy(
                    lifecycle = CallLifecycleState.ACTIVE,
                    callStartedAtMs = if (it.callStartedAtMs == 0L) System.currentTimeMillis() else it.callStartedAtMs
                )
            }
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                _session.value = CallSessionState()
            }
        }
    }

    fun onSystemCallRemoved(call: Call) {
        if (activeSystemCall == call) {
            activeSystemCall = null
            _session.value = CallSessionState()
        }
    }

    fun resolveBackdropSpec(contact: ContactEntity?, settings: DialerSettings): ResolvedCallerBackdrop {
        if (contact != null) {
            val contactExplicitMode = if (contact.callerBackdropMode.isNotBlank()) {
                CallerBackdropMode.fromId(contact.callerBackdropMode)
            } else {
                when {
                    !contact.videoBackdropUri.isNullOrBlank() -> CallerBackdropMode.VIDEO
                    !contact.backdropPhotoUri.isNullOrBlank() -> CallerBackdropMode.PHOTO
                    !contact.callingCardUri.isNullOrBlank() -> CallerBackdropMode.CALLING_CARD
                    else -> CallerBackdropMode.NONE
                }
            }
            when (contactExplicitMode) {
                CallerBackdropMode.VIDEO -> if (!contact.videoBackdropUri.isNullOrBlank()) {
                    return ResolvedCallerBackdrop(
                        mode = CallerBackdropMode.VIDEO,
                        videoUri = contact.videoBackdropUri
                    )
                }
                CallerBackdropMode.PHOTO -> {
                    val uri = contact.backdropPhotoUri ?: contact.photoUri
                    if (!uri.isNullOrBlank()) {
                        return ResolvedCallerBackdrop(
                            mode = CallerBackdropMode.PHOTO,
                            photoUri = uri
                        )
                    }
                }
                CallerBackdropMode.CALLING_CARD -> if (!contact.callingCardUri.isNullOrBlank()) {
                    return ResolvedCallerBackdrop(
                        mode = CallerBackdropMode.CALLING_CARD,
                        callingCardUri = contact.callingCardUri
                    )
                }
                CallerBackdropMode.NONE -> Unit
            }
        }

        return when (settings.defaultBackdropMode) {
            CallerBackdropMode.VIDEO -> if (!settings.defaultVideoBackdropUri.isNullOrBlank()) {
                ResolvedCallerBackdrop(
                    mode = CallerBackdropMode.VIDEO,
                    videoUri = settings.defaultVideoBackdropUri
                )
            } else ResolvedCallerBackdrop()
            CallerBackdropMode.PHOTO -> if (!settings.defaultPhotoBackdropUri.isNullOrBlank()) {
                ResolvedCallerBackdrop(
                    mode = CallerBackdropMode.PHOTO,
                    photoUri = settings.defaultPhotoBackdropUri
                )
            } else ResolvedCallerBackdrop()
            CallerBackdropMode.CALLING_CARD -> if (!settings.defaultCallingCardUri.isNullOrBlank()) {
                ResolvedCallerBackdrop(
                    mode = CallerBackdropMode.CALLING_CARD,
                    callingCardUri = settings.defaultCallingCardUri
                )
            } else ResolvedCallerBackdrop()
            CallerBackdropMode.NONE -> ResolvedCallerBackdrop()
        }
    }

    fun startInAppCallSession(
        displayName: String,
        phoneNumber: String,
        contact: ContactEntity?,
        isIncoming: Boolean,
        compactBannerWhenIncoming: Boolean,
        isSpam: Boolean,
        spamScore: Int,
        spamReason: String,
        backdrop: ResolvedCallerBackdrop,
        defaultVibrationId: Long,
        initialAudioRoute: ActiveAudioRoute,
        bluetoothConnected: Boolean
    ) {
        val now = System.currentTimeMillis()
        _session.value = CallSessionState(
            lifecycle = if (isIncoming) CallLifecycleState.INCOMING_RINGING else CallLifecycleState.ACTIVE,
            displayMode = if (isIncoming && compactBannerWhenIncoming) {
                CallDisplayMode.COMPACT_BANNER
            } else {
                CallDisplayMode.FULL_SCREEN
            },
            displayName = displayName.ifBlank { phoneNumber },
            phoneNumber = phoneNumber,
            contactId = contact?.id,
            contactPhotoUri = contact?.photoUri,
            activeBackdropMode = backdrop.mode,
            photoBackdropUri = backdrop.photoUri,
            callingCardUri = backdrop.callingCardUri,
            videoBackdropUri = backdrop.videoUri,
            vibrationPatternId = contact?.vibrationPatternId ?: defaultVibrationId,
            isSpamWarning = isSpam,
            spamReason = spamReason,
            spamScore = spamScore,
            isSpeakerOn = initialAudioRoute == ActiveAudioRoute.SPEAKER,
            activeAudioRoute = initialAudioRoute,
            isBluetoothConnected = bluetoothConnected,
            callStartedAtMs = if (isIncoming) 0L else now,
            isSystemTelecomCall = false,
            lastDirection = if (isIncoming) CallDirection.INCOMING else CallDirection.OUTGOING
        )
    }

    fun answerCall(acceptToStatusBar: Boolean = false): Boolean {
        val current = _session.value
        if (current.lifecycle != CallLifecycleState.INCOMING_RINGING) return false
        activeSystemCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        _session.update {
            it.copy(
                lifecycle = CallLifecycleState.ACTIVE,
                displayMode = if (acceptToStatusBar) CallDisplayMode.STATUS_BAR_PILL else CallDisplayMode.FULL_SCREEN,
                isRingMuted = false,
                callStartedAtMs = System.currentTimeMillis()
            )
        }
        return true
    }

    fun swipeUpMuteIncoming(): Boolean {
        val current = _session.value
        if (current.lifecycle != CallLifecycleState.INCOMING_RINGING) return false
        _session.update {
            it.copy(
                isRingMuted = true,
                displayMode = CallDisplayMode.RINGING_MUTED_STATUS
            )
        }
        return true
    }

    fun setRingMutedInPlace(muted: Boolean) {
        _session.update { it.copy(isRingMuted = muted) }
    }

    fun updateVoiceChangerState(preset: VoiceChangerPreset, dspRunning: Boolean) {
        _session.update {
            it.copy(
                voiceChangerPreset = preset,
                isVoiceChangerDspRunning = dspRunning
            )
        }
    }

    fun setDisplayMode(mode: CallDisplayMode) {
        _session.update { it.copy(displayMode = mode) }
    }

    fun endCall(): CallSessionState? {
        val current = _session.value
        if (current.lifecycle == CallLifecycleState.IDLE) return null

        val call = activeSystemCall
        if (call != null) {
            if (current.lifecycle == CallLifecycleState.INCOMING_RINGING) {
                call.reject(false, null)
            } else {
                call.disconnect()
            }
        }

        val finalDirection = if (current.lifecycle == CallLifecycleState.INCOMING_RINGING) {
            if (current.isSpamWarning) CallDirection.BLOCKED_SPAM else CallDirection.MISSED
        } else {
            current.lastDirection
        }

        val snapshot = current.copy(lastDirection = finalDirection)
        _session.value = CallSessionState()
        return snapshot
    }

    fun updateElapsed(seconds: Int) {
        _session.update { it.copy(elapsedSeconds = seconds) }
    }

    fun setMuted(context: Context, muted: Boolean) {
        inCallServiceRef?.setMuted(muted)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audio?.isMicrophoneMute = muted
        _session.update { it.copy(isMuted = muted, isRingMuted = if (muted) true else it.isRingMuted) }
    }

    fun setSpeaker(context: Context, speaker: Boolean) {
        val route = if (speaker) ActiveAudioRoute.SPEAKER else ActiveAudioRoute.EAR_RECEIVER
        setAudioRoute(context, route)
    }

    fun setAudioRoute(context: Context, route: ActiveAudioRoute) {
        val telecomRoute = when (route) {
            ActiveAudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
            ActiveAudioRoute.EAR_RECEIVER -> CallAudioState.ROUTE_EARPIECE
            ActiveAudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
        }
        inCallServiceRef?.setAudioRoute(telecomRoute)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        @Suppress("DEPRECATION")
        audio?.isSpeakerphoneOn = (route == ActiveAudioRoute.SPEAKER)
        _session.update {
            it.copy(
                activeAudioRoute = route,
                isSpeakerOn = route == ActiveAudioRoute.SPEAKER
            )
        }
    }

    fun updateBluetoothConnected(connected: Boolean) {
        _session.update { it.copy(isBluetoothConnected = connected) }
    }

    fun updateRecordingState(isRecording: Boolean, filePath: String? = _session.value.recordingFilePath) {
        _session.update { it.copy(isRecording = isRecording, recordingFilePath = filePath) }
    }

    fun updateMiniNotepad(notes: String) {
        _session.update { it.copy(miniNotepadText = notes) }
    }

    fun sendInCallDtmf(digit: String) {
        val ch = digit.firstOrNull() ?: return
        runCatching {
            activeSystemCall?.stopDtmfTone()
            activeSystemCall?.playDtmfTone(ch)
        }
        _session.update { state ->
            state.copy(dtmfDigits = (state.dtmfDigits + ch).takeLast(32))
        }
    }

    fun stopSystemDtmfTone() {
        runCatching {
            activeSystemCall?.stopDtmfTone()
        }
    }

    fun clearInCallDtmfDigits() {
        _session.update { it.copy(dtmfDigits = "") }
    }

    fun updateAiCountdown(secondsLeft: Int) {
        _session.update {
            it.copy(
                aiPhase = if (secondsLeft > 0) AiCallPhase.RING_COUNTDOWN else it.aiPhase,
                aiCountdownSecondsLeft = secondsLeft.coerceAtLeast(0)
            )
        }
    }

    fun updateAiAssistantState(
        active: Boolean,
        phase: AiCallPhase,
        languageCode: String = _session.value.aiActiveLanguageCode,
        ownerName: String = _session.value.aiOwnerName,
        purposeTag: String = _session.value.aiCallerPurposeTag
    ) {
        _session.update {
            it.copy(
                isAiAssistantActive = active,
                aiPhase = phase,
                aiActiveLanguageCode = languageCode,
                aiOwnerName = ownerName,
                aiCountdownSecondsLeft = 0,
                aiCallerPurposeTag = purposeTag
            )
        }
    }

    fun appendAiTranscriptTurn(speaker: String, text: String, languageCode: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        _session.update {
            it.copy(
                aiTranscript = it.aiTranscript + AiTranscriptTurn(
                    speaker = speaker,
                    text = clean,
                    languageCode = languageCode
                )
            )
        }
    }

    fun clearAiAssistantState() {
        _session.update {
            it.copy(
                isAiAssistantActive = false,
                aiPhase = AiCallPhase.IDLE,
                aiCountdownSecondsLeft = 0
            )
        }
    }

    fun enrichWithContact(
        contact: ContactEntity?,
        isSpam: Boolean,
        spamScore: Int,
        spamReason: String,
        backdrop: ResolvedCallerBackdrop,
        defaultVibrationId: Long
    ) {
        _session.update { current ->
            val resolvedName = contact?.name?.takeIf { it.isNotBlank() } ?: current.displayName
            current.copy(
                displayName = resolvedName,
                contactId = contact?.id ?: current.contactId,
                contactPhotoUri = contact?.photoUri ?: current.contactPhotoUri,
                activeBackdropMode = backdrop.mode,
                photoBackdropUri = backdrop.photoUri,
                callingCardUri = backdrop.callingCardUri,
                videoBackdropUri = backdrop.videoUri,
                vibrationPatternId = contact?.vibrationPatternId ?: defaultVibrationId,
                isSpamWarning = isSpam || current.isSpamWarning,
                spamScore = maxOf(spamScore, current.spamScore),
                spamReason = spamReason.ifBlank { current.spamReason }
            )
        }
    }

    fun isDefaultDialer(context: Context): Boolean {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        return telecom.defaultDialerPackage == context.packageName
    }

    fun placeSystemCallIfSupported(context: Context, phoneNumber: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val hasTelephony = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        if (!hasTelephony) return false

        return runCatching {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val uri = Uri.fromParts("tel", phoneNumber, null)
            if (telecom != null && isDefaultDialer(context)) {
                telecom.placeCall(uri, null)
            } else {
                val intent = Intent(Intent.ACTION_CALL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            true
        }.getOrDefault(false)
    }

    fun placeVideoCallIfSupported(context: Context, phoneNumber: String): Boolean {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) return false
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val hasTelephony = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        if (!hasTelephony || !hasCallPermission) return false

        return runCatching {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val uri = Uri.fromParts("tel", clean, null)
            val extras = Bundle().apply {
                putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL)
            }
            if (telecom != null && isDefaultDialer(context)) {
                telecom.placeCall(uri, extras)
            } else {
                val intent = Intent(Intent.ACTION_CALL, uri).apply {
                    putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            true
        }.getOrDefault(false)
    }
}
