package com.example.data.system

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.telecom.CallAudioState
import com.example.data.preferences.AudioOutputMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ActiveAudioRoute(val label: String) {
    BLUETOOTH("Bluetooth"),
    EAR_RECEIVER("Ear Receiver"),
    SPEAKER("Speaker")
}

class AudioRouteManager(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _bluetoothConnected = MutableStateFlow(detectBluetoothHeadset())
    val bluetoothConnected: StateFlow<Boolean> = _bluetoothConnected.asStateFlow()

    private val _activeRoute = MutableStateFlow(
        if (_bluetoothConnected.value) ActiveAudioRoute.BLUETOOTH else ActiveAudioRoute.EAR_RECEIVER
    )
    val activeRoute: StateFlow<ActiveAudioRoute> = _activeRoute.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            _bluetoothConnected.value = detectBluetoothHeadset()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            _bluetoothConnected.value = detectBluetoothHeadset()
        }
    }

    init {
        audioManager?.registerAudioDeviceCallback(deviceCallback, null)
    }

    fun detectBluetoothHeadset(): Boolean {
        val manager = audioManager ?: return false
        val outputs = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return outputs.any { dev ->
            dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
    }

    fun resolvePreferredRoute(preference: AudioOutputMode): ActiveAudioRoute {
        val bt = detectBluetoothHeadset()
        _bluetoothConnected.value = bt
        return when (preference) {
            AudioOutputMode.AUTO_BT_EAR ->
                if (bt) ActiveAudioRoute.BLUETOOTH else ActiveAudioRoute.EAR_RECEIVER
            AudioOutputMode.BLUETOOTH ->
                if (bt) ActiveAudioRoute.BLUETOOTH else ActiveAudioRoute.EAR_RECEIVER
            AudioOutputMode.EAR_RECEIVER -> ActiveAudioRoute.EAR_RECEIVER
            AudioOutputMode.SPEAKER -> ActiveAudioRoute.SPEAKER
        }
    }

    fun applyRoute(
        route: ActiveAudioRoute,
        onSystemRoute: ((Int) -> Unit)? = null
    ) {
        val manager = audioManager
        _activeRoute.value = route
        when (route) {
            ActiveAudioRoute.BLUETOOTH -> {
                @Suppress("DEPRECATION")
                manager?.isSpeakerphoneOn = false
                @Suppress("DEPRECATION")
                runCatching { manager?.startBluetoothSco(); manager?.isBluetoothScoOn = true }
                onSystemRoute?.invoke(CallAudioState.ROUTE_BLUETOOTH)
            }
            ActiveAudioRoute.EAR_RECEIVER -> {
                @Suppress("DEPRECATION")
                runCatching { manager?.isBluetoothScoOn = false; manager?.stopBluetoothSco() }
                @Suppress("DEPRECATION")
                manager?.isSpeakerphoneOn = false
                onSystemRoute?.invoke(CallAudioState.ROUTE_EARPIECE)
            }
            ActiveAudioRoute.SPEAKER -> {
                @Suppress("DEPRECATION")
                runCatching { manager?.isBluetoothScoOn = false; manager?.stopBluetoothSco() }
                @Suppress("DEPRECATION")
                manager?.isSpeakerphoneOn = true
                onSystemRoute?.invoke(CallAudioState.ROUTE_SPEAKER)
            }
        }
    }

    fun release() {
        audioManager?.unregisterAudioDeviceCallback(deviceCallback)
    }
}
