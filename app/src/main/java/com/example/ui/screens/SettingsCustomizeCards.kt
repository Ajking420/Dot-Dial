package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AudioOutputMode
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.DialpadSoundStyle
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.theme.DialerFontPreset
import com.example.ui.theme.DialerThemePreset
import com.example.ui.theme.FontStyleMode
import com.example.ui.theme.LocalDotDialStyle

@Composable
fun ThemeAppearanceSettingsCard(
    settings: DialerSettings,
    onSelectTheme: (DialerThemePreset) -> Unit,
    onToggleAmoledBlack: (Boolean) -> Unit,
    onUpdateCustomColors: (Long, Long, Long, Long) -> Unit,
    onDotDensityChange: (Float) -> Unit
) {
    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Themes",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DIALER INTERFACE THEME",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            DialerThemePreset.entries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { preset ->
                        val active = settings.themePreset == preset
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectTheme(preset) }
                                .testTag("theme_preset_${preset.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (active) MaterialTheme.colorScheme.primary
                                else LocalDotDialStyle.current.hairlineBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            SettingsSwitchRow(
                title = "Treat Normal Black as AMOLED Black",
                subtitle = "Force pure #000000 black pixels across dark themes",
                checked = settings.amoledBlackEnabled,
                onCheckedChange = onToggleAmoledBlack,
                testTag = "amoled_black_switch"
            )

            AnimatedVisibility(
                visible = settings.themePreset == DialerThemePreset.CUSTOM,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                CustomThemeColorBuilder(
                    settings = settings,
                    onUpdateCustomColors = onUpdateCustomColors
                )
            }

            Text(
                text = "DOT MATRIX DENSITY (${(settings.dotDensity * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = settings.dotDensity,
                onValueChange = onDotDensityChange,
                valueRange = 0.2f..1.0f
            )
        }
    }
}

@Composable
private fun CustomThemeColorBuilder(
    settings: DialerSettings,
    onUpdateCustomColors: (Long, Long, Long, Long) -> Unit
) {
    val bgChoices = listOf(
        0xFF000000L to "OLED",
        0xFF0C0E14L to "Carbon",
        0xFF161124L to "Plum",
        0xFFFFFFFFL to "White"
    )
    val accentChoices = listOf(
        0xFFD71921L to "Nothing Red",
        0xFFFFC83DL to "Amber",
        0xFF2ED573L to "Emerald",
        0xFF4DABF7L to "Cyan",
        0xFFFF6B9DL to "Rose"
    )
    val textChoices = listOf(
        0xFFF5F5F7L to "Chalk",
        0xFFFFE082L to "Yellow",
        0xFFA8FFB8L to "Mint",
        0xFF101014L to "Ink"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CUSTOM PALETTE STUDIO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "BACKGROUND SURFACE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                bgChoices.forEach { (hex, label) ->
                    val surf = if (hex == 0xFFFFFFFFL) 0xFFF2F2F7L else 0xFF17171FL
                    ColorSwatchCircle(
                        colorHex = hex,
                        label = label,
                        selected = settings.customBgHex == hex,
                        onClick = {
                            onUpdateCustomColors(hex, surf, settings.customAccentHex, settings.customTextHex)
                        }
                    )
                }
            }

            Text(
                text = "ACCENT COLOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accentChoices.forEach { (hex, label) ->
                    ColorSwatchCircle(
                        colorHex = hex,
                        label = label,
                        selected = settings.customAccentHex == hex,
                        onClick = {
                            onUpdateCustomColors(settings.customBgHex, settings.customSurfaceHex, hex, settings.customTextHex)
                        }
                    )
                }
            }

            Text(
                text = "FONT & CONTACT TEXT COLOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                textChoices.forEach { (hex, label) ->
                    ColorSwatchCircle(
                        colorHex = hex,
                        label = label,
                        selected = settings.customTextHex == hex,
                        onClick = {
                            onUpdateCustomColors(settings.customBgHex, settings.customSurfaceHex, settings.customAccentHex, hex)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchCircle(
    colorHex: Long,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(colorHex))
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TypographySettingsCard(
    settings: DialerSettings,
    onSelectFontPreset: (DialerFontPreset) -> Unit,
    onSelectFontStyle: (FontStyleMode) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onToggleCoolJazzGlow: (Boolean) -> Unit,
    onImportCustomFontUri: (Uri) -> Unit
) {
    val fontFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportCustomFontUri(uri)
    }

    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FontDownload,
                    contentDescription = "Typography & fonts",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FONTS & TYPOGRAPHY STUDIO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialerFontPreset.entries.forEach { preset ->
                    DotMatrixTagChip(
                        label = preset.label,
                        active = settings.fontPreset == preset,
                        onClick = { onSelectFontPreset(preset) },
                        testTag = "font_preset_${preset.id}"
                    )
                }
            }

            OutlinedButton(
                onClick = { fontFileLauncher.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_custom_font_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (settings.customFontPath != null) "CHANGE CUSTOM .TTF / .OTF FONT"
                    else "IMPORT CUSTOM .TTF / .OTF FONT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "STYLE WEIGHT & SLANT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontStyleMode.entries.forEach { mode ->
                    DotMatrixTagChip(
                        label = mode.label,
                        active = settings.fontStyleMode == mode,
                        onClick = { onSelectFontStyle(mode) },
                        testTag = "font_style_${mode.id}"
                    )
                }
            }

            SettingsSwitchRow(
                title = "White Luminous Text Glow",
                subtitle = "Soft glowing highlight (Cool Jazz & dark modes)",
                checked = settings.coolJazzGlowEnabled,
                onCheckedChange = onToggleCoolJazzGlow,
                testTag = "cool_jazz_glow_switch"
            )

            Text(
                text = "FONT SIZE SCALE (${(settings.fontScale * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = settings.fontScale,
                onValueChange = onFontScaleChange,
                valueRange = 0.85f..1.30f
            )
        }
    }
}

@Composable
fun DisplayAndAudioSettingsCard(
    settings: DialerSettings,
    supportedRefreshRates: List<Int>,
    onSetPreferredFps: (Int) -> Unit,
    onSetAdaptiveRefreshRate: (Boolean) -> Unit,
    onSetUltraSmoothMode: (Boolean) -> Unit,
    onSetUiSounds: (Boolean) -> Unit,
    onSetDialpadSoundStyle: (DialpadSoundStyle) -> Unit,
    onSetAudioOutputMode: (AudioOutputMode) -> Unit,
    onPickCustomRingtone: (Uri?) -> Unit
) {
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onPickCustomRingtone(uri)
    }

    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Display and audio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DISPLAY FPS, SOUNDS & AUDIO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "FRAME RATE TARGET (AUTO / 60 - 120 FPS)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (supportedRefreshRates.isNotEmpty()) {
                Text(
                    text = "Display supports: ${supportedRefreshRates.joinToString(" • ") { "${it}Hz" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "AUTO",
                    60 to "60 FPS",
                    90 to "90 FPS (DEF)",
                    120 to "120 FPS"
                ).forEach { (fps, label) ->
                    DotMatrixTagChip(
                        label = label,
                        active = settings.preferredFps == fps,
                        onClick = { onSetPreferredFps(fps) },
                        testTag = "fps_chip_$fps"
                    )
                }
            }

            SettingsSwitchRow(
                title = "Adaptive Refresh Rate & Touch Boost",
                subtitle = "Boosts to peak Hz on touch/scroll and saves power when idle",
                checked = settings.adaptiveRefreshRate,
                onCheckedChange = onSetAdaptiveRefreshRate,
                testTag = "adaptive_refresh_switch"
            )

            SettingsSwitchRow(
                title = "Ultra-Smooth Low-Overdraw Mode",
                subtitle = "Uses a flat background and disables text glow to reduce graphics work while scrolling",
                checked = settings.ultraSmoothMode,
                onCheckedChange = onSetUltraSmoothMode,
                testTag = "ultra_smooth_switch"
            )

            Text(
                text = "DIALPAD NUMBER PRESS SOUND",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialpadSoundStyle.entries.forEach { soundStyle ->
                    DotMatrixTagChip(
                        label = soundStyle.label,
                        active = settings.dialpadSoundStyle == soundStyle,
                        onClick = { onSetDialpadSoundStyle(soundStyle) },
                        testTag = "dialpad_sound_${soundStyle.id}"
                    )
                }
            }

            SettingsSwitchRow(
                title = "UI Navigation & Tap Sounds",
                subtitle = "Play tactile click sound on tabs, buttons & backspace",
                checked = settings.uiSoundsEnabled,
                onCheckedChange = onSetUiSounds,
                testTag = "ui_sounds_switch"
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Audio output priority",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DEFAULT CALL AUDIO OUTPUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioOutputMode.entries.forEach { mode ->
                    DotMatrixTagChip(
                        label = mode.label,
                        active = settings.audioOutputMode == mode,
                        onClick = { onSetAudioOutputMode(mode) },
                        testTag = "audio_output_${mode.id}"
                    )
                }
            }

            Text(
                text = "CUSTOM RINGTONE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { ringtoneLauncher.launch(arrayOf("audio/*")) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_custom_ringtone_button")
                ) {
                    Text(
                        text = if (settings.customRingtoneUri != null) "CHANGE RINGTONE" else "CHOOSE AUDIO FILE",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (settings.customRingtoneUri != null) {
                    OutlinedButton(
                        onClick = { onPickCustomRingtone(null) },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "DEFAULT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
