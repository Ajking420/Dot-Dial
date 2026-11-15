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
import androidx.compose.material.icons.filled.Backup
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
import com.example.R
import com.example.data.local.VibrationPatternEntity
import com.example.data.preferences.AudioOutputMode
import com.example.data.preferences.AutoRecordMode
import com.example.data.preferences.ColorPlateStyle
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.DialpadSoundStyle
import com.example.data.preferences.UiBackgroundMode
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.DialerFontPreset
import com.example.ui.theme.DialerThemePreset
import com.example.ui.theme.FontStyleMode

@Composable
fun SettingsScreen(
    settings: DialerSettings,
    supportedRefreshRates: List<Int>,
    vibrationPatterns: List<VibrationPatternEntity>,
    onSelectTheme: (DialerThemePreset) -> Unit,
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
    onSetDialpadSoundStyle: (DialpadSoundStyle) -> Unit,
    onSetAudioOutputMode: (AudioOutputMode) -> Unit,
    onPickCustomRingtone: (Uri?) -> Unit,
    onPickDefaultVideo: (Uri?) -> Unit,
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
    onExportContactsVcf: (Uri) -> Unit,
    onRestoreBackupJson: (Uri) -> Unit,
    onPinContactWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickDefaultVideo(uri)
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) onExportBackupJson(uri) }

    val exportVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri -> if (uri != null) onExportContactsVcf(uri) }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onRestoreBackupJson(uri) }

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
                subtitle = "Themes, fonts, 60-120 FPS, ringtones, audio routing & privacy"
            )
        }

        // Theme & AMOLED Studio
        item(key = "theme_appearance", contentType = "settings_card") {
            ThemeAppearanceSettingsCard(
                settings = settings,
                onSelectTheme = onSelectTheme,
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
                onSetDialpadSoundStyle = onSetDialpadSoundStyle,
                onSetAudioOutputMode = onSetAudioOutputMode,
                onPickCustomRingtone = onPickCustomRingtone
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

        // Backup & Restore Card
        item(key = "backup_restore", contentType = "settings_card") {
            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup and restore",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BACKUP & RESTORE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportJsonLauncher.launch("dot_dial_backup.json") },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("EXPORT JSON", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { restoreJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "RESTORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { exportVcfLauncher.launch("dot_dial_contacts.vcf") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "EXPORT CONTACTS (.VCF VCARD)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Caller Screen Video Backdrop Customizer
        item(key = "video_backdrop", contentType = "settings_card") {
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
                            contentDescription = "Video backdrop",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CALLER SCREEN VIDEO BACKDROP",
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
                        Image(
                            painter = painterResource(R.drawable.img_nothing_glyph_backdrop_1794769643357),
                            contentDescription = "Caller screen video backdrop preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
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
                        Text(
                            text = if (settings.defaultVideoBackdropUri != null) "CUSTOM VIDEO + THEME TINT"
                            else "PHONE (3A) GLYPH LOOP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                defaultVideoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
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
                            Text("PICK DEVICE VIDEO", style = MaterialTheme.typography.labelMedium)
                        }
                        if (settings.defaultVideoBackdropUri != null) {
                            OutlinedButton(
                                onClick = { onPickDefaultVideo(null) },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    "RESET",
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
}
