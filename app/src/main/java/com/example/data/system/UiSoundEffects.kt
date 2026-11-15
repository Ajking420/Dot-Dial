package com.example.data.system

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

private const val TAG = "UiSoundEffects"

class UiSoundEffects(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @Volatile
    private var dtmfGenerator: ToneGenerator? = null

    private fun getOrCreateToneGenerator(): ToneGenerator? {
        if (dtmfGenerator == null) {
            dtmfGenerator = runCatching {
                ToneGenerator(AudioManager.STREAM_DTMF, 55)
            }.onFailure {
                Log.w(TAG, "ToneGenerator unavailable: ${it.message}")
            }.getOrNull()
        }
        return dtmfGenerator
    }

    fun playDialTone(digit: String, enabled: Boolean) {
        if (!enabled) return
        val tone = when (digit) {
            "0" -> ToneGenerator.TONE_DTMF_0
            "1" -> ToneGenerator.TONE_DTMF_1
            "2" -> ToneGenerator.TONE_DTMF_2
            "3" -> ToneGenerator.TONE_DTMF_3
            "4" -> ToneGenerator.TONE_DTMF_4
            "5" -> ToneGenerator.TONE_DTMF_5
            "6" -> ToneGenerator.TONE_DTMF_6
            "7" -> ToneGenerator.TONE_DTMF_7
            "8" -> ToneGenerator.TONE_DTMF_8
            "9" -> ToneGenerator.TONE_DTMF_9
            "*" -> ToneGenerator.TONE_DTMF_S
            "#" -> ToneGenerator.TONE_DTMF_P
            else -> ToneGenerator.TONE_DTMF_0
        }
        runCatching {
            getOrCreateToneGenerator()?.startTone(tone, 110)
        }
    }

    fun playUiClick(enabled: Boolean) {
        if (!enabled) return
        runCatching {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.45f)
        }
    }

    fun release() {
        runCatching { dtmfGenerator?.release() }
        dtmfGenerator = null
    }
}
