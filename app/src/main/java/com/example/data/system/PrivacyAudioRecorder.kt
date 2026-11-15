package com.example.data.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val TAG = "PrivacyAudioRecorder"

class PrivacyAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L

    private var player: MediaPlayer? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _playingRecordingId = MutableStateFlow<Long?>(null)
    val playingRecordingId: StateFlow<Long?> = _playingRecordingId.asStateFlow()

    private val recordingsDir: File by lazy {
        File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }
    }

    fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun playPrivacyConsentTone() {
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        }
    }

    fun startRecording(playConsentBeep: Boolean): Result<File> {
        if (!hasRecordPermission()) {
            return Result.failure(SecurityException("Microphone permission is required for local call recording."))
        }
        if (_isRecording.value) {
            return currentFile?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Recording already active."))
        }

        return try {
            if (playConsentBeep) {
                playPrivacyConsentTone()
            }

            val outputFile = File(recordingsDir, "dotdial_${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            // Try VOICE_COMMUNICATION first, fallback to MIC for devices restricting call audio source
            try {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            } catch (_: Exception) {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            }

            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(96000)
            mediaRecorder.setAudioSamplingRate(44100)
            mediaRecorder.setOutputFile(outputFile.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()

            recorder = mediaRecorder
            currentFile = outputFile
            startTimeMs = System.currentTimeMillis()
            _isRecording.value = true
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local recording", e)
            cleanupRecorder()
            Result.failure(e)
        }
    }

    fun stopRecording(): Pair<File?, Int> {
        val file = currentFile
        val durationSec = if (startTimeMs > 0L) {
            ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceAtLeast(1)
        } else {
            0
        }

        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "MediaRecorder stop threw exception (short clip)", e)
        } finally {
            cleanupRecorder()
        }

        return Pair(file, durationSec)
    }

    private fun cleanupRecorder() {
        runCatching { recorder?.release() }
        recorder = null
        currentFile = null
        startTimeMs = 0L
        _isRecording.value = false
    }

    fun togglePlayback(recordingId: Long, filePath: String, onError: (String) -> Unit) {
        if (_playingRecordingId.value == recordingId) {
            stopPlayback()
            return
        }

        stopPlayback()
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            onError("Audio file is unavailable or empty on this device.")
            return
        }

        try {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    _playingRecordingId.value = null
                    release()
                    player = null
                }
                prepare()
                start()
            }
            player = mediaPlayer
            _playingRecordingId.value = recordingId
        } catch (e: Exception) {
            Log.e(TAG, "Playback failed", e)
            onError(e.localizedMessage ?: "Unable to play private recording.")
        }
    }

    fun stopPlayback() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        _playingRecordingId.value = null
    }

    fun shredFile(filePath: String) {
        runCatching {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
