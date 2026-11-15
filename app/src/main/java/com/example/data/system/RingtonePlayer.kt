package com.example.data.system

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log

private const val TAG = "RingtonePlayer"

class RingtonePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var systemRingtone: Ringtone? = null

    fun startRinging(customRingtoneUri: String?) {
        stop()
        val parsedUri = customRingtoneUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        if (parsedUri != null) {
            val started = runCatching {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, parsedUri)
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = mp
                true
            }.onFailure {
                Log.w(TAG, "Custom ringtone playback failed, falling back to system default: ${it.message}")
            }.getOrDefault(false)
            if (started) return
        }

        runCatching {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, defaultUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            ringtone?.play()
            systemRingtone = ringtone
        }.onFailure {
            Log.w(TAG, "Default ringtone playback skipped: ${it.message}")
        }
    }

    fun stop() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        runCatching {
            systemRingtone?.stop()
        }
        systemRingtone = null
    }
}
