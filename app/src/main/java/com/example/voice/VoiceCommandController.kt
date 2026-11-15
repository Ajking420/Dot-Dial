package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class VoiceUiState(
    val isAvailable: Boolean = false,
    val isListening: Boolean = false,
    val isContinuousCallMode: Boolean = false,
    val partialTranscript: String = "",
    val lastFinalTranscript: String = "",
    val statusMessage: String = ""
)

enum class FastCallVoiceAction {
    RECEIVE_CALL,
    END_CALL,
    MUTE_CALL,
    TOGGLE_SPEAKER_ON,
    TOGGLE_SPEAKER_OFF,
    START_RECORDING,
    NONE
}

class VoiceCommandController(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var onFinalUtteranceCallback: ((String) -> Unit)? = null
    private var continuousModeActive = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(
        VoiceUiState(isAvailable = SpeechRecognizer.isRecognitionAvailable(context))
    )
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun startHandsFreeCallLoop(onUtterance: (String) -> Unit) {
        continuousModeActive = true
        _state.update { it.copy(isContinuousCallMode = true) }
        startListeningInternal(onUtterance)
    }

    fun startListening(onFinalUtterance: (String) -> Unit) {
        continuousModeActive = false
        _state.update { it.copy(isContinuousCallMode = false) }
        startListeningInternal(onFinalUtterance)
    }

    private fun startListeningInternal(onFinalUtterance: (String) -> Unit) {
        if (!hasMicPermission()) {
            continuousModeActive = false
            _state.update {
                it.copy(
                    isListening = false,
                    isContinuousCallMode = false,
                    statusMessage = "Mic permission required for hands-free answer/end."
                )
            }
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            continuousModeActive = false
            _state.update {
                it.copy(
                    isAvailable = false,
                    isListening = false,
                    isContinuousCallMode = false,
                    statusMessage = "Speech recognition unavailable on this device."
                )
            }
            return
        }

        onFinalUtteranceCallback = onFinalUtterance
        destroyRecognizerOnly()

        mainHandler.post {
            runCatching {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.update {
                            it.copy(
                                isListening = true,
                                partialTranscript = "",
                                statusMessage = if (continuousModeActive) "Hands-free voice active..." else "Listening..."
                            )
                        }
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _state.update { it.copy(isListening = false) }
                    }

                    override fun onError(error: Int) {
                        _state.update { it.copy(isListening = false) }
                        if (continuousModeActive &&
                            (error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        ) {
                            mainHandler.postDelayed({
                                if (continuousModeActive) {
                                    onFinalUtteranceCallback?.let { cb -> startListeningInternal(cb) }
                                }
                            }, 350L)
                        } else {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Voice listening timed out."
                                SpeechRecognizer.ERROR_AUDIO -> "Audio input error."
                                else -> "Voice interrupted (code $error)."
                            }
                            _state.update { it.copy(statusMessage = msg) }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val spoken = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        _state.update {
                            it.copy(
                                isListening = false,
                                lastFinalTranscript = spoken,
                                partialTranscript = spoken
                            )
                        }
                        if (spoken.isNotEmpty()) {
                            onFinalUtteranceCallback?.invoke(spoken)
                        }
                        if (continuousModeActive) {
                            mainHandler.postDelayed({
                                if (continuousModeActive) {
                                    onFinalUtteranceCallback?.let { cb -> startListeningInternal(cb) }
                                }
                            }, 450L)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (partial.isNotEmpty()) {
                            _state.update { it.copy(partialTranscript = partial) }
                            if (continuousModeActive && detectFastCallAction(partial) != FastCallVoiceAction.NONE) {
                                onFinalUtteranceCallback?.invoke(partial)
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer = recognizer
                recognizer.startListening(intent)
            }
        }
    }

    private fun destroyRecognizerOnly() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    fun stopListening() {
        continuousModeActive = false
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizerOnly()
        _state.update { it.copy(isListening = false, isContinuousCallMode = false) }
    }

    companion object {
        fun detectFastCallAction(utterance: String): FastCallVoiceAction {
            val normalized = utterance.lowercase(Locale.US).trim()
            return when {
                normalized.contains("answer") ||
                    normalized.contains("accept") ||
                    normalized.contains("receive") ||
                    normalized.contains("pick up") -> FastCallVoiceAction.RECEIVE_CALL

                normalized.contains("end") ||
                    normalized.contains("hang up") ||
                    normalized.contains("disconnect") ||
                    normalized.contains("decline") ||
                    normalized.contains("reject") -> FastCallVoiceAction.END_CALL

                normalized.contains("mute") ||
                    normalized.contains("silent") ||
                    normalized.contains("silence") -> FastCallVoiceAction.MUTE_CALL

                normalized.contains("speaker on") -> FastCallVoiceAction.TOGGLE_SPEAKER_ON
                normalized.contains("speaker off") -> FastCallVoiceAction.TOGGLE_SPEAKER_OFF
                normalized.contains("record") -> FastCallVoiceAction.START_RECORDING
                else -> FastCallVoiceAction.NONE
            }
        }
    }
}
