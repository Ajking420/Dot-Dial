package com.example.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.preferences.VoiceChangerPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

private const val TAG = "InCallVoiceChanger"
private const val SAMPLE_RATE_HZ = 16_000

class InCallVoiceChangerEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var dspJob: Job? = null
    private var previewJob: Job? = null

    @Volatile
    private var currentPreset: VoiceChangerPreset = VoiceChangerPreset.NORMAL

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun updatePreset(preset: VoiceChangerPreset) {
        currentPreset = preset
    }

    fun startLiveDsp(preset: VoiceChangerPreset): Boolean {
        currentPreset = preset
        if (preset == VoiceChangerPreset.NORMAL) {
            stopLiveDsp()
            return false
        }
        if (!hasMicPermission()) return false
        if (dspJob?.isActive == true) return true

        val minInBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        val minOutBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        dspJob = scope.launch {
            var recorder: AudioRecord? = null
            var player: AudioTrack? = null
            try {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minInBytes * 2
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE_HZ,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minInBytes * 2
                    )
                }

                player = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE_HZ)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minOutBytes * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                if (recorder.state != AudioRecord.STATE_INITIALIZED ||
                    player.state != AudioTrack.STATE_INITIALIZED
                ) {
                    return@launch
                }

                recorder.startRecording()
                player.play()

                val frameSamples = (minInBytes / 2).coerceIn(512, 1024)
                val inputBuf = ShortArray(frameSamples)
                val outputBuf = ShortArray(frameSamples)
                var phaseRad = 0.0
                var lpState = 0f
                var hpPrevIn = 0f
                var hpPrevOut = 0f

                while (isActive) {
                    val readCount = recorder.read(inputBuf, 0, frameSamples)
                    if (readCount <= 0) continue

                    val activePreset = currentPreset
                    if (activePreset == VoiceChangerPreset.NORMAL) {
                        player.write(inputBuf, 0, readCount)
                        continue
                    }

                    processPcmBlock(
                        input = inputBuf,
                        output = outputBuf,
                        count = readCount,
                        preset = activePreset,
                        phaseRef = { step ->
                            val cur = phaseRad
                            phaseRad = (phaseRad + step) % (2.0 * PI)
                            cur
                        },
                        lpFilter = { sample, alpha ->
                            lpState = alpha * sample + (1f - alpha) * lpState
                            lpState
                        },
                        hpUpdate = { sample ->
                            val out = 0.88f * (hpPrevOut + sample - hpPrevIn)
                            hpPrevIn = sample
                            hpPrevOut = out
                            out
                        }
                    )
                    player.write(outputBuf, 0, readCount)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Live voice DSP loop stopped cleanly: ${e.message}")
            } finally {
                runCatching { recorder?.stop() }
                runCatching { recorder?.release() }
                runCatching { player?.stop() }
                runCatching { player?.release() }
            }
        }
        return true
    }

    fun stopLiveDsp() {
        dspJob?.cancel()
        dspJob = null
    }

    fun previewPreset(preset: VoiceChangerPreset) {
        previewJob?.cancel()
        previewJob = scope.launch {
            val totalSamples = SAMPLE_RATE_HZ * 14 / 10 // 1.4s crisp vocal-formant sweep
            val raw = ShortArray(totalSamples)
            val processed = ShortArray(totalSamples)

            // Generate a warm harmonic speech-like fundamental + formants so user immediately hears
            // each Voice FX transformation cleanly inside Settings.
            val baseFreq = 152.0
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE_HZ
                val env = when {
                    i < 800 -> i / 800f
                    i > totalSamples - 1600 -> (totalSamples - i) / 1600f
                    else -> 1f
                }
                val vib = 1.0 + 0.015 * sin(2.0 * PI * 5.2 * t)
                val f0 = baseFreq * vib * (1.0 + 0.14 * sin(2.0 * PI * 1.4 * t))
                val wave = 0.55 * sin(2.0 * PI * f0 * t) +
                    0.30 * sin(2.0 * PI * f0 * 2.0 * t) +
                    0.15 * sin(2.0 * PI * f0 * 3.0 * t)
                raw[i] = (wave * env * 14_000.0).roundToInt().coerceIn(-32767, 32767).toShort()
            }

            var phase = 0.0
            var lp = 0f
            var hpIn = 0f
            var hpOut = 0f
            processPcmBlock(
                input = raw,
                output = processed,
                count = totalSamples,
                preset = preset,
                phaseRef = { step ->
                    val c = phase
                    phase = (phase + step) % (2.0 * PI)
                    c
                },
                lpFilter = { sample, alpha ->
                    lp = alpha * sample + (1f - alpha) * lp
                    lp
                },
                hpUpdate = { s ->
                    val o = 0.88f * (hpOut + s - hpIn)
                    hpIn = s
                    hpOut = o
                    o
                }
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE_HZ)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(processed.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            try {
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.write(processed, 0, processed.size)
                    track.play()
                    kotlinx.coroutines.delay(1450L)
                }
            } finally {
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }
    }

    private inline fun processPcmBlock(
        input: ShortArray,
        output: ShortArray,
        count: Int,
        preset: VoiceChangerPreset,
        phaseRef: (Double) -> Double,
        lpFilter: (Float, Float) -> Float,
        hpUpdate: (Float) -> Float
    ) {
        val ratio = preset.pitchFactor
        val lastIdx = (count - 1).coerceAtLeast(1)

        for (i in 0 until count) {
            // Fractional resampling for pitch shift
            val srcPos = (i * ratio) % lastIdx
            val idx0 = srcPos.toInt().coerceIn(0, lastIdx)
            val idx1 = (idx0 + 1).coerceAtMost(lastIdx)
            val frac = srcPos - idx0
            var sample = (input[idx0] * (1f - frac) + input[idx1] * frac) / 32768f

            sample = when (preset) {
                VoiceChangerPreset.NORMAL -> input[i] / 32768f
                VoiceChangerPreset.DEEP_MALE -> {
                    val filtered = lpFilter(sample, 0.34f)
                    tanh((filtered * 1.35f).toDouble()).toFloat()
                }
                VoiceChangerPreset.BRIGHT_FEMALE -> {
                    val highLift = hpUpdate(sample)
                    (sample * 0.65f + highLift * 0.55f).coerceIn(-0.98f, 0.98f)
                }
                VoiceChangerPreset.ROBOT_CYBER -> {
                    val mod = sin(phaseRef(2.0 * PI * 62.0 / SAMPLE_RATE_HZ)).toFloat()
                    val quantized = ((sample * 14f).roundToInt() / 14f)
                    (quantized * (0.45f + 0.55f * mod)).coerceIn(-0.98f, 0.98f)
                }
                VoiceChangerPreset.STUDIO_WARM -> {
                    val warm = tanh((sample * 1.65f).toDouble()).toFloat()
                    (warm * 0.85f).coerceIn(-0.98f, 0.98f)
                }
                VoiceChangerPreset.RADIO_LOFI -> {
                    val bp = hpUpdate(lpFilter(sample, 0.45f))
                    tanh((bp * 2.1f).toDouble()).toFloat() * 0.78f
                }
            }

            output[i] = (sample * 32767f).roundToInt().coerceIn(-32767, 32767).toShort()
        }
    }

    fun release() {
        stopLiveDsp()
        previewJob?.cancel()
        previewJob = null
    }
}
