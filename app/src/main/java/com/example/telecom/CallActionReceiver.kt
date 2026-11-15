package com.example.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_END_CALL -> {
                CallSessionBridge.endCall()
                CallNotificationManager.cancel(context)
            }
            ACTION_TOGGLE_SPEAKER -> {
                val nextSpeaker = !CallSessionBridge.session.value.isSpeakerOn
                CallSessionBridge.setSpeaker(context, nextSpeaker)
                val s = CallSessionBridge.session.value
                if (s.lifecycle == CallLifecycleState.ACTIVE) {
                    CallNotificationManager.showOrUpdateCallNotification(
                        context = context,
                        callerName = s.displayName,
                        phoneNumber = s.phoneNumber,
                        isRinging = false,
                        isSpeakerOn = nextSpeaker,
                        callStartMs = s.callStartedAtMs
                    )
                }
            }
            ACTION_ANSWER_CALL -> {
                CallSessionBridge.answerCall(acceptToStatusBar = true)
                val s = CallSessionBridge.session.value
                CallNotificationManager.showOrUpdateCallNotification(
                    context = context,
                    callerName = s.displayName,
                    phoneNumber = s.phoneNumber,
                    isRinging = false,
                    isSpeakerOn = s.isSpeakerOn,
                    callStartMs = s.callStartedAtMs
                )
            }
        }
    }

    companion object {
        const val ACTION_END_CALL = "com.example.dotdial.ACTION_END_CALL"
        const val ACTION_TOGGLE_SPEAKER = "com.example.dotdial.ACTION_TOGGLE_SPEAKER"
        const val ACTION_ANSWER_CALL = "com.example.dotdial.ACTION_ANSWER_CALL"
    }
}
