package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.ai.AssistantReplyLanguage
import com.example.data.preferences.AiAssistantLanguageMode
import java.util.Locale
import java.util.UUID

private const val TAG = "AiCallAssistantEngine"

class AiCallAssistantEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val pendingSpeechQueue = mutableListOf<PendingUtterance>()
    private val utteranceCallbacks = mutableMapOf<String, () -> Unit>()
    private var speechRecognizer: SpeechRecognizer? = null

    private data class PendingUtterance(
        val text: String,
        val language: AssistantReplyLanguage,
        val isPreview: Boolean,
        val pitch: Float,
        val speechRate: Float,
        val onDone: () -> Unit
    )

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun buildInitialGreeting(
        ownerName: String,
        languageMode: AiAssistantLanguageMode
    ): Pair<String, AssistantReplyLanguage> {
        val safeOwner = ownerName.ifBlank { "the device owner" }
        return when (languageMode) {
            AiAssistantLanguageMode.HINDI ->
                "नमस्ते, मैं $safeOwner का पर्सनल एआई असिस्टेंट हूँ। वे अभी कॉल लेने में व्यस्त हैं। कृपया बताएं कि आप क्यों कॉल कर रहे हैं, मैंने उन्हें सूचित कर दिया है और वे जल्द से जल्द आपको कॉल बैक करेंगे।" to
                    AssistantReplyLanguage.HI

            AiAssistantLanguageMode.BENGALI ->
                "নমস্কার, আমি $safeOwner-এর পার্সোনাল এআই অ্যাসিস্ট্যান্ট বলছি। উনি এই মুহূর্তে কল ধরতে ব্যস্ত আছেন। অনুগ্রহ করে বলুন আপনি কী কারণে কল করছেন, আমি ওনাকে জানিয়ে দিয়েছি এবং উনি যত দ্রুত সম্ভব আপনাকে কল ব্যাক করবেন।" to
                    AssistantReplyLanguage.BN

            AiAssistantLanguageMode.ENGLISH,
            AiAssistantLanguageMode.AUTO,
            AiAssistantLanguageMode.ENGLISH_GREET_AUTO ->
                "Hello, I am a personal AI assistant of $safeOwner. He is busy to take calls right now. Please explain why you are calling, and I've notified him that he will call back as soon as possible." to
                    AssistantReplyLanguage.EN
        }
    }

    fun speak(
        text: String,
        language: AssistantReplyLanguage,
        isPreview: Boolean = false,
        pitch: Float = 1.0f,
        speechRate: Float = 0.98f,
        onDone: () -> Unit
    ) {
        mainHandler.post {
            ensureTtsInitialized()
            if (!ttsReady) {
                pendingSpeechQueue.add(PendingUtterance(text, language, isPreview, pitch, speechRate, onDone))
                return@post
            }
            executeSpeakInternal(text, language, isPreview, pitch, speechRate, onDone)
        }
    }

    fun listenForCallerTurn(
        languageHint: AssistantReplyLanguage,
        languageMode: AiAssistantLanguageMode,
        onSpoken: (String) -> Unit,
        onSilenceOrError: (Int) -> Unit
    ) {
        if (!hasMicPermission()) {
            onSilenceOrError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSilenceOrError(SpeechRecognizer.ERROR_CLIENT)
            return
        }

        mainHandler.post {
            destroyRecognizerOnly()
            runCatching {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit

                    override fun onError(error: Int) {
                        destroyRecognizerOnly()
                        onSilenceOrError(error)
                    }

                    override fun onResults(results: Bundle?) {
                        val spoken = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        destroyRecognizerOnly()
                        if (spoken.isNotEmpty()) {
                            onSpoken(spoken)
                        } else {
                            onSilenceOrError(SpeechRecognizer.ERROR_NO_MATCH)
                        }
                    }
                })

                val recognitionLangTag = when (languageMode) {
                    AiAssistantLanguageMode.HINDI -> "hi-IN"
                    AiAssistantLanguageMode.BENGALI -> "bn-IN"
                    AiAssistantLanguageMode.ENGLISH -> "en-IN"
                    AiAssistantLanguageMode.AUTO,
                    AiAssistantLanguageMode.ENGLISH_GREET_AUTO -> languageHint.languageTag
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLangTag)
                    putExtra(
                        RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                        arrayListOf("en-IN", "hi-IN", "bn-IN")
                    )
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer = recognizer
                recognizer.startListening(intent)
            }.onFailure { err ->
                Log.e(TAG, "Failed to start AI call speech recognizer", err)
                onSilenceOrError(SpeechRecognizer.ERROR_CLIENT)
            }
        }
    }

    fun previewSampleVoice(
        ownerName: String,
        language: AssistantReplyLanguage,
        pitch: Float = 1.0f,
        speechRate: Float = 0.98f,
        onStatus: (String) -> Unit
    ) {
        val safeOwner = ownerName.ifBlank { "AJ" }
        val (sampleText, label) = when (language) {
            AssistantReplyLanguage.EN ->
                "Hello, I am a personal AI assistant of $safeOwner. He is busy to take calls right now. Please explain why you are calling." to
                    "Playing English AI Assistant voice preview..."
            AssistantReplyLanguage.HI ->
                "नमस्ते, मैं $safeOwner का पर्सनल एआई असिस्टेंट हूँ। वे अभी कॉल लेने में व्यस्त हैं। कृपया बताएं आप क्यों कॉल कर रहे हैं।" to
                    "Playing Hindi AI Assistant voice preview..."
            AssistantReplyLanguage.BN ->
                "নমস্কার, আমি $safeOwner-এর পার্সোনাল এআই অ্যাসিস্ট্যান্ট বলছি। উনি এখন ব্যস্ত আছেন। অনুগ্রহ করে বলুন আপনি কেন কল করছেন।" to
                    "Playing Bengali AI Assistant voice preview..."
        }
        onStatus(label)
        speak(text = sampleText, language = language, isPreview = true, pitch = pitch, speechRate = speechRate, onDone = {})
    }

    fun stopAll() {
        mainHandler.post {
            pendingSpeechQueue.clear()
            utteranceCallbacks.clear()
            runCatching { tts?.stop() }
            destroyRecognizerOnly()
        }
    }

    fun release() {
        mainHandler.post {
            pendingSpeechQueue.clear()
            utteranceCallbacks.clear()
            destroyRecognizerOnly()
            runCatching {
                tts?.stop()
                tts?.shutdown()
            }
            tts = null
            ttsReady = false
        }
    }

    private fun ensureTtsInitialized() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post {
                ttsReady = (status == TextToSpeech.SUCCESS)
                if (ttsReady) {
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) {
                            mainHandler.post {
                                utteranceCallbacks.remove(utteranceId)?.invoke()
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post {
                                utteranceCallbacks.remove(utteranceId)?.invoke()
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainHandler.post {
                                utteranceCallbacks.remove(utteranceId)?.invoke()
                            }
                        }
                    })
                    val queued = pendingSpeechQueue.toList()
                    pendingSpeechQueue.clear()
                    queued.forEach { item ->
                        executeSpeakInternal(item.text, item.language, item.isPreview, item.pitch, item.speechRate, item.onDone)
                    }
                } else {
                    Log.e(TAG, "TextToSpeech engine failed to initialize: status=$status")
                    val queued = pendingSpeechQueue.toList()
                    pendingSpeechQueue.clear()
                    queued.forEach { it.onDone() }
                }
            }
        }
    }

    private fun executeSpeakInternal(
        text: String,
        language: AssistantReplyLanguage,
        isPreview: Boolean,
        pitch: Float,
        speechRate: Float,
        onDone: () -> Unit
    ) {
        val engine = tts
        if (engine == null || !ttsReady) {
            onDone()
            return
        }

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(
                if (isPreview) AudioAttributes.USAGE_MEDIA
                else AudioAttributes.USAGE_VOICE_COMMUNICATION
            )
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        engine.setAudioAttributes(audioAttrs)

        applyBestLocale(engine, language)
        engine.setSpeechRate(speechRate.coerceIn(0.65f, 1.45f))
        engine.setPitch(pitch.coerceIn(0.55f, 1.65f))

        val utteranceId = UUID.randomUUID().toString()
        utteranceCallbacks[utteranceId] = onDone
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            utteranceCallbacks.remove(utteranceId)
            onDone()
        }
    }

    private fun applyBestLocale(engine: TextToSpeech, language: AssistantReplyLanguage) {
        val candidates = when (language) {
            AssistantReplyLanguage.HI -> listOf(
                Locale.forLanguageTag("hi-IN"),
                Locale("hi", "IN"),
                Locale("hi")
            )
            AssistantReplyLanguage.BN -> listOf(
                Locale.forLanguageTag("bn-IN"),
                Locale.forLanguageTag("bn-BD"),
                Locale("bn", "IN"),
                Locale("bn")
            )
            AssistantReplyLanguage.EN -> listOf(
                Locale.forLanguageTag("en-IN"),
                Locale.US,
                Locale.ENGLISH
            )
        }

        for (loc in candidates) {
            val avail = engine.isLanguageAvailable(loc)
            if (avail >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = loc
                return
            }
        }
        engine.language = candidates.first()
    }

    private fun destroyRecognizerOnly() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }
}
