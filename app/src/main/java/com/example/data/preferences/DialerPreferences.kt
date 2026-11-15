package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.DialerFontPreset
import com.example.ui.theme.DialerThemePreset
import com.example.ui.theme.FontStyleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dialerDataStore: DataStore<Preferences> by preferencesDataStore(name = "dot_dial_prefs")

enum class AudioOutputMode(val id: String, val label: String) {
    AUTO_BT_EAR("auto_bt_ear", "Auto (Bluetooth -> Ear)"),
    EAR_RECEIVER("ear_receiver", "Ear Receiver"),
    BLUETOOTH("bluetooth", "Bluetooth"),
    SPEAKER("speaker", "Speaker");

    companion object {
        fun fromId(id: String?): AudioOutputMode =
            entries.firstOrNull { it.id == id } ?: AUTO_BT_EAR
    }
}

enum class AutoRecordMode(val id: String, val label: String) {
    OFF("off", "Off"),
    UNKNOWN_ONLY("unknown", "Unknown Callers"),
    ALL_CALLS("all", "All Calls");

    companion object {
        fun fromId(id: String?): AutoRecordMode =
            entries.firstOrNull { it.id == id } ?: OFF
    }
}

data class DialerSettings(
    val themePreset: DialerThemePreset = DialerThemePreset.NOTHING_DARK,
    val amoledBlackEnabled: Boolean = false,
    val customBgHex: Long = 0xFF0A0A0EL,
    val customSurfaceHex: Long = 0xFF17171FL,
    val customAccentHex: Long = 0xFFD71921L,
    val customTextHex: Long = 0xFFF5F5F7L,
    val fontPreset: DialerFontPreset = DialerFontPreset.NOTHING_DOT,
    val fontStyleMode: FontStyleMode = FontStyleMode.NORMAL,
    val fontScale: Float = 1.0f,
    val coolJazzGlowEnabled: Boolean = true,
    val customFontPath: String? = null,
    val dotDensity: Float = 0.65f,
    val uiSoundsEnabled: Boolean = true,
    val preferredFps: Int = 90,
    val adaptiveRefreshRate: Boolean = true,
    val ultraSmoothMode: Boolean = false,
    val audioOutputMode: AudioOutputMode = AudioOutputMode.AUTO_BT_EAR,
    val autoRecordMode: AutoRecordMode = AutoRecordMode.OFF,
    val silentCallRecording: Boolean = true,
    val autoSummarizeEnabled: Boolean = false,
    val voiceCommandControlEnabled: Boolean = true,
    val activeVibrationPatternId: Long = 1L,
    val defaultVideoBackdropUri: String? = null,
    val customRingtoneUri: String? = null,
    val videoRedTintStrength: Float = 0.58f,
    val vaultPin: String = "",
    val syncedGoogleAccounts: Set<String> = emptySet(),
    val googleSyncEnabled: Boolean = true
)

class DialerPreferences(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val CUSTOM_BG_HEX = longPreferencesKey("custom_bg_hex")
        val CUSTOM_SURFACE_HEX = longPreferencesKey("custom_surface_hex")
        val CUSTOM_ACCENT_HEX = longPreferencesKey("custom_accent_hex")
        val CUSTOM_TEXT_HEX = longPreferencesKey("custom_text_hex")
        val FONT_PRESET = stringPreferencesKey("font_preset")
        val FONT_STYLE_MODE = stringPreferencesKey("font_style_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val COOL_JAZZ_GLOW = booleanPreferencesKey("cool_jazz_glow")
        val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val DOT_DENSITY = floatPreferencesKey("dot_density")
        val UI_SOUNDS = booleanPreferencesKey("ui_sounds")
        val PREFERRED_FPS = intPreferencesKey("preferred_fps")
        val ADAPTIVE_REFRESH_RATE = booleanPreferencesKey("adaptive_refresh_rate")
        val ULTRA_SMOOTH_MODE = booleanPreferencesKey("ultra_smooth_mode")
        val AUDIO_OUTPUT_MODE = stringPreferencesKey("audio_output_mode")
        val AUTO_RECORD_MODE = stringPreferencesKey("auto_record_mode")
        val SILENT_RECORDING = booleanPreferencesKey("silent_recording")
        val AUTO_SUMMARIZE = booleanPreferencesKey("auto_summarize")
        val VOICE_CONTROL = booleanPreferencesKey("voice_control")
        val ACTIVE_VIBRATION_ID = longPreferencesKey("active_vibration_id")
        val DEFAULT_VIDEO_URI = stringPreferencesKey("default_video_uri")
        val CUSTOM_RINGTONE_URI = stringPreferencesKey("custom_ringtone_uri")
        val VIDEO_RED_TINT = floatPreferencesKey("video_red_tint")
        val VAULT_PIN = stringPreferencesKey("vault_pin")
        val SYNCED_GOOGLE_ACCOUNTS = stringSetPreferencesKey("synced_google_accounts")
        val GOOGLE_SYNC_ENABLED = booleanPreferencesKey("google_sync_enabled")
    }

    val settingsFlow: Flow<DialerSettings> = context.dialerDataStore.data.map { prefs ->
        DialerSettings(
            themePreset = DialerThemePreset.fromId(prefs[Keys.THEME_PRESET]),
            amoledBlackEnabled = prefs[Keys.AMOLED_BLACK] ?: false,
            customBgHex = prefs[Keys.CUSTOM_BG_HEX] ?: 0xFF0A0A0EL,
            customSurfaceHex = prefs[Keys.CUSTOM_SURFACE_HEX] ?: 0xFF17171FL,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX] ?: 0xFFD71921L,
            customTextHex = prefs[Keys.CUSTOM_TEXT_HEX] ?: 0xFFF5F5F7L,
            fontPreset = DialerFontPreset.fromId(prefs[Keys.FONT_PRESET]),
            fontStyleMode = FontStyleMode.fromId(prefs[Keys.FONT_STYLE_MODE]),
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            coolJazzGlowEnabled = prefs[Keys.COOL_JAZZ_GLOW] ?: true,
            customFontPath = prefs[Keys.CUSTOM_FONT_PATH]?.takeIf { it.isNotBlank() },
            dotDensity = prefs[Keys.DOT_DENSITY] ?: 0.65f,
            uiSoundsEnabled = prefs[Keys.UI_SOUNDS] ?: true,
            preferredFps = prefs[Keys.PREFERRED_FPS] ?: 90,
            adaptiveRefreshRate = prefs[Keys.ADAPTIVE_REFRESH_RATE] ?: true,
            ultraSmoothMode = prefs[Keys.ULTRA_SMOOTH_MODE] ?: false,
            audioOutputMode = AudioOutputMode.fromId(prefs[Keys.AUDIO_OUTPUT_MODE]),
            autoRecordMode = AutoRecordMode.fromId(prefs[Keys.AUTO_RECORD_MODE]),
            silentCallRecording = prefs[Keys.SILENT_RECORDING] ?: true,
            autoSummarizeEnabled = prefs[Keys.AUTO_SUMMARIZE] ?: false,
            voiceCommandControlEnabled = prefs[Keys.VOICE_CONTROL] ?: true,
            activeVibrationPatternId = prefs[Keys.ACTIVE_VIBRATION_ID] ?: 1L,
            defaultVideoBackdropUri = prefs[Keys.DEFAULT_VIDEO_URI]?.takeIf { it.isNotBlank() },
            customRingtoneUri = prefs[Keys.CUSTOM_RINGTONE_URI]?.takeIf { it.isNotBlank() },
            videoRedTintStrength = prefs[Keys.VIDEO_RED_TINT] ?: 0.58f,
            vaultPin = prefs[Keys.VAULT_PIN] ?: "",
            syncedGoogleAccounts = prefs[Keys.SYNCED_GOOGLE_ACCOUNTS] ?: emptySet(),
            googleSyncEnabled = prefs[Keys.GOOGLE_SYNC_ENABLED] ?: true
        )
    }

    suspend fun setThemePreset(preset: DialerThemePreset) {
        context.dialerDataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.AMOLED_BLACK] = enabled }
    }

    suspend fun setCustomThemeColors(bgHex: Long, surfaceHex: Long, accentHex: Long, textHex: Long) {
        context.dialerDataStore.edit {
            it[Keys.CUSTOM_BG_HEX] = bgHex
            it[Keys.CUSTOM_SURFACE_HEX] = surfaceHex
            it[Keys.CUSTOM_ACCENT_HEX] = accentHex
            it[Keys.CUSTOM_TEXT_HEX] = textHex
            it[Keys.THEME_PRESET] = DialerThemePreset.CUSTOM.id
        }
    }

    suspend fun setFontPreset(preset: DialerFontPreset) {
        context.dialerDataStore.edit { it[Keys.FONT_PRESET] = preset.id }
    }

    suspend fun setFontStyleMode(mode: FontStyleMode) {
        context.dialerDataStore.edit { it[Keys.FONT_STYLE_MODE] = mode.id }
    }

    suspend fun setFontScale(scale: Float) {
        context.dialerDataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.30f) }
    }

    suspend fun setCoolJazzGlow(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.COOL_JAZZ_GLOW] = enabled }
    }

    suspend fun setCustomFontPath(path: String?) {
        context.dialerDataStore.edit {
            if (path.isNullOrBlank()) it.remove(Keys.CUSTOM_FONT_PATH)
            else {
                it[Keys.CUSTOM_FONT_PATH] = path
                it[Keys.FONT_PRESET] = DialerFontPreset.CUSTOM.id
            }
        }
    }

    suspend fun setDotDensity(density: Float) {
        context.dialerDataStore.edit { it[Keys.DOT_DENSITY] = density.coerceIn(0.2f, 1.0f) }
    }

    suspend fun setUiSoundsEnabled(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.UI_SOUNDS] = enabled }
    }

    suspend fun setPreferredFps(fps: Int) {
        val clamped = if (fps == 0) 0 else fps.coerceIn(60, 120)
        context.dialerDataStore.edit { it[Keys.PREFERRED_FPS] = clamped }
    }

    suspend fun setAdaptiveRefreshRate(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.ADAPTIVE_REFRESH_RATE] = enabled }
    }

    suspend fun setUltraSmoothMode(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.ULTRA_SMOOTH_MODE] = enabled }
    }

    suspend fun setAudioOutputMode(mode: AudioOutputMode) {
        context.dialerDataStore.edit { it[Keys.AUDIO_OUTPUT_MODE] = mode.id }
    }

    suspend fun setAutoRecordMode(mode: AutoRecordMode) {
        context.dialerDataStore.edit { it[Keys.AUTO_RECORD_MODE] = mode.id }
    }

    suspend fun setSilentCallRecording(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.SILENT_RECORDING] = enabled }
    }

    suspend fun setAutoSummarizeEnabled(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.AUTO_SUMMARIZE] = enabled }
    }

    suspend fun setVoiceCommandControl(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.VOICE_CONTROL] = enabled }
    }

    suspend fun setActiveVibrationPattern(id: Long) {
        context.dialerDataStore.edit { it[Keys.ACTIVE_VIBRATION_ID] = id }
    }

    suspend fun setDefaultVideoBackdropUri(uri: String?) {
        context.dialerDataStore.edit {
            if (uri.isNullOrBlank()) it.remove(Keys.DEFAULT_VIDEO_URI)
            else it[Keys.DEFAULT_VIDEO_URI] = uri
        }
    }

    suspend fun setCustomRingtoneUri(uri: String?) {
        context.dialerDataStore.edit {
            if (uri.isNullOrBlank()) it.remove(Keys.CUSTOM_RINGTONE_URI)
            else it[Keys.CUSTOM_RINGTONE_URI] = uri
        }
    }

    suspend fun setVideoRedTintStrength(strength: Float) {
        context.dialerDataStore.edit { it[Keys.VIDEO_RED_TINT] = strength.coerceIn(0f, 0.9f) }
    }

    suspend fun setVaultPin(pin: String) {
        context.dialerDataStore.edit { it[Keys.VAULT_PIN] = pin }
    }

    suspend fun addGoogleSyncAccount(accountEmail: String) {
        val clean = accountEmail.trim()
        if (clean.isEmpty()) return
        context.dialerDataStore.edit {
            val current = it[Keys.SYNCED_GOOGLE_ACCOUNTS] ?: emptySet()
            it[Keys.SYNCED_GOOGLE_ACCOUNTS] = current + clean
            it[Keys.GOOGLE_SYNC_ENABLED] = true
        }
    }

    suspend fun removeGoogleSyncAccount(accountEmail: String) {
        context.dialerDataStore.edit {
            val current = it[Keys.SYNCED_GOOGLE_ACCOUNTS] ?: emptySet()
            it[Keys.SYNCED_GOOGLE_ACCOUNTS] = current - accountEmail
        }
    }

    suspend fun setGoogleSyncEnabled(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.GOOGLE_SYNC_ENABLED] = enabled }
    }
}
