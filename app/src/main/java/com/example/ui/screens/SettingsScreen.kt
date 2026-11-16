package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ai.AssistantReplyLanguage
import com.example.data.local.ContactEntity
import com.example.data.local.VibrationPatternEntity
import com.example.data.preferences.AiAssistantLanguageMode
import com.example.data.preferences.AiAssistantTriggerMode
import com.example.data.preferences.AudioOutputMode
import com.example.data.preferences.AutoRecordMode
import com.example.data.preferences.CallerBackdropMode
import com.example.data.preferences.ColorPlateStyle
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.DialpadSoundStyle
import com.example.data.preferences.NewContactStorageMode
import com.example.data.preferences.UiBackgroundMode
import com.example.data.preferences.VoiceChangerPreset
import com.example.data.preferences.VoiceChangerSettingsMode
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.DialerFontPreset
import com.example.ui.theme.DialerThemePreset
import com.example.ui.theme.FontStyleMode
import com.example.viewmodel.GoogleAuthUiState
import com.example.viewmodel.GoogleDriveUiState

@Composable
fun SettingsScreen(
    settings: DialerSettings,
    supportedRefreshRates: List<Int>,
    vibrationPatterns: List<VibrationPatternEntity>,
    onSelectTheme: (DialerThemePreset) -> Unit,
    onToggleAutoDayNight: (Boolean) -> Unit = {},
    onToggleAmoledBlack: (Boolean) -> Unit,
    onUpdateCustomColors: (Long, Long, Long, Long) -> Unit,
    onDotDensityChange: (Float) -> Unit,
    onSelectFontPreset: (DialerFontPreset) -> Unit,
    onSelectFontStyle: (FontStyleMode) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onToggleCoolJazzGlow: (Boolean) -> Unit,
    onImportCustomFontUri: (Uri) -> Unit,
    onSetPreferredFps: (Int) -> Unit,
    onSetAdaptiveRefreshRate: (Boolean) -> Unit,
    onSetUltraSmoothMode: (Boolean) -> Unit,
    onSetUiSounds: (Boolean) -> Unit,
    onSetUiVibration: (Boolean) -> Unit = {},
    onSetDialpadSoundStyle: (DialpadSoundStyle) -> Unit,
    onSetAudioOutputMode: (AudioOutputMode) -> Unit,
    onPickCustomRingtone: (Uri?) -> Unit,
    onSelectDefaultBackdropMode: (CallerBackdropMode) -> Unit = {},
    onPickDefaultVideo: (Uri?) -> Unit,
    onPickDefaultPhoto: (Uri?) -> Unit = {},
    onPickDefaultCallingCard: (Uri?) -> Unit = {},
    contacts: List<ContactEntity> = emptyList(),
    onAssignContactCallingCard: (ContactEntity, Uri?) -> Unit = { _, _ -> },
    onRedTintStrengthChange: (Float) -> Unit,
    onSelectVibrationPattern: (Long) -> Unit,
    onTestVibrationPattern: (VibrationPatternEntity) -> Unit,
    onSaveCustomVibration: (String, List<Long>) -> Unit,
    onDeleteCustomVibration: (Long) -> Unit,
    onSetAutoRecordMode: (AutoRecordMode) -> Unit,
    onSetSilentRecording: (Boolean) -> Unit,
    onSetAutoSummarize: (Boolean) -> Unit,
    onSetVoiceControl: (Boolean) -> Unit,
    onSetShowContactQuickActions: (Boolean) -> Unit,
    onSelectBackgroundMode: (UiBackgroundMode) -> Unit,
    onToggleDotMatrix: (Boolean) -> Unit,
    onSelectDotMatrixBgHex: (Long) -> Unit,
    onSelectColorPlateStyle: (ColorPlateStyle) -> Unit,
    onSelectSolidBgHex: (Long) -> Unit,
    onSelectGradientColors: (Long, Long) -> Unit,
    onPickWallpaper: (Uri?) -> Unit,
    onWallpaperDimChange: (Float) -> Unit,
    onSetVaultPin: (String) -> Unit,
    onExportBackupJson: (Uri) -> Unit,
    onExportSettingsJson: (Uri) -> Unit,
    onExportContactsVcf: (Uri) -> Unit,
    onRestoreBackupJson: (Uri) -> Unit,
    onRestoreSettingsJson: (Uri) -> Unit,
    driveUiState: GoogleDriveUiState,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onBackupToDrive: (Boolean, Boolean) -> Unit,
    onRefreshDriveBackups: () -> Unit,
    onRestoreFromDrive: (String, Boolean, Boolean) -> Unit,
    onSetAiAssistantEnabled: (Boolean) -> Unit,
    onSetAiBusyMode: (Boolean) -> Unit,
    onSetAiOwnerName: (String) -> Unit,
    onSetAiTriggerMode: (AiAssistantTriggerMode) -> Unit,
    onSetAiDelaySeconds: (Int) -> Unit,
    onSetAiLanguageMode: (AiAssistantLanguageMode) -> Unit,
    onSetAiAutoRecord: (Boolean) -> Unit,
    onPreviewAiVoice: (AssistantReplyLanguage) -> Unit,
    onPinContactWidget: () -> Unit,
    googleAuthState: GoogleAuthUiState = GoogleAuthUiState(),
    onGoogleSignInOrSwitch: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    availableGoogleAccounts: List<String> = emptyList(),
    resolvedTargetContactAccount: String = "",
    onSetNewContactStorageMode: (NewContactStorageMode) -> Unit = {},
    onSelectPreferredGoogleContactAccount: (String) -> Unit = {},
    onSetVoiceChangerMode: (VoiceChangerSettingsMode) -> Unit = {},
    onSelectVoiceChangerPreset: (VoiceChangerPreset) -> Unit = {},
    onPreviewVoiceChangerPreset: (VoiceChangerPreset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val defaultVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickDefaultVideo(uri)
    }

    val defaultPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickDefaultPhoto(uri)
    }

    var showCallingCardContactPicker by remember { mutableStateOf(false) }
    var pendingCallingCardContact by remember { mutableStateOf<ContactEntity?>(null) }

    val contactCallingCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingCallingCardContact
        pendingCallingCardContact = null
        if (uri != null && target != null) {
            onAssignContactCallingCard(target, uri)
        }
    }

    var customPatternName by remember { mutableStateOf("") }
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    val recordedTimings = remember { mutableStateListOf<Long>() }
    var pinInput by remember(settings.vaultPin) { mutableStateOf(settings.vaultPin) }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        item(key = "settings_header", contentType = "header") {
            SectionHeaderRow(
                title = "SETTINGS & NOTHING STUDIO",
                subtitle = "Google profile, voice FX, themes, AI assistant & backups"
            )
        }

        // Google Account Sign-In & Multi-User Switcher
        item(key = "google_identity", contentType = "settings_card") {
            GoogleAccountIdentityCard(
                settings = settings,
                authState = googleAuthState,
                onSignInOrSwitch = onGoogleSignInOrSwitch,
                onSignOut = onGoogleSignOut
            )
        }

        // Theme & AMOLED Studio
        item(key = "theme_appearance", contentType = "settings_card") {
            ThemeAppearanceSettingsCard(
                settings = settings,
                onSelectTheme = onSelectTheme,
                onToggleAutoDayNight = onToggleAutoDayNight,
                onToggleAmoledBlack = onToggleAmoledBlack,
                onUpdateCustomColors = onUpdateCustomColors,
                onDotDensityChange = onDotDensityChange
            )
        }

        // UI Background Studio (Dot Matrix / Color Plate / Wallpaper)
        item(key = "bg_studio", contentType = "settings_card") {
            UiBackgroundSettingsCard(
                settings = settings,
                onSelectBackgroundMode = onSelectBackgroundMode,
                onToggleDotMatrix = onToggleDotMatrix,
                onSelectDotMatrixBgHex = onSelectDotMatrixBgHex,
                onToggleAmoledBlack = onToggleAmoledBlack,
                onDotDensityChange = onDotDensityChange,
                onSelectColorPlateStyle = onSelectColorPlateStyle,
                onSelectSolidBgHex = onSelectSolidBgHex,
                onSelectGradientColors = onSelectGradientColors,
                onPickWallpaper = onPickWallpaper,
                onWallpaperDimChange = onWallpaperDimChange
            )
        }

        // Font & Cool Jazz Studio
        item(key = "typography_studio", contentType = "settings_card") {
            TypographySettingsCard(
                settings = settings,
                onSelectFontPreset = onSelectFontPreset,
                onSelectFontStyle = onSelectFontStyle,
                onFontScaleChange = onFontScaleChange,
                onToggleCoolJazzGlow = onToggleCoolJazzGlow,
                onImportCustomFontUri = onImportCustomFontUri
            )
        }

        // Display FPS, Sounds, Ringtone & Audio Output
        item(key = "display_audio", contentType = "settings_card") {
            DisplayAndAudioSettingsCard(
                settings = settings,
                supportedRefreshRates = supportedRefreshRates,
                onSetPreferredFps = onSetPreferredFps,
                onSetAdaptiveRefreshRate = onSetAdaptiveRefreshRate,
                onSetUltraSmoothMode = onSetUltraSmoothMode,
                onSetUiSounds = onSetUiSounds,
                onSetUiVibration = onSetUiVibration,
                onSetDialpadSoundStyle = onSetDialpadSoundStyle,
                onSetAudioOutputMode = onSetAudioOutputMode,
                onPickCustomRingtone = onPickCustomRingtone
            )
        }

        // Gemini AI Call Assistant (English, Hindi & Bengali)
        item(key = "ai_call_assistant", contentType = "settings_card") {
            AiCallAssistantSettingsCard(
                settings = settings,
                onSetAiAssistantEnabled = onSetAiAssistantEnabled,
                onSetAiBusyMode = onSetAiBusyMode,
                onSetAiOwnerName = onSetAiOwnerName,
                onSetAiTriggerMode = onSetAiTriggerMode,
                onSetAiDelaySeconds = onSetAiDelaySeconds,
                onSetAiLanguageMode = onSetAiLanguageMode,
                onSetAiAutoRecord = onSetAiAutoRecord,
                onPreviewAiVoice = onPreviewAiVoice
            )
        }

        // In-Call Voice Changer FX Studio
        item(key = "voice_changer_fx", contentType = "settings_card") {
            VoiceChangerSettingsCard(
                settings = settings,
                onSetMode = onSetVoiceChangerMode,
                onSelectPreset = onSelectVoiceChangerPreset,
                onPreviewPreset = onPreviewVoiceChangerPreset
            )
        }

        // Home Screen Contact Widget
        item(key = "contact_widget", contentType = "settings_card") {
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AddToHomeScreen,
                                contentDescription = "Home contact widget",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONTACT SPEED-DIAL WIDGET",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add a 2x2 Nothing OS Contact Widget with photos & 1-tap calling to your home screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onPinContactWidget,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("settings_pin_widget_button")
                    ) {
                        Text("PIN WIDGET", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Privacy, Auto Call Recording & Voice Settings
        item(key = "privacy_recording", contentType = "settings_card") {
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRIVACY, RECORDING & VOICE SETTINGS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    SettingsSwitchRow(
                        title = "Hands-Free Voice Answer / End",
                        subtitle = "Say Answer, Pick Up, End, Hang Up, or Mute without tapping mic",
                        checked = settings.voiceCommandControlEnabled,
                        onCheckedChange = onSetVoiceControl,
                        testTag = "voice_control_switch"
                    )

                    Text(
                        text = "AUTO CALL RECORDER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoRecordMode.entries.forEach { mode ->
                            DotMatrixTagChip(
                                label = mode.label,
                                active = settings.autoRecordMode == mode,
                                onClick = { onSetAutoRecordMode(mode) },
                                testTag = "auto_record_${mode.id}"
                            )
                        }
                    }

                    SettingsSwitchRow(
                        title = "Silent Call Recording",
                        subtitle = "Record silently without playing start tone or audio alert",
                        checked = settings.silentCallRecording,
                        onCheckedChange = onSetSilentRecording,
                        testTag = "silent_record_switch"
                    )

                    SettingsSwitchRow(
                        title = "Auto-Summarize on Call End",
                        subtitle = "When ON, silently saves Gemini summary to Vault (no popup screen)",
                        checked = settings.autoSummarizeEnabled,
                        onCheckedChange = onSetAutoSummarize,
                        testTag = "auto_summarize_switch"
                    )

                    SettingsSwitchRow(
                        title = "Contact Detail Quick Actions (SMS, WhatsApp, Gmail)",
                        subtitle = "Show or hide SMS, WhatsApp & Gmail buttons on contact cards",
                        checked = settings.showContactQuickActions,
                        onCheckedChange = onSetShowContactQuickActions,
                        testTag = "contact_quick_actions_switch"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(4) },
                            label = {
                                Text(
                                    "Vault PIN (4 digits, blank = off)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onSetVaultPin(pinInput) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("SET PIN", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // New Contact Save Destination (Google Account vs Device Storage)
        item(key = "new_contact_storage", contentType = "settings_card") {
            NewContactStorageSettingsCard(
                settings = settings,
                availableGoogleAccounts = availableGoogleAccounts,
                resolvedTargetAccount = resolvedTargetContactAccount,
                onSetStorageMode = onSetNewContactStorageMode,
                onSelectPreferredGoogleAccount = onSelectPreferredGoogleContactAccount
            )
        }

        // Google Drive Direct Backup, Restore & App Settings Export
        item(key = "backup_restore", contentType = "settings_card") {
            GoogleDriveAndBackupSettingsCard(
                settings = settings,
                driveUiState = driveUiState,
                onConnectDrive = onConnectDrive,
                onDisconnectDrive = onDisconnectDrive,
                onBackupToDrive = onBackupToDrive,
                onRefreshDriveBackups = onRefreshDriveBackups,
                onRestoreFromDrive = onRestoreFromDrive,
                onExportFullBackupJson = onExportBackupJson,
                onRestoreFullBackupJson = onRestoreBackupJson,
                onExportSettingsJson = onExportSettingsJson,
                onRestoreSettingsJson = onRestoreSettingsJson,
                onExportContactsVcf = onExportContactsVcf
            )
        }

        // Caller Screen Backdrop Studio (1 active service at a time: Video / Gallery Photo / Calling Card)
        item(key = "video_backdrop", contentType = "settings_card") {
            val activeBackdrop = settings.defaultBackdropMode
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Caller screen backdrop",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CALLER SCREEN BACKDROP (1 ACTIVE AT A TIME)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        when {
                            activeBackdrop == CallerBackdropMode.PHOTO && !settings.defaultPhotoBackdropUri.isNullOrBlank() -> {
                                AsyncImage(
                                    model = Uri.parse(settings.defaultPhotoBackdropUri),
                                    contentDescription = "Caller screen gallery photo preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            activeBackdrop == CallerBackdropMode.CALLING_CARD && !settings.defaultCallingCardUri.isNullOrBlank() -> {
                                AsyncImage(
                                    model = Uri.parse(settings.defaultCallingCardUri),
                                    contentDescription = "Caller screen calling card preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Image(
                                    painter = painterResource(R.drawable.img_nothing_glyph_backdrop_1794769643357),
                                    contentDescription = "Caller screen backdrop preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = settings.videoRedTintStrength * 0.65f
                                            ),
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
                                        )
                                    )
                                )
                        )
                        val previewTitle = when (activeBackdrop) {
                            CallerBackdropMode.VIDEO ->
                                if (settings.defaultVideoBackdropUri != null) "ACTIVE: VIDEO BACKDROP"
                                else "VIDEO MODE • PICK A VIDEO"
                            CallerBackdropMode.PHOTO ->
                                if (settings.defaultPhotoBackdropUri != null) "ACTIVE: PHOTO FROM GALLERY"
                                else "PHOTO MODE • PICK A GALLERY PHOTO"
                            CallerBackdropMode.CALLING_CARD ->
                                if (settings.defaultCallingCardUri != null) "ACTIVE: CALLING CARD"
                                else "CALLING CARD • PICK CARD IMAGE"
                            CallerBackdropMode.NONE -> "PHONE (3A) DEFAULT GLYPH"
                        }
                        Text(
                            text = previewTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        )
                    }

                    Text(
                        text = "SELECT 1 BACKDROP SERVICE (OTHERS STAY OFF)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DotMatrixTagChip(
                            label = if (activeBackdrop == CallerBackdropMode.VIDEO) "VIDEO • ON" else "VIDEO • OFF",
                            active = activeBackdrop == CallerBackdropMode.VIDEO,
                            onClick = {
                                if (activeBackdrop == CallerBackdropMode.VIDEO) {
                                    onSelectDefaultBackdropMode(CallerBackdropMode.NONE)
                                } else if (settings.defaultVideoBackdropUri != null) {
                                    onSelectDefaultBackdropMode(CallerBackdropMode.VIDEO)
                                } else {
                                    defaultVideoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                            },
                            testTag = "backdrop_mode_video"
                        )
                        DotMatrixTagChip(
                            label = if (activeBackdrop == CallerBackdropMode.PHOTO) "GALLERY PHOTO • ON" else "GALLERY PHOTO • OFF",
                            active = activeBackdrop == CallerBackdropMode.PHOTO,
                            onClick = {
                                if (activeBackdrop == CallerBackdropMode.PHOTO) {
                                    onSelectDefaultBackdropMode(CallerBackdropMode.NONE)
                                } else if (settings.defaultPhotoBackdropUri != null) {
                                    onSelectDefaultBackdropMode(CallerBackdropMode.PHOTO)
                                } else {
                                    defaultPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                            testTag = "backdrop_mode_photo"
                        )
                        val callingCardCount = remember(contacts) {
                            contacts.count { !it.callingCardUri.isNullOrBlank() }
                        }
                        DotMatrixTagChip(
                            label = if (callingCardCount > 0) "CALLING CARD ($callingCardCount)" else "CALLING CARD • PER CONTACT",
                            active = callingCardCount > 0 || activeBackdrop == CallerBackdropMode.CALLING_CARD,
                            onClick = { showCallingCardContactPicker = true },
                            testTag = "backdrop_mode_calling_card"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                when (activeBackdrop) {
                                    CallerBackdropMode.PHOTO -> defaultPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                    CallerBackdropMode.CALLING_CARD -> showCallingCardContactPicker = true
                                    else -> defaultVideoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_pick_video_button")
                        ) {
                            Text(
                                text = when (activeBackdrop) {
                                    CallerBackdropMode.PHOTO -> "PICK GALLERY PHOTO"
                                    CallerBackdropMode.CALLING_CARD -> "CHOOSE CONTACT CARD"
                                    else -> "PICK DEVICE VIDEO"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (activeBackdrop != CallerBackdropMode.NONE) {
                            OutlinedButton(
                                onClick = {
                                    when (activeBackdrop) {
                                        CallerBackdropMode.VIDEO -> onPickDefaultVideo(null)
                                        CallerBackdropMode.PHOTO -> onPickDefaultPhoto(null)
                                        CallerBackdropMode.CALLING_CARD -> onPickDefaultCallingCard(null)
                                        CallerBackdropMode.NONE -> Unit
                                    }
                                    onSelectDefaultBackdropMode(CallerBackdropMode.NONE)
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    "TURN OFF",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Text(
                        text = "TINT INTENSITY (${(settings.videoRedTintStrength * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.videoRedTintStrength,
                        onValueChange = onRedTintStrengthChange,
                        valueRange = 0.0f..0.85f
                    )
                }
            }
        }

        // Vibration Glyph Pattern Studio
        item(key = "vibration_studio", contentType = "settings_card") {
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Vibration studio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOM VIBRATION GLYPH COMPOSER",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPatternName,
                            onValueChange = { customPatternName = it },
                            placeholder = {
                                Text(
                                    "Pattern name e.g. 3A STROBE",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(54.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    if (recordedTimings.isEmpty()) {
                                        recordedTimings.add(0L)
                                        recordedTimings.add(75L)
                                    } else {
                                        val gap = (now - lastTapTimeMs).coerceIn(45L, 700L)
                                        recordedTimings.add(gap)
                                        recordedTimings.add(85L)
                                    }
                                    lastTapTimeMs = now
                                }
                                .testTag("tap_rhythm_pad"),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = "Tap rhythm beat",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Text(
                        text = "Taps recorded: ${recordedTimings.size / 2} beats (${recordedTimings.joinToString(",")})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveCustomVibration(customPatternName, recordedTimings.toList())
                                customPatternName = ""
                                recordedTimings.clear()
                            },
                            enabled = customPatternName.isNotBlank() && recordedTimings.isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("SAVE GLYPH", style = MaterialTheme.typography.labelMedium)
                        }
                        if (recordedTimings.isNotEmpty()) {
                            TextButton(onClick = { recordedTimings.clear() }) {
                                Text("CLEAR TAPS", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        items(
            items = vibrationPatterns,
            key = { it.id },
            contentType = { "vibration_pattern" }
        ) { pattern ->
            val isSelected = settings.activeVibrationPatternId == pattern.id
            NothingSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vibration_pattern_${pattern.id}"),
                highlighted = isSelected,
                onClick = { onSelectVibrationPattern(pattern.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pattern.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = pattern.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onTestVibrationPattern(pattern) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("test_vibration_${pattern.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Test vibration ${pattern.name}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!pattern.isBuiltIn) {
                            IconButton(
                                onClick = { onDeleteCustomVibration(pattern.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete custom vibration",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "about_card", contentType = "settings_card") {
            AboutSettingsCard()
        }
    }

    if (showCallingCardContactPicker) {
        ContactCallingCardPickerSheet(
            contacts = contacts,
            onDismiss = { showCallingCardContactPicker = false },
            onSelectContactForCard = { contact ->
                pendingCallingCardContact = contact
                contactCallingCardLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onClearContactCallingCard = { contact ->
                onAssignContactCallingCard(contact, null)
            }
        )
    }
}
