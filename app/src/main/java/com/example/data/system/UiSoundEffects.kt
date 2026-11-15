package com.example.data.system

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.util.Log
import com.example.data.preferences.DialpadSoundStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

private const val TAG = "UiSoundEffects"
private const val SAMPLE_RATE = 22050

class UiSoundEffects(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loadedSampleIds = ConcurrentHashMap<String, Int>()
    private val readySamples = ConcurrentHashMap<Int, Boolean>()

    @Volatile
    private var dtmfFallbackGenerator: ToneGenerator? = null

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                readySamples[sampleId] = true
            }
        }
        scope.launch {
            prepareSoundBank()
        }
    }

    private fun prepareSoundBank() {
        runCatching {
            val cacheDir = File(context.cacheDir, "dial_tones_v2").apply { mkdirs() }
            val keys = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#", "+")

            keys.forEachIndexed { index, digit ->
                // 1. Classic crisp DTMF dual-tone
                val dtmfKey = "${DialpadSoundStyle.CLASSIC_DTMF.id}_$digit"
                val dtmfFile = File(cacheDir, "dtmf_${sanitizeKey(digit)}.wav")
                if (!dtmfFile.exists() || dtmfFile.length() < 64L) {
                    val (f1, f2) = dtmfFrequencies(digit)
                    writeWav(dtmfFile, synthesizeDtmfSamples(f1, f2))
                }
                val dtmfSoundId = soundPool.load(dtmfFile.absolutePath, 1)
                if (dtmfSoundId > 0) loadedSampleIds[dtmfKey] = dtmfSoundId

                // 2. Nothing Soft Pop percussive tonal click
                val softKey = "${DialpadSoundStyle.NOTHING_SOFT.id}_$digit"
                val softFile = File(cacheDir, "soft_${sanitizeKey(digit)}.wav")
                if (!softFile.exists() || softFile.length() < 64L) {
                    writeWav(softFile, synthesizeSoftPopSamples(softNoteFrequency(index)))
                }
                val softSoundId = soundPool.load(softFile.absolutePath, 1)
                if (softSoundId > 0) loadedSampleIds[softKey] = softSoundId

                // 3. Dot Chime harmonic pip
                val chimeKey = "${DialpadSoundStyle.DOT_CHIME.id}_$digit"
                val chimeFile = File(cacheDir, "chime_${sanitizeKey(digit)}.wav")
                if (!chimeFile.exists() || chimeFile.length() < 64L) {
                    writeWav(chimeFile, synthesizeChimeSamples(chimeFrequency(index)))
                }
                val chimeSoundId = soundPool.load(chimeFile.absolutePath, 1)
                if (chimeSoundId > 0) loadedSampleIds[chimeKey] = chimeSoundId
            }

            // 4. Subtle UI tactile click
            val clickFile = File(cacheDir, "ui_click.wav")
            if (!clickFile.exists() || clickFile.length() < 64L) {
                writeWav(clickFile, synthesizeUiClickSamples())
            }
            val clickId = soundPool.load(clickFile.absolutePath, 1)
            if (clickId > 0) loadedSampleIds["ui_click"] = clickId
        }.onFailure {
            Log.w(TAG, "Failed preparing dial sound bank: ${it.message}")
        }
    }

    fun playDialTone(digit: String, style: DialpadSoundStyle) {
        if (style == DialpadSoundStyle.OFF) return
        val normalizedDigit = when (digit) {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#", "+" -> digit
            else -> return
        }
        val key = "${style.id}_$normalizedDigit"
        val sampleId = loadedSampleIds[key]
        if (sampleId != null && readySamples[sampleId] == true) {
            val streamId = soundPool.play(sampleId, 0.85f, 0.85f, 1, 0, 1.0f)
            if (streamId != 0) return
        }

        // Instant fallback while SoundPool finishes async preload on first tap
        playFallbackTone(normalizedDigit)
    }

    fun playUiClick(enabled: Boolean) {
        if (!enabled) return
        val sampleId = loadedSampleIds["ui_click"]
        if (sampleId != null && readySamples[sampleId] == true) {
            val streamId = soundPool.play(sampleId, 0.48f, 0.48f, 0, 0, 1.0f)
            if (streamId != 0) return
        }
        runCatching {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.55f)
        }
    }

    private fun playFallbackTone(digit: String) {
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
            "+" -> ToneGenerator.TONE_DTMF_D
            else -> ToneGenerator.TONE_DTMF_0
        }
        runCatching {
            if (dtmfFallbackGenerator == null) {
                dtmfFallbackGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            }
            dtmfFallbackGenerator?.startTone(tone, 95)
        }
    }

    fun release() {
        scope.cancel()
        runCatching { soundPool.release() }
        runCatching { dtmfFallbackGenerator?.release() }
        dtmfFallbackGenerator = null
    }

    private fun sanitizeKey(digit: String): String = when (digit) {
        "*" -> "star"
        "#" -> "pound"
        "+" -> "plus"
        else -> digit
    }

    private fun dtmfFrequencies(digit: String): Pair<Double, Double> = when (digit) {
        "1" -> 697.0 to 1209.0
        "2" -> 697.0 to 1336.0
        "3" -> 697.0 to 1477.0
        "4" -> 770.0 to 1209.0
        "5" -> 770.0 to 1336.0
        "6" -> 770.0 to 1477.0
        "7" -> 852.0 to 1209.0
        "8" -> 852.0 to 1336.0
        "9" -> 852.0 to 1477.0
        "*" -> 941.0 to 1209.0
        "0" -> 941.0 to 1336.0
        "#" -> 941.0 to 1477.0
        "+" -> 941.0 to 1633.0
        else -> 941.0 to 1336.0
    }

    private fun softNoteFrequency(index: Int): Double {
        val pentatonicHz = doubleArrayOf(
            392.00, // 0
            440.00, // 1
            493.88, // 2
            523.25, // 3
            587.33, // 4
            659.25, // 5
            698.46, // 6
            783.99, // 7
            880.00, // 8
            987.77, // 9
            349.23, // *
            554.37, // #
            1046.50 // +
        )
        return pentatonicHz[index.coerceIn(0, pentatonicHz.lastIndex)]
    }

    private fun chimeFrequency(index: Int): Double {
        val base = 520.0 + index * 46.0
        return base.coerceAtMost(1220.0)
    }

    private fun synthesizeDtmfSamples(f1: Double, f2: Double): ShortArray {
        val durationMs = 92
        val count = (SAMPLE_RATE * durationMs) / 1000
        val attackSamples = (SAMPLE_RATE * 4) / 1000
        val releaseSamples = (SAMPLE_RATE * 18) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val wave = 0.52 * sin(2.0 * PI * f1 * t) + 0.52 * sin(2.0 * PI * f2 * t)
            // Subtle crisp 3ms tactile transient at key press start
            val transient = if (i < 66) 0.14 * exp(-i / 18.0) * sin(2.0 * PI * 2400.0 * t) else 0.0
            val env = when {
                i < attackSamples -> 0.5 * (1.0 - cos(PI * i / attackSamples))
                i > count - releaseSamples -> {
                    val rem = (count - i).coerceAtLeast(0)
                    0.5 * (1.0 - cos(PI * rem / releaseSamples))
                }
                else -> 1.0
            }
            ((wave + transient) * env * 18500.0).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private fun synthesizeSoftPopSamples(freq: Double): ShortArray {
        val durationMs = 76
        val count = (SAMPLE_RATE * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 42.0) * (1.0 - exp(-t * 650.0))
            val pitchDrop = freq * (1.0 + 0.12 * exp(-t * 120.0))
            val wave = 0.78 * sin(2.0 * PI * pitchDrop * t) + 0.22 * sin(2.0 * PI * pitchDrop * 2.0 * t)
            (wave * env * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private fun synthesizeChimeSamples(freq: Double): ShortArray {
        val durationMs = 88
        val count = (SAMPLE_RATE * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 34.0) * (1.0 - exp(-t * 700.0))
            val wave = 0.68 * sin(2.0 * PI * freq * t) + 0.32 * sin(2.0 * PI * (freq * 1.5) * t)
            (wave * env * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private fun synthesizeUiClickSamples(): ShortArray {
        val durationMs = 22
        val count = (SAMPLE_RATE * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 170.0)
            val wave = 0.65 * sin(2.0 * PI * 1450.0 * t) + 0.35 * sin(2.0 * PI * 2900.0 * t)
            (wave * env * 14500.0).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private fun writeWav(targetFile: File, samples: ShortArray) {
        val dataBytes = samples.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataBytes)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // AudioFormat = PCM
            putShort(1) // NumChannels = Mono
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2) // ByteRate
            putShort(2) // BlockAlign
            putShort(16) // BitsPerSample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataBytes)
        }.array()

        val pcmBuffer = ByteArrayOutputStream(44 + dataBytes)
        pcmBuffer.write(header)
        val sampleBytes = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { sampleBytes.putShort(it) }
        pcmBuffer.write(sampleBytes.array())

        FileOutputStream(targetFile).use { out ->
            out.write(pcmBuffer.toByteArray())
        }
    }
}
