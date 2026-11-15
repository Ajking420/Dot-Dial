package com.example.telecom

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object CallNotificationManager {

    private const val CHANNEL_ID = "dot_dial_live_call_channel"
    private const val NOTIFICATION_ID = 3030

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active & Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays live call timer, speaker toggle, and end call controls"
                setSound(null, null)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    fun showOrUpdateCallNotification(
        context: Context,
        callerName: String,
        phoneNumber: String,
        isRinging: Boolean,
        isSpeakerOn: Boolean,
        callStartMs: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_END_CALL
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speakerIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_TOGGLE_SPEAKER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_ANSWER_CALL
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName.ifBlank { phoneNumber })
            .setContentText(
                if (isRinging) "Incoming call (muted) • $phoneNumber"
                else "Ongoing call • $phoneNumber"
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent)

        if (!isRinging && callStartMs > 0L) {
            builder.setUsesChronometer(true)
            builder.setWhen(callStartMs)
        }

        if (isRinging) {
            builder.addAction(0, "Answer", answerIntent)
            builder.addAction(0, "End", endIntent)
        } else {
            builder.addAction(0, if (isSpeakerOn) "Speaker On" else "Speaker", speakerIntent)
            builder.addAction(0, "End", endIntent)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun cancel(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}
