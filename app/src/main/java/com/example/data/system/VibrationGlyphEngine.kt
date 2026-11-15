package com.example.data.system

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.VibrationPatternEntity

class VibrationGlyphEngine(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun playGlyphPattern(pattern: VibrationPatternEntity, repeat: Boolean = false) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val timings = pattern.timingsCsv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .toLongArray()
        val amplitudes = pattern.amplitudesCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull()?.coerceIn(0, 255) }
            .toIntArray()

        if (timings.isEmpty()) return

        val repeatIndex = if (repeat) 0 else -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (vib.hasAmplitudeControl() && amplitudes.size == timings.size) {
                VibrationEffect.createWaveform(timings, amplitudes, repeatIndex)
            } else {
                VibrationEffect.createWaveform(timings, repeatIndex)
            }
            vib.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(timings, repeatIndex)
        }
    }

    fun pulseKeypadTick() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(14L, 120))
        }
    }

    fun stop() {
        vibrator?.cancel()
    }
}
