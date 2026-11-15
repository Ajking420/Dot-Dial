package com.example.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.example.MainActivity

class DotInCallService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallSessionBridge.onSystemCallStateChanged(call, state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        CallSessionBridge.attachService(this)
    }

    override fun onDestroy() {
        CallSessionBridge.attachService(null)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        CallSessionBridge.onSystemCallAdded(call)
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(launchIntent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        CallSessionBridge.onSystemCallRemoved(call)
    }
}
