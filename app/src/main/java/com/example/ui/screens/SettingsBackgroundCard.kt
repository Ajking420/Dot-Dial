package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.preferences.ColorPlateStyle
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.UiBackgroundMode
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.theme.LocalDotDialStyle
import java.io.File

private data class GradientSchemePreset(
    val title: String,
    val startHex: Long,
    val endHex: Long
)

private val GradientPresets = listOf(
    GradientSchemePreset("Nothing Crimson", 0xFF28050BL, 0xFF050508L),
    GradientSchemePreset("AMOLED Ember", 0xFF3E090FL, 0xFF000000L),
    GradientSchemePreset("Cyber Cobalt", 0xFF0A1D3BL, 0xFF04070FL),
    GradientSchemePreset("Emerald Matrix", 0xFF06291BL, 0xFF040807L),
    GradientSchemePreset("Amber Eclipse", 0xFF2F2004L, 0xFF080704L),
    GradientSchemePreset("Violet Dusk", 0xFF251038L, 0xFF06040CL),
    GradientSchemePreset("Obsidian Mist", 0xFF20232CL, 0xFF090A0EL),
    GradientSchemePreset("Silver Frost", 0xFFECEEF4L, 0xFFC7CCD8L)
)

private val DotMatrixBgSwatches = listOf(
    0L to "Theme",
    0xFF000000L to "AMOLED",
    0xFF0D0D12L to "Carbon",
    0xFF1A0508L to "Crimson",
    0xFF07111FL to "Navy",
    0xFF061610L to "Emerald",
    0xFF141006L to "Amber",
    0xFFFFFFFFL to "White"
)

private val BasicSolidSwatches = listOf(
    0xFF000000L to "AMOLED",
    0xFF0F1016L to "Obsidian",
    0xFF1E060AL to "Crimson",
    0xFF081426L to "Cobalt",
    0xFF071A12L to "Matrix",
    0xFF1B1406L to "Stealth",
    0xFF1B1028L to "Plum",
    0xFFF4F4F8L to "Chalk"
)

@Composable
fun UiBackgroundSettingsCard(
    settings: DialerSettings,
    onSelectBackgroundMode: (UiBackgroundMode) -> Unit,
    onToggleDotMatrix: (Boolean) -> Unit,
    onSelectDotMatrixBgHex: (Long) -> Unit,
    onToggleAmoledBlack: (Boolean) -> Unit,
    onDotDensityChange: (Float) -> Unit,
    onSelectColorPlateStyle: (ColorPlateStyle) -> Unit,
    onSelectSolidBgHex: (Long) -> Unit,
    onSelectGradientColors: (Long, Long) -> Unit,
    onPickWallpaper: (Uri?) -> Unit,
    onWallpaperDimChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickWallpaper(uri)
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
                    imageVector = Icons.Default.Layers,
                    contentDescription = "UI background studio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UI BACKGROUND STUDIO (1 ACTIVE AT A TIME)",
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
                UiBackgroundMode.entries.forEach { mode ->
                    DotMatrixTagChip(
                        label = mode.label,
                        active = settings.uiBackgroundMode == mode,
                        onClick = { onSelectBackgroundMode(mode) },
                        testTag = "bg_mode_${mode.id}"
                    )
                }
            }

            when (settings.uiBackgroundMode) {
                UiBackgroundMode.DOT_MATRIX -> {
                    SettingsSwitchRow(
                        title = "Dot Matrix Pattern",
                        subtitle = "Turn dot-matrix texture ON or OFF behind UI cards",
                        checked = settings.dotMatrixEnabled,
                        onCheckedChange = onToggleDotMatrix,
                        testTag = "dot_matrix_enabled_switch"
                    )

                    SettingsSwitchRow(
                        title = "AMOLED Pure Black (#000000)",
                        subtitle = "Force true pitch-black pixels behind dots",
                        checked = settings.amoledBlackEnabled,
                        onCheckedChange = onToggleAmoledBlack,
                        testTag = "bg_amoled_switch"
                    )

                    Text(
                        text = "DOT MATRIX BACKGROUND COLOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DotMatrixBgSwatches.forEach { (hex, label) ->
                            val swatchColor = if (hex == 0L) MaterialTheme.colorScheme.background else Color(hex)
                            BgSwatchCircle(
                                color = swatchColor,
                                label = label,
                                selected = !settings.amoledBlackEnabled && settings.dotMatrixBgHex == hex,
                                onClick = {
                                    if (hex == 0xFF000000L) {
                                        onToggleAmoledBlack(true)
                                    } else {
                                        if (settings.amoledBlackEnabled) onToggleAmoledBlack(false)
                                        onSelectDotMatrixBgHex(hex)
                                    }
                                }
                            )
                        }
                    }

                    if (settings.dotMatrixEnabled) {
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

                UiBackgroundMode.COLOR_PLATE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorPlateStyle.entries.forEach { plateStyle ->
                            DotMatrixTagChip(
                                label = plateStyle.label,
                                active = settings.colorPlateStyle == plateStyle,
                                onClick = { onSelectColorPlateStyle(plateStyle) },
                                testTag = "color_plate_${plateStyle.id}"
                            )
                        }
                    }

                    if (settings.colorPlateStyle == ColorPlateStyle.SOLID) {
                        SettingsSwitchRow(
                            title = "AMOLED Pure Black",
                            subtitle = "Use true #000000 background",
                            checked = settings.amoledBlackEnabled,
                            onCheckedChange = onToggleAmoledBlack
                        )
                        Text(
                            text = "BASIC BACKGROUND COLOR PLATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BasicSolidSwatches.forEach { (hex, label) ->
                                BgSwatchCircle(
                                    color = Color(hex),
                                    label = label,
                                    selected = !settings.amoledBlackEnabled && settings.solidBgHex == hex,
                                    onClick = {
                                        if (hex == 0xFF000000L) {
                                            onToggleAmoledBlack(true)
                                        } else {
                                            if (settings.amoledBlackEnabled) onToggleAmoledBlack(false)
                                            onSelectSolidBgHex(hex)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "GRADIENT COLOR SCHEMES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        GradientPresets.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { preset ->
                                    val selected = settings.gradientStartHex == preset.startHex &&
                                        settings.gradientEndHex == preset.endHex
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                if (settings.amoledBlackEnabled) onToggleAmoledBlack(false)
                                                onSelectGradientColors(preset.startHex, preset.endHex)
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else LocalDotDialStyle.current.hairlineBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(preset.startHex), Color(preset.endHex))
                                                    )
                                                )
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val labelColor = if (preset.startHex == 0xFFECEEF4L) {
                                                Color(0xFF101014)
                                            } else {
                                                Color(0xFFF5F5F7)
                                            }
                                            Text(
                                                text = preset.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = labelColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                UiBackgroundMode.WALLPAPER -> {
                    val wallpaperFile = remember(settings.wallpaperPath) {
                        settings.wallpaperPath?.let { File(it) }?.takeIf { it.exists() }
                    }
                    if (wallpaperFile != null) {
                        val req = remember(settings.wallpaperPath) {
                            ImageRequest.Builder(context)
                                .data(wallpaperFile)
                                .size(640, 320)
                                .crossfade(false)
                                .build()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, LocalDotDialStyle.current.hairlineBorder, RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = req,
                                contentDescription = "Selected UI wallpaper preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Text(
                            text = "Pick any photo from your gallery as your UI background. Only Wallpaper is active while selected.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                wallpaperLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_ui_wallpaper_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (wallpaperFile != null) "CHANGE WALLPAPER" else "PICK WALLPAPER",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (wallpaperFile != null) {
                            OutlinedButton(
                                onClick = { onPickWallpaper(null) },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("CLEAR", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Text(
                        text = "WALLPAPER DIMMING (${(settings.wallpaperDimStrength * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.wallpaperDimStrength,
                        onValueChange = onWallpaperDimChange,
                        valueRange = 0.05f..0.85f
                    )
                }
            }
        }
    }
}

@Composable
private fun BgSwatchCircle(
    color: Color,
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
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
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
