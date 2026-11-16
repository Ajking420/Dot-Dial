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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

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

enum class UiBackgroundMode(val id: String, val label: String) {
    DOT_MATRIX("dot_matrix", "DOT MATRIX"),
    COLOR_PLATE("color_plate", "COLOR / GRADIENT"),
    WALLPAPER("wallpaper", "WALLPAPER");

    companion object {
        fun fromId(id: String?): UiBackgroundMode =
            entries.firstOrNull { it.id == id } ?: DOT_MATRIX
    }
}

enum class ColorPlateStyle(val id: String, val label: String) {
    SOLID("solid", "BASIC COLOR"),
    GRADIENT("gradient", "GRADIENT");

    companion object {
        fun fromId(id: String?): ColorPlateStyle =
            entries.firstOrNull { it.id == id } ?: GRADIENT
    }
}

enum class DialpadSoundStyle(val id: String, val label: String) {
    CLASSIC_DTMF("classic_dtmf", "DTMF TONE"),
    NOTHING_SOFT("nothing_soft", "SOFT POP"),
    DOT_CHIME("dot_chime", "DOT CHIME"),
    OFF("off", "OFF");

    companion object {
        fun fromId(id: String?): DialpadSoundStyle =
            entries.firstOrNull { it.id == id } ?: CLASSIC_DTMF
    }
}

enum class AiAssistantTriggerMode(val id: String, val label: String) {
    AFTER_DELAY("after_delay", "After Ring Delay"),
    ALL_INCOMING("all_incoming", "All Incoming"),
    UNKNOWN_ONLY("unknown_only", "Unknown Callers"),
    BUSY_MODE_ONLY("busy_only", "When Busy Mode On");

    companion object {
        fun fromId(id: String?): AiAssistantTriggerMode =
            entries.firstOrNull { it.id == id } ?: BUSY_MODE_ONLY
    }
}

enum class CallerBackdropMode(val id: String, val label: String) {
    NONE("none", "Default Glyph"),
    VIDEO("video", "Video Backdrop"),
    PHOTO("photo", "Photo from Gallery"),
    CALLING_CARD("calling_card", "Calling Card");

    companion object {
        fun fromId(id: String?): CallerBackdropMode =
            entries.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) } ?: NONE
    }
}

enum class AiAssistantLanguageMode(val id: String, val label: String) {
    AUTO("auto", "Auto (AI Chooses)"),
    ENGLISH("en", "English"),
    HINDI("hi", "Hindi"),
    BENGALI("bn", "Bengali"),
    ENGLISH_GREET_AUTO("en_greet_auto", "EN Greet -> Caller Lang");

    companion object {
        fun fromId(id: String?): AiAssistantLanguageMode =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

enum class NewContactStorageMode(val id: String, val label: String) {
    GOOGLE_ACCOUNT("google_account", "Google Account"),
    DEVICE("device", "Device Storage");

    companion object {
        fun fromId(id: String?): NewContactStorageMode =
            entries.firstOrNull { it.id == id } ?: GOOGLE_ACCOUNT
    }
}

enum class VoiceChangerSettingsMode(val id: String, val label: String) {
    LIVE_DSP_AND_AI_SYNC("live_dsp_ai", "Live DSP + AI Sync"),
    LIVE_DSP_ONLY("live_dsp", "Live Voice Only"),
    AI_PERSONA_ONLY("ai_only", "AI Persona Only"),
    OFF("off", "Off");

    companion object {
        fun fromId(id: String?): VoiceChangerSettingsMode =
            entries.firstOrNull { it.id == id } ?: LIVE_DSP_AND_AI_SYNC
    }
}

enum class VoiceChangerPreset(
    val id: String,
    val label: String,
    val shortLabel: String,
    val pitchFactor: Float,
    val ttsPitch: Float,
    val ttsRate: Float
) {
    NORMAL("normal", "Natural Voice", "NORMAL", 1.00f, 1.00f, 0.98f),
    DEEP_MALE("deep_male", "Deep / Male", "DEEP", 0.78f, 0.76f, 0.93f),
    BRIGHT_FEMALE("bright_female", "Bright / Female", "FEMALE", 1.24f, 1.28f, 1.02f),
    ROBOT_CYBER("robot_cyber", "Robot / Cyber", "ROBOT", 1.00f, 0.84f, 0.88f),
    STUDIO_WARM("studio_warm", "Studio / Warm", "STUDIO", 0.92f, 0.92f, 0.97f),
    RADIO_LOFI("radio_lofi", "Radio / Lo-Fi", "RADIO", 1.05f, 1.06f, 1.05f);

    companion object {
        fun fromId(id: String?): VoiceChangerPreset =
            entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

data class DialerSettings(
    val themePreset: DialerThemePreset = DialerThemePreset.NOTHING_DARK,
    val autoDayNightThemeEnabled: Boolean = false,
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
    val uiVibrationEnabled: Boolean = true,
    val dialpadSoundStyle: DialpadSoundStyle = DialpadSoundStyle.CLASSIC_DTMF,
    val preferredFps: Int = 90,
    val adaptiveRefreshRate: Boolean = true,
    val ultraSmoothMode: Boolean = false,
    val showContactQuickActions: Boolean = true,
    val uiBackgroundMode: UiBackgroundMode = UiBackgroundMode.DOT_MATRIX,
    val dotMatrixEnabled: Boolean = true,
    val dotMatrixBgHex: Long = 0L,
    val colorPlateStyle: ColorPlateStyle = ColorPlateStyle.GRADIENT,
    val solidBgHex: Long = 0xFF000000L,
    val gradientStartHex: Long = 0xFF23060BL,
    val gradientEndHex: Long = 0xFF050508L,
    val wallpaperPath: String? = null,
    val wallpaperDimStrength: Float = 0.45f,
    val audioOutputMode: AudioOutputMode = AudioOutputMode.AUTO_BT_EAR,
    val autoRecordMode: AutoRecordMode = AutoRecordMode.OFF,
    val silentCallRecording: Boolean = true,
    val autoSummarizeEnabled: Boolean = false,
    val voiceCommandControlEnabled: Boolean = true,
    val activeVibrationPatternId: Long = 1L,
    val defaultBackdropMode: CallerBackdropMode = CallerBackdropMode.NONE,
    val defaultVideoBackdropUri: String? = null,
    val defaultPhotoBackdropUri: String? = null,
    val defaultCallingCardUri: String? = null,
    val customRingtoneUri: String? = null,
    val videoRedTintStrength: Float = 0.58f,
    val vaultPin: String = "",
    val syncedGoogleAccounts: Set<String> = emptySet(),
    val googleSyncEnabled: Boolean = true,
    val aiAssistantEnabled: Boolean = false,
    val aiAssistantTriggerMode: AiAssistantTriggerMode = AiAssistantTriggerMode.BUSY_MODE_ONLY,
    val aiAssistantDelaySeconds: Int = 8,
    val aiBusyModeEnabled: Boolean = false,
    val aiAssistantLanguageMode: AiAssistantLanguageMode = AiAssistantLanguageMode.AUTO,
    val aiAssistantOwnerName: String = "",
    val aiAssistantAutoRecord: Boolean = true,
    val driveConnectedEmail: String = "",
    val lastDriveBackupAtMs: Long = 0L,
    val signedInGoogleEmail: String = "",
    val signedInGoogleName: String = "",
    val signedInGooglePhotoUri: String = "",
    val hasPromptedGoogleSignIn: Boolean = false,
    val newContactStorageMode: NewContactStorageMode = NewContactStorageMode.GOOGLE_ACCOUNT,
    val preferredGoogleContactAccount: String = "",
    val voiceChangerMode: VoiceChangerSettingsMode = VoiceChangerSettingsMode.LIVE_DSP_AND_AI_SYNC,
    val voiceChangerPreset: VoiceChangerPreset = VoiceChangerPreset.NORMAL
)

class DialerPreferences(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val AUTO_DAY_NIGHT_THEME = booleanPreferencesKey("auto_day_night_theme")
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
        val UI_VIBRATION = booleanPreferencesKey("ui_vibration")
        val DIALPAD_SOUND_STYLE = stringPreferencesKey("dialpad_sound_style")
        val PREFERRED_FPS = intPreferencesKey("preferred_fps")
        val ADAPTIVE_REFRESH_RATE = booleanPreferencesKey("adaptive_refresh_rate")
        val ULTRA_SMOOTH_MODE = booleanPreferencesKey("ultra_smooth_mode")
        val SHOW_CONTACT_QUICK_ACTIONS = booleanPreferencesKey("show_contact_quick_actions")
        val UI_BACKGROUND_MODE = stringPreferencesKey("ui_background_mode")
        val DOT_MATRIX_ENABLED = booleanPreferencesKey("dot_matrix_enabled")
        val DOT_MATRIX_BG_HEX = longPreferencesKey("dot_matrix_bg_hex")
        val COLOR_PLATE_STYLE = stringPreferencesKey("color_plate_style")
        val SOLID_BG_HEX = longPreferencesKey("solid_bg_hex")
        val GRADIENT_START_HEX = longPreferencesKey("gradient_start_hex")
        val GRADIENT_END_HEX = longPreferencesKey("gradient_end_hex")
        val WALLPAPER_PATH = stringPreferencesKey("wallpaper_path")
        val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
        val AUDIO_OUTPUT_MODE = stringPreferencesKey("audio_output_mode")
        val AUTO_RECORD_MODE = stringPreferencesKey("auto_record_mode")
        val SILENT_RECORDING = booleanPreferencesKey("silent_recording")
        val AUTO_SUMMARIZE = booleanPreferencesKey("auto_summarize")
        val VOICE_CONTROL = booleanPreferencesKey("voice_control")
        val ACTIVE_VIBRATION_ID = longPreferencesKey("active_vibration_id")
        val DEFAULT_BACKDROP_MODE = stringPreferencesKey("default_backdrop_mode")
        val DEFAULT_VIDEO_URI = stringPreferencesKey("default_video_uri")
        val DEFAULT_PHOTO_URI = stringPreferencesKey("default_photo_uri")
        val DEFAULT_CALLING_CARD_URI = stringPreferencesKey("default_calling_card_uri")
        val CUSTOM_RINGTONE_URI = stringPreferencesKey("custom_ringtone_uri")
        val VIDEO_RED_TINT = floatPreferencesKey("video_red_tint")
        val VAULT_PIN = stringPreferencesKey("vault_pin")
        val SYNCED_GOOGLE_ACCOUNTS = stringSetPreferencesKey("synced_google_accounts")
        val GOOGLE_SYNC_ENABLED = booleanPreferencesKey("google_sync_enabled")
        val AI_ASSISTANT_ENABLED = booleanPreferencesKey("ai_assistant_enabled")
        val AI_ASSISTANT_TRIGGER_MODE = stringPreferencesKey("ai_assistant_trigger_mode")
        val AI_ASSISTANT_DELAY_SECONDS = intPreferencesKey("ai_assistant_delay_seconds")
        val AI_BUSY_MODE_ENABLED = booleanPreferencesKey("ai_busy_mode_enabled")
        val AI_ASSISTANT_LANGUAGE_MODE = stringPreferencesKey("ai_assistant_language_mode")
        val AI_ASSISTANT_OWNER_NAME = stringPreferencesKey("ai_assistant_owner_name")
        val AI_ASSISTANT_AUTO_RECORD = booleanPreferencesKey("ai_assistant_auto_record")
        val DRIVE_CONNECTED_EMAIL = stringPreferencesKey("drive_connected_email")
        val LAST_DRIVE_BACKUP_AT_MS = longPreferencesKey("last_drive_backup_at_ms")
        val SIGNED_IN_GOOGLE_EMAIL = stringPreferencesKey("signed_in_google_email")
        val SIGNED_IN_GOOGLE_NAME = stringPreferencesKey("signed_in_google_name")
        val SIGNED_IN_GOOGLE_PHOTO_URI = stringPreferencesKey("signed_in_google_photo_uri")
        val HAS_PROMPTED_GOOGLE_SIGN_IN = booleanPreferencesKey("has_prompted_google_sign_in")
        val NEW_CONTACT_STORAGE_MODE = stringPreferencesKey("new_contact_storage_mode")
        val PREFERRED_GOOGLE_CONTACT_ACCOUNT = stringPreferencesKey("preferred_google_contact_account")
        val VOICE_CHANGER_MODE = stringPreferencesKey("voice_changer_mode")
        val VOICE_CHANGER_PRESET = stringPreferencesKey("voice_changer_preset")
    }

    val settingsFlow: Flow<DialerSettings> = context.dialerDataStore.data.map { prefs ->
        val rawVideo = prefs[Keys.DEFAULT_VIDEO_URI]?.takeIf { it.isNotBlank() }
        val rawMode = prefs[Keys.DEFAULT_BACKDROP_MODE]
        val resolvedBackdropMode = when {
            rawMode != null -> CallerBackdropMode.fromId(rawMode)
            rawVideo != null -> CallerBackdropMode.VIDEO
            else -> CallerBackdropMode.NONE
        }
        DialerSettings(
            themePreset = DialerThemePreset.fromId(prefs[Keys.THEME_PRESET]),
            autoDayNightThemeEnabled = prefs[Keys.AUTO_DAY_NIGHT_THEME] ?: false,
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
            uiVibrationEnabled = prefs[Keys.UI_VIBRATION] ?: true,
            dialpadSoundStyle = DialpadSoundStyle.fromId(prefs[Keys.DIALPAD_SOUND_STYLE]),
            preferredFps = prefs[Keys.PREFERRED_FPS] ?: 90,
            adaptiveRefreshRate = prefs[Keys.ADAPTIVE_REFRESH_RATE] ?: true,
            ultraSmoothMode = prefs[Keys.ULTRA_SMOOTH_MODE] ?: false,
            showContactQuickActions = prefs[Keys.SHOW_CONTACT_QUICK_ACTIONS] ?: true,
            uiBackgroundMode = UiBackgroundMode.fromId(prefs[Keys.UI_BACKGROUND_MODE]),
            dotMatrixEnabled = prefs[Keys.DOT_MATRIX_ENABLED] ?: true,
            dotMatrixBgHex = prefs[Keys.DOT_MATRIX_BG_HEX] ?: 0L,
            colorPlateStyle = ColorPlateStyle.fromId(prefs[Keys.COLOR_PLATE_STYLE]),
            solidBgHex = prefs[Keys.SOLID_BG_HEX] ?: 0xFF000000L,
            gradientStartHex = prefs[Keys.GRADIENT_START_HEX] ?: 0xFF23060BL,
            gradientEndHex = prefs[Keys.GRADIENT_END_HEX] ?: 0xFF050508L,
            wallpaperPath = prefs[Keys.WALLPAPER_PATH]?.takeIf { it.isNotBlank() },
            wallpaperDimStrength = prefs[Keys.WALLPAPER_DIM] ?: 0.45f,
            audioOutputMode = AudioOutputMode.fromId(prefs[Keys.AUDIO_OUTPUT_MODE]),
            autoRecordMode = AutoRecordMode.fromId(prefs[Keys.AUTO_RECORD_MODE]),
            silentCallRecording = prefs[Keys.SILENT_RECORDING] ?: true,
            autoSummarizeEnabled = prefs[Keys.AUTO_SUMMARIZE] ?: false,
            voiceCommandControlEnabled = prefs[Keys.VOICE_CONTROL] ?: true,
            activeVibrationPatternId = prefs[Keys.ACTIVE_VIBRATION_ID] ?: 1L,
            defaultBackdropMode = resolvedBackdropMode,
            defaultVideoBackdropUri = rawVideo,
            defaultPhotoBackdropUri = prefs[Keys.DEFAULT_PHOTO_URI]?.takeIf { it.isNotBlank() },
            defaultCallingCardUri = prefs[Keys.DEFAULT_CALLING_CARD_URI]?.takeIf { it.isNotBlank() },
            customRingtoneUri = prefs[Keys.CUSTOM_RINGTONE_URI]?.takeIf { it.isNotBlank() },
            videoRedTintStrength = prefs[Keys.VIDEO_RED_TINT] ?: 0.58f,
            vaultPin = prefs[Keys.VAULT_PIN] ?: "",
            syncedGoogleAccounts = prefs[Keys.SYNCED_GOOGLE_ACCOUNTS] ?: emptySet(),
            googleSyncEnabled = prefs[Keys.GOOGLE_SYNC_ENABLED] ?: true,
            aiAssistantEnabled = prefs[Keys.AI_ASSISTANT_ENABLED] ?: false,
            aiAssistantTriggerMode = AiAssistantTriggerMode.fromId(prefs[Keys.AI_ASSISTANT_TRIGGER_MODE]),
            aiAssistantDelaySeconds = (prefs[Keys.AI_ASSISTANT_DELAY_SECONDS] ?: 8).coerceIn(2, 20),
            aiBusyModeEnabled = prefs[Keys.AI_BUSY_MODE_ENABLED] ?: false,
            aiAssistantLanguageMode = AiAssistantLanguageMode.fromId(prefs[Keys.AI_ASSISTANT_LANGUAGE_MODE]),
            aiAssistantOwnerName = prefs[Keys.AI_ASSISTANT_OWNER_NAME] ?: "",
            aiAssistantAutoRecord = prefs[Keys.AI_ASSISTANT_AUTO_RECORD] ?: true,
            driveConnectedEmail = prefs[Keys.DRIVE_CONNECTED_EMAIL] ?: "",
            lastDriveBackupAtMs = prefs[Keys.LAST_DRIVE_BACKUP_AT_MS] ?: 0L,
            signedInGoogleEmail = prefs[Keys.SIGNED_IN_GOOGLE_EMAIL] ?: "",
            signedInGoogleName = prefs[Keys.SIGNED_IN_GOOGLE_NAME] ?: "",
            signedInGooglePhotoUri = prefs[Keys.SIGNED_IN_GOOGLE_PHOTO_URI] ?: "",
            hasPromptedGoogleSignIn = prefs[Keys.HAS_PROMPTED_GOOGLE_SIGN_IN] ?: false,
            newContactStorageMode = NewContactStorageMode.fromId(prefs[Keys.NEW_CONTACT_STORAGE_MODE]),
            preferredGoogleContactAccount = prefs[Keys.PREFERRED_GOOGLE_CONTACT_ACCOUNT] ?: "",
            voiceChangerMode = VoiceChangerSettingsMode.fromId(prefs[Keys.VOICE_CHANGER_MODE]),
            voiceChangerPreset = VoiceChangerPreset.fromId(prefs[Keys.VOICE_CHANGER_PRESET])
        )
    }

    suspend fun setThemePreset(preset: DialerThemePreset) {
        context.dialerDataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }

    suspend fun setAutoDayNightThemeEnabled(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.AUTO_DAY_NIGHT_THEME] = enabled }
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

    suspend fun setUiVibrationEnabled(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.UI_VIBRATION] = enabled }
    }

    suspend fun setDialpadSoundStyle(style: DialpadSoundStyle) {
        context.dialerDataStore.edit { it[Keys.DIALPAD_SOUND_STYLE] = style.id }
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

    suspend fun setShowContactQuickActions(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.SHOW_CONTACT_QUICK_ACTIONS] = enabled }
    }

    suspend fun setUiBackgroundMode(mode: UiBackgroundMode) {
        context.dialerDataStore.edit { it[Keys.UI_BACKGROUND_MODE] = mode.id }
    }

    suspend fun setDotMatrixEnabled(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.DOT_MATRIX_ENABLED] = enabled }
    }

    suspend fun setDotMatrixBgHex(hex: Long) {
        context.dialerDataStore.edit {
            it[Keys.DOT_MATRIX_BG_HEX] = hex
            it[Keys.UI_BACKGROUND_MODE] = UiBackgroundMode.DOT_MATRIX.id
        }
    }

    suspend fun setColorPlateStyle(style: ColorPlateStyle) {
        context.dialerDataStore.edit {
            it[Keys.COLOR_PLATE_STYLE] = style.id
            it[Keys.UI_BACKGROUND_MODE] = UiBackgroundMode.COLOR_PLATE.id
        }
    }

    suspend fun setSolidBgHex(hex: Long) {
        context.dialerDataStore.edit {
            it[Keys.SOLID_BG_HEX] = hex
            it[Keys.COLOR_PLATE_STYLE] = ColorPlateStyle.SOLID.id
            it[Keys.UI_BACKGROUND_MODE] = UiBackgroundMode.COLOR_PLATE.id
        }
    }

    suspend fun setGradientColors(startHex: Long, endHex: Long) {
        context.dialerDataStore.edit {
            it[Keys.GRADIENT_START_HEX] = startHex
            it[Keys.GRADIENT_END_HEX] = endHex
            it[Keys.COLOR_PLATE_STYLE] = ColorPlateStyle.GRADIENT.id
            it[Keys.UI_BACKGROUND_MODE] = UiBackgroundMode.COLOR_PLATE.id
        }
    }

    suspend fun setWallpaperPath(path: String?) {
        context.dialerDataStore.edit {
            if (path.isNullOrBlank()) {
                it.remove(Keys.WALLPAPER_PATH)
                if (it[Keys.UI_BACKGROUND_MODE] == UiBackgroundMode.WALLPAPER.id) {
                    it[Keys.UI_BACKGROUND_MODE] = UiBackgroundMode.DOT_MATRIX.id
                }
            } else {
                it[Keys.WALLPAPER_PATH] = path
                it[Keys.UI_BACKGROUND_MODE] = UiBackgroundMode.WALLPAPER.id
            }
        }
    }

    suspend fun setWallpaperDimStrength(dim: Float) {
        context.dialerDataStore.edit { it[Keys.WALLPAPER_DIM] = dim.coerceIn(0.05f, 0.85f) }
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

    suspend fun setDefaultBackdropMode(mode: CallerBackdropMode) {
        context.dialerDataStore.edit { it[Keys.DEFAULT_BACKDROP_MODE] = mode.id }
    }

    suspend fun setDefaultVideoBackdropUri(uri: String?) {
        context.dialerDataStore.edit {
            if (uri.isNullOrBlank()) {
                it.remove(Keys.DEFAULT_VIDEO_URI)
                if (it[Keys.DEFAULT_BACKDROP_MODE] == CallerBackdropMode.VIDEO.id) {
                    it[Keys.DEFAULT_BACKDROP_MODE] = CallerBackdropMode.NONE.id
                }
            } else {
                it[Keys.DEFAULT_VIDEO_URI] = uri
                it[Keys.DEFAULT_BACKDROP_MODE] = CallerBackdropMode.VIDEO.id
            }
        }
    }

    suspend fun setDefaultPhotoBackdropUri(uri: String?) {
        context.dialerDataStore.edit {
            if (uri.isNullOrBlank()) {
                it.remove(Keys.DEFAULT_PHOTO_URI)
                if (it[Keys.DEFAULT_BACKDROP_MODE] == CallerBackdropMode.PHOTO.id) {
                    it[Keys.DEFAULT_BACKDROP_MODE] = CallerBackdropMode.NONE.id
                }
            } else {
                it[Keys.DEFAULT_PHOTO_URI] = uri
                it[Keys.DEFAULT_BACKDROP_MODE] = CallerBackdropMode.PHOTO.id
            }
        }
    }

    suspend fun setDefaultCallingCardUri(uri: String?) {
        context.dialerDataStore.edit {
            if (uri.isNullOrBlank()) {
                it.remove(Keys.DEFAULT_CALLING_CARD_URI)
                if (it[Keys.DEFAULT_BACKDROP_MODE] == CallerBackdropMode.CALLING_CARD.id) {
                    it[Keys.DEFAULT_BACKDROP_MODE] = CallerBackdropMode.NONE.id
                }
            } else {
                it[Keys.DEFAULT_CALLING_CARD_URI] = uri
                it[Keys.DEFAULT_BACKDROP_MODE] = CallerBackdropMode.CALLING_CARD.id
            }
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

    suspend fun setAiAssistantEnabled(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.AI_ASSISTANT_ENABLED] = enabled }
    }

    suspend fun setAiAssistantTriggerMode(mode: AiAssistantTriggerMode) {
        context.dialerDataStore.edit { it[Keys.AI_ASSISTANT_TRIGGER_MODE] = mode.id }
    }

    suspend fun setAiAssistantDelaySeconds(seconds: Int) {
        context.dialerDataStore.edit { it[Keys.AI_ASSISTANT_DELAY_SECONDS] = seconds.coerceIn(2, 20) }
    }

    suspend fun setAiBusyModeEnabled(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.AI_BUSY_MODE_ENABLED] = enabled }
    }

    suspend fun setAiAssistantLanguageMode(mode: AiAssistantLanguageMode) {
        context.dialerDataStore.edit { it[Keys.AI_ASSISTANT_LANGUAGE_MODE] = mode.id }
    }

    suspend fun setAiAssistantOwnerName(name: String) {
        context.dialerDataStore.edit { it[Keys.AI_ASSISTANT_OWNER_NAME] = name.trim() }
    }

    suspend fun setAiAssistantAutoRecord(enabled: Boolean) {
        context.dialerDataStore.edit { it[Keys.AI_ASSISTANT_AUTO_RECORD] = enabled }
    }

    suspend fun setDriveConnectionInfo(email: String, lastBackupAtMs: Long? = null) {
        context.dialerDataStore.edit {
            it[Keys.DRIVE_CONNECTED_EMAIL] = email.trim()
            if (lastBackupAtMs != null) {
                it[Keys.LAST_DRIVE_BACKUP_AT_MS] = lastBackupAtMs
            }
        }
    }

    suspend fun setSignedInGoogleProfile(email: String, displayName: String, photoUri: String) {
        context.dialerDataStore.edit {
            it[Keys.SIGNED_IN_GOOGLE_EMAIL] = email.trim()
            it[Keys.SIGNED_IN_GOOGLE_NAME] = displayName.trim()
            it[Keys.SIGNED_IN_GOOGLE_PHOTO_URI] = photoUri.trim()
            if (email.isNotBlank()) {
                it[Keys.HAS_PROMPTED_GOOGLE_SIGN_IN] = true
            }
        }
    }

    suspend fun setHasPromptedGoogleSignIn(prompted: Boolean) {
        context.dialerDataStore.edit { it[Keys.HAS_PROMPTED_GOOGLE_SIGN_IN] = prompted }
    }

    suspend fun setNewContactStorageMode(mode: NewContactStorageMode) {
        context.dialerDataStore.edit { it[Keys.NEW_CONTACT_STORAGE_MODE] = mode.id }
    }

    suspend fun setPreferredGoogleContactAccount(email: String) {
        context.dialerDataStore.edit { it[Keys.PREFERRED_GOOGLE_CONTACT_ACCOUNT] = email.trim() }
    }

    suspend fun setVoiceChangerMode(mode: VoiceChangerSettingsMode) {
        context.dialerDataStore.edit { it[Keys.VOICE_CHANGER_MODE] = mode.id }
    }

    suspend fun setVoiceChangerPreset(preset: VoiceChangerPreset) {
        context.dialerDataStore.edit { it[Keys.VOICE_CHANGER_PRESET] = preset.id }
    }

    suspend fun currentSettingsOnce(): DialerSettings = settingsFlow.first()

    fun encodeSettingsJson(s: DialerSettings): JSONObject = JSONObject().apply {
        put("themePreset", s.themePreset.id)
        put("autoDayNightThemeEnabled", s.autoDayNightThemeEnabled)
        put("amoledBlackEnabled", s.amoledBlackEnabled)
        put("customBgHex", s.customBgHex)
        put("customSurfaceHex", s.customSurfaceHex)
        put("customAccentHex", s.customAccentHex)
        put("customTextHex", s.customTextHex)
        put("fontPreset", s.fontPreset.id)
        put("fontStyleMode", s.fontStyleMode.id)
        put("fontScale", s.fontScale.toDouble())
        put("coolJazzGlowEnabled", s.coolJazzGlowEnabled)
        put("dotDensity", s.dotDensity.toDouble())
        put("uiSoundsEnabled", s.uiSoundsEnabled)
        put("uiVibrationEnabled", s.uiVibrationEnabled)
        put("dialpadSoundStyle", s.dialpadSoundStyle.id)
        put("preferredFps", s.preferredFps)
        put("adaptiveRefreshRate", s.adaptiveRefreshRate)
        put("ultraSmoothMode", s.ultraSmoothMode)
        put("showContactQuickActions", s.showContactQuickActions)
        put("uiBackgroundMode", s.uiBackgroundMode.id)
        put("dotMatrixEnabled", s.dotMatrixEnabled)
        put("dotMatrixBgHex", s.dotMatrixBgHex)
        put("colorPlateStyle", s.colorPlateStyle.id)
        put("solidBgHex", s.solidBgHex)
        put("gradientStartHex", s.gradientStartHex)
        put("gradientEndHex", s.gradientEndHex)
        put("wallpaperDimStrength", s.wallpaperDimStrength.toDouble())
        put("audioOutputMode", s.audioOutputMode.id)
        put("autoRecordMode", s.autoRecordMode.id)
        put("silentCallRecording", s.silentCallRecording)
        put("autoSummarizeEnabled", s.autoSummarizeEnabled)
        put("voiceCommandControlEnabled", s.voiceCommandControlEnabled)
        put("activeVibrationPatternId", s.activeVibrationPatternId)
        put("defaultBackdropMode", s.defaultBackdropMode.id)
        put("videoRedTintStrength", s.videoRedTintStrength.toDouble())
        put("googleSyncEnabled", s.googleSyncEnabled)
        put("aiAssistantEnabled", s.aiAssistantEnabled)
        put("aiAssistantTriggerMode", s.aiAssistantTriggerMode.id)
        put("aiAssistantDelaySeconds", s.aiAssistantDelaySeconds)
        put("aiBusyModeEnabled", s.aiBusyModeEnabled)
        put("aiAssistantLanguageMode", s.aiAssistantLanguageMode.id)
        put("aiAssistantOwnerName", s.aiAssistantOwnerName)
        put("aiAssistantAutoRecord", s.aiAssistantAutoRecord)
        put("newContactStorageMode", s.newContactStorageMode.id)
        put("preferredGoogleContactAccount", s.preferredGoogleContactAccount)
        put("voiceChangerMode", s.voiceChangerMode.id)
        put("voiceChangerPreset", s.voiceChangerPreset.id)
    }

    suspend fun applySettingsJson(obj: JSONObject): Int {
        var appliedCount = 0
        context.dialerDataStore.edit { prefs ->
            if (obj.has("themePreset")) {
                prefs[Keys.THEME_PRESET] = DialerThemePreset.fromId(obj.optString("themePreset")).id
                appliedCount++
            }
            if (obj.has("autoDayNightThemeEnabled")) {
                prefs[Keys.AUTO_DAY_NIGHT_THEME] = obj.optBoolean("autoDayNightThemeEnabled", false)
                appliedCount++
            }
            if (obj.has("defaultBackdropMode")) {
                prefs[Keys.DEFAULT_BACKDROP_MODE] = CallerBackdropMode.fromId(obj.optString("defaultBackdropMode")).id
                appliedCount++
            }
            if (obj.has("amoledBlackEnabled")) {
                prefs[Keys.AMOLED_BLACK] = obj.optBoolean("amoledBlackEnabled", false)
                appliedCount++
            }
            if (obj.has("customBgHex")) {
                prefs[Keys.CUSTOM_BG_HEX] = obj.optLong("customBgHex", 0xFF0A0A0EL)
                appliedCount++
            }
            if (obj.has("customSurfaceHex")) {
                prefs[Keys.CUSTOM_SURFACE_HEX] = obj.optLong("customSurfaceHex", 0xFF17171FL)
                appliedCount++
            }
            if (obj.has("customAccentHex")) {
                prefs[Keys.CUSTOM_ACCENT_HEX] = obj.optLong("customAccentHex", 0xFFD71921L)
                appliedCount++
            }
            if (obj.has("customTextHex")) {
                prefs[Keys.CUSTOM_TEXT_HEX] = obj.optLong("customTextHex", 0xFFF5F5F7L)
                appliedCount++
            }
            if (obj.has("fontPreset")) {
                prefs[Keys.FONT_PRESET] = DialerFontPreset.fromId(obj.optString("fontPreset")).id
                appliedCount++
            }
            if (obj.has("fontStyleMode")) {
                prefs[Keys.FONT_STYLE_MODE] = FontStyleMode.fromId(obj.optString("fontStyleMode")).id
                appliedCount++
            }
            if (obj.has("fontScale")) {
                prefs[Keys.FONT_SCALE] = obj.optDouble("fontScale", 1.0).toFloat().coerceIn(0.85f, 1.30f)
                appliedCount++
            }
            if (obj.has("coolJazzGlowEnabled")) {
                prefs[Keys.COOL_JAZZ_GLOW] = obj.optBoolean("coolJazzGlowEnabled", true)
                appliedCount++
            }
            if (obj.has("dotDensity")) {
                prefs[Keys.DOT_DENSITY] = obj.optDouble("dotDensity", 0.65).toFloat().coerceIn(0.2f, 1.0f)
                appliedCount++
            }
            if (obj.has("uiSoundsEnabled")) {
                prefs[Keys.UI_SOUNDS] = obj.optBoolean("uiSoundsEnabled", true)
                appliedCount++
            }
            if (obj.has("uiVibrationEnabled")) {
                prefs[Keys.UI_VIBRATION] = obj.optBoolean("uiVibrationEnabled", true)
                appliedCount++
            }
            if (obj.has("dialpadSoundStyle")) {
                prefs[Keys.DIALPAD_SOUND_STYLE] = DialpadSoundStyle.fromId(obj.optString("dialpadSoundStyle")).id
                appliedCount++
            }
            if (obj.has("preferredFps")) {
                val fps = obj.optInt("preferredFps", 90)
                prefs[Keys.PREFERRED_FPS] = if (fps == 0) 0 else fps.coerceIn(60, 120)
                appliedCount++
            }
            if (obj.has("adaptiveRefreshRate")) {
                prefs[Keys.ADAPTIVE_REFRESH_RATE] = obj.optBoolean("adaptiveRefreshRate", true)
                appliedCount++
            }
            if (obj.has("ultraSmoothMode")) {
                prefs[Keys.ULTRA_SMOOTH_MODE] = obj.optBoolean("ultraSmoothMode", false)
                appliedCount++
            }
            if (obj.has("showContactQuickActions")) {
                prefs[Keys.SHOW_CONTACT_QUICK_ACTIONS] = obj.optBoolean("showContactQuickActions", true)
                appliedCount++
            }
            if (obj.has("uiBackgroundMode")) {
                prefs[Keys.UI_BACKGROUND_MODE] = UiBackgroundMode.fromId(obj.optString("uiBackgroundMode")).id
                appliedCount++
            }
            if (obj.has("dotMatrixEnabled")) {
                prefs[Keys.DOT_MATRIX_ENABLED] = obj.optBoolean("dotMatrixEnabled", true)
                appliedCount++
            }
            if (obj.has("dotMatrixBgHex")) {
                prefs[Keys.DOT_MATRIX_BG_HEX] = obj.optLong("dotMatrixBgHex", 0L)
                appliedCount++
            }
            if (obj.has("colorPlateStyle")) {
                prefs[Keys.COLOR_PLATE_STYLE] = ColorPlateStyle.fromId(obj.optString("colorPlateStyle")).id
                appliedCount++
            }
            if (obj.has("solidBgHex")) {
                prefs[Keys.SOLID_BG_HEX] = obj.optLong("solidBgHex", 0xFF000000L)
                appliedCount++
            }
            if (obj.has("gradientStartHex")) {
                prefs[Keys.GRADIENT_START_HEX] = obj.optLong("gradientStartHex", 0xFF23060BL)
                appliedCount++
            }
            if (obj.has("gradientEndHex")) {
                prefs[Keys.GRADIENT_END_HEX] = obj.optLong("gradientEndHex", 0xFF050508L)
                appliedCount++
            }
            if (obj.has("wallpaperDimStrength")) {
                prefs[Keys.WALLPAPER_DIM] = obj.optDouble("wallpaperDimStrength", 0.45).toFloat().coerceIn(0.05f, 0.85f)
                appliedCount++
            }
            if (obj.has("audioOutputMode")) {
                prefs[Keys.AUDIO_OUTPUT_MODE] = AudioOutputMode.fromId(obj.optString("audioOutputMode")).id
                appliedCount++
            }
            if (obj.has("autoRecordMode")) {
                prefs[Keys.AUTO_RECORD_MODE] = AutoRecordMode.fromId(obj.optString("autoRecordMode")).id
                appliedCount++
            }
            if (obj.has("silentCallRecording")) {
                prefs[Keys.SILENT_RECORDING] = obj.optBoolean("silentCallRecording", true)
                appliedCount++
            }
            if (obj.has("autoSummarizeEnabled")) {
                prefs[Keys.AUTO_SUMMARIZE] = obj.optBoolean("autoSummarizeEnabled", false)
                appliedCount++
            }
            if (obj.has("voiceCommandControlEnabled")) {
                prefs[Keys.VOICE_CONTROL] = obj.optBoolean("voiceCommandControlEnabled", true)
                appliedCount++
            }
            if (obj.has("activeVibrationPatternId")) {
                prefs[Keys.ACTIVE_VIBRATION_ID] = obj.optLong("activeVibrationPatternId", 1L)
                appliedCount++
            }
            if (obj.has("videoRedTintStrength")) {
                prefs[Keys.VIDEO_RED_TINT] = obj.optDouble("videoRedTintStrength", 0.58).toFloat().coerceIn(0f, 0.9f)
                appliedCount++
            }
            if (obj.has("googleSyncEnabled")) {
                prefs[Keys.GOOGLE_SYNC_ENABLED] = obj.optBoolean("googleSyncEnabled", true)
                appliedCount++
            }
            if (obj.has("aiAssistantEnabled")) {
                prefs[Keys.AI_ASSISTANT_ENABLED] = obj.optBoolean("aiAssistantEnabled", false)
                appliedCount++
            }
            if (obj.has("aiAssistantTriggerMode")) {
                prefs[Keys.AI_ASSISTANT_TRIGGER_MODE] = AiAssistantTriggerMode.fromId(obj.optString("aiAssistantTriggerMode")).id
                appliedCount++
            }
            if (obj.has("aiAssistantDelaySeconds")) {
                prefs[Keys.AI_ASSISTANT_DELAY_SECONDS] = obj.optInt("aiAssistantDelaySeconds", 8).coerceIn(2, 20)
                appliedCount++
            }
            if (obj.has("aiBusyModeEnabled")) {
                prefs[Keys.AI_BUSY_MODE_ENABLED] = obj.optBoolean("aiBusyModeEnabled", false)
                appliedCount++
            }
            if (obj.has("aiAssistantLanguageMode")) {
                prefs[Keys.AI_ASSISTANT_LANGUAGE_MODE] = AiAssistantLanguageMode.fromId(obj.optString("aiAssistantLanguageMode")).id
                appliedCount++
            }
            if (obj.has("aiAssistantOwnerName")) {
                prefs[Keys.AI_ASSISTANT_OWNER_NAME] = obj.optString("aiAssistantOwnerName", "").trim()
                appliedCount++
            }
            if (obj.has("aiAssistantAutoRecord")) {
                prefs[Keys.AI_ASSISTANT_AUTO_RECORD] = obj.optBoolean("aiAssistantAutoRecord", true)
                appliedCount++
            }
            if (obj.has("newContactStorageMode")) {
                prefs[Keys.NEW_CONTACT_STORAGE_MODE] = NewContactStorageMode.fromId(obj.optString("newContactStorageMode")).id
                appliedCount++
            }
            if (obj.has("preferredGoogleContactAccount")) {
                prefs[Keys.PREFERRED_GOOGLE_CONTACT_ACCOUNT] = obj.optString("preferredGoogleContactAccount", "").trim()
                appliedCount++
            }
            if (obj.has("voiceChangerMode")) {
                prefs[Keys.VOICE_CHANGER_MODE] = VoiceChangerSettingsMode.fromId(obj.optString("voiceChangerMode")).id
                appliedCount++
            }
            if (obj.has("voiceChangerPreset")) {
                prefs[Keys.VOICE_CHANGER_PRESET] = VoiceChangerPreset.fromId(obj.optString("voiceChangerPreset")).id
                appliedCount++
            }
        }
        return appliedCount
    }
}
