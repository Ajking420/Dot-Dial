package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.data.preferences.ColorPlateStyle
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.UiBackgroundMode

@Immutable
data class DotDialExtendedStyle(
    val preset: DialerThemePreset = DialerThemePreset.NOTHING_DARK,
    val dotDensity: Float = 0.65f,
    val hairlineBorder: Color = HairlineDark,
    val dotGridColor: Color = Color(0xFF28282E),
    val accentGlow: Color = NothingRed,
    val isDark: Boolean = true,
    val fontPreset: DialerFontPreset = DialerFontPreset.NOTHING_DOT,
    val fontStyleMode: FontStyleMode = FontStyleMode.NORMAL,
    val coolJazzGlow: Boolean = false
)

val LocalDotDialStyle = staticCompositionLocalOf { DotDialExtendedStyle() }
val LocalUiHapticPulse = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun DotDialTheme(
    preset: DialerThemePreset = DialerThemePreset.NOTHING_DARK,
    autoDayNightThemeEnabled: Boolean = false,
    amoledBlackEnabled: Boolean = false,
    dotDensity: Float = 0.65f,
    fontPreset: DialerFontPreset = DialerFontPreset.NOTHING_DOT,
    fontStyleMode: FontStyleMode = FontStyleMode.NORMAL,
    fontScale: Float = 1.0f,
    coolJazzGlow: Boolean = true,
    customFontPath: String? = null,
    customBgHex: Long = 0xFF0A0A0EL,
    customSurfaceHex: Long = 0xFF17171FL,
    customAccentHex: Long = 0xFFD71921L,
    customTextHex: Long = 0xFFF5F5F7L,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectivePreset = remember(preset, autoDayNightThemeEnabled, systemDark) {
        if (!autoDayNightThemeEnabled) {
            preset
        } else if (systemDark) {
            if (preset.isDark) preset else DialerThemePreset.NOTHING_DARK
        } else {
            if (!preset.isDark) preset else DialerThemePreset.WHITE_RED
        }
    }

    val forceAmoledBg = amoledBlackEnabled &&
        effectivePreset != DialerThemePreset.WHITE_RED &&
        effectivePreset != DialerThemePreset.CHALK_MATRIX

    val colorScheme = remember(
        effectivePreset,
        forceAmoledBg,
        customBgHex,
        customSurfaceHex,
        customAccentHex,
        customTextHex
    ) { when (effectivePreset) {
        DialerThemePreset.NOTHING_DARK -> darkColorScheme(
            primary = NothingRed,
            onPrimary = ChalkWhite,
            primaryContainer = DeepCrimsonTint,
            onPrimaryContainer = CrimsonPulse,
            secondary = ChalkWhite,
            onSecondary = NothingBlack,
            secondaryContainer = HighCarbon,
            onSecondaryContainer = ChalkWhite,
            tertiary = MatrixEmerald,
            onTertiary = NothingBlack,
            background = if (forceAmoledBg) NothingBlack else CarbonSurface,
            onBackground = ChalkWhite,
            surface = if (forceAmoledBg) NothingBlack else CarbonSurface,
            onSurface = ChalkWhite,
            surfaceVariant = if (forceAmoledBg) Color(0xFF0A0A0D) else ElevatedSlate,
            onSurfaceVariant = DotMutedDark,
            outline = HairlineDark,
            error = CrimsonPulse,
            onError = ChalkWhite
        )

        DialerThemePreset.PURE_OLED_RED -> darkColorScheme(
            primary = CrimsonPulse,
            onPrimary = ChalkWhite,
            primaryContainer = Color(0xFF4A080D),
            onPrimaryContainer = ChalkWhite,
            secondary = ChalkWhite,
            onSecondary = NothingBlack,
            secondaryContainer = Color(0xFF141418),
            onSecondaryContainer = ChalkWhite,
            tertiary = MatrixEmerald,
            onTertiary = NothingBlack,
            background = NothingBlack,
            onBackground = ChalkWhite,
            surface = NothingBlack,
            onSurface = ChalkWhite,
            surfaceVariant = Color(0xFF0D0D11),
            onSurfaceVariant = Color(0xFFA8A8B0),
            outline = Color(0xFF3E1519),
            error = CrimsonPulse,
            onError = ChalkWhite
        )

        DialerThemePreset.WHITE_RED -> lightColorScheme(
            primary = NothingRed,
            onPrimary = PureWhite,
            primaryContainer = Color(0xFFFFE5E7),
            onPrimaryContainer = Color(0xFF8C0910),
            secondary = InkBlack,
            onSecondary = PureWhite,
            secondaryContainer = Color(0xFFF3F3F7),
            onSecondaryContainer = InkBlack,
            tertiary = Color(0xFF1E8E4E),
            onTertiary = PureWhite,
            background = PureWhite,
            onBackground = InkBlack,
            surface = PureWhite,
            onSurface = InkBlack,
            surfaceVariant = Color(0xFFF6F6FA),
            onSurfaceVariant = Color(0xFF54545E),
            outline = Color(0xFFDCDCE4),
            error = NothingRed,
            onError = PureWhite
        )

        DialerThemePreset.CHALK_MATRIX -> lightColorScheme(
            primary = NothingRed,
            onPrimary = ChalkElevated,
            primaryContainer = Color(0xFFFCE4E6),
            onPrimaryContainer = Color(0xFF8A0B11),
            secondary = InkBlack,
            onSecondary = ChalkElevated,
            secondaryContainer = Color(0xFFDCDCE4),
            onSecondaryContainer = InkBlack,
            tertiary = Color(0xFF1E8E4E),
            onTertiary = ChalkElevated,
            background = ChalkWhite,
            onBackground = InkBlack,
            surface = ChalkSurface,
            onSurface = InkBlack,
            surfaceVariant = ChalkElevated,
            onSurfaceVariant = DotMutedLight,
            outline = HairlineLight,
            error = NothingRed,
            onError = ChalkElevated
        )

        DialerThemePreset.STEALTH_AMBER -> darkColorScheme(
            primary = StealthAmber,
            onPrimary = NothingBlack,
            primaryContainer = Color(0xFF362500),
            onPrimaryContainer = StealthAmberBright,
            secondary = StealthAmberBright,
            onSecondary = NothingBlack,
            secondaryContainer = Color(0xFF1D190E),
            onSecondaryContainer = StealthAmberBright,
            tertiary = NothingRed,
            onTertiary = ChalkWhite,
            background = if (forceAmoledBg) NothingBlack else Color(0xFF0B0B09),
            onBackground = StealthAmberBright,
            surface = if (forceAmoledBg) NothingBlack else Color(0xFF12110E),
            onSurface = StealthAmberBright,
            surfaceVariant = if (forceAmoledBg) Color(0xFF0E0D09) else Color(0xFF1B1912),
            onSurfaceVariant = StealthAmberMuted,
            outline = Color(0xFF473A1B),
            error = CrimsonPulse,
            onError = ChalkWhite
        )

        DialerThemePreset.CUSTOM -> {
            val bg = if (forceAmoledBg) NothingBlack else Color(customBgHex)
            val surf = if (forceAmoledBg) Color(0xFF0A0A0D) else Color(customSurfaceHex)
            val acc = Color(customAccentHex)
            val txt = Color(customTextHex)
            val darkMode = bg.luminance() < 0.45f
            if (darkMode) {
                darkColorScheme(
                    primary = acc,
                    onPrimary = if (acc.luminance() > 0.55f) NothingBlack else ChalkWhite,
                    primaryContainer = acc.copy(alpha = 0.25f),
                    onPrimaryContainer = txt,
                    secondary = txt,
                    onSecondary = bg,
                    background = bg,
                    onBackground = txt,
                    surface = bg,
                    onSurface = txt,
                    surfaceVariant = surf,
                    onSurfaceVariant = txt.copy(alpha = 0.72f),
                    outline = txt.copy(alpha = 0.22f),
                    error = CrimsonPulse,
                    onError = ChalkWhite
                )
            } else {
                lightColorScheme(
                    primary = acc,
                    onPrimary = if (acc.luminance() > 0.55f) InkBlack else PureWhite,
                    primaryContainer = acc.copy(alpha = 0.16f),
                    onPrimaryContainer = txt,
                    secondary = txt,
                    onSecondary = bg,
                    background = bg,
                    onBackground = txt,
                    surface = bg,
                    onSurface = txt,
                    surfaceVariant = surf,
                    onSurfaceVariant = txt.copy(alpha = 0.68f),
                    outline = txt.copy(alpha = 0.20f),
                    error = NothingRed,
                    onError = PureWhite
                )
            }
        }
    } }

    val isDark = colorScheme.background.luminance() < 0.45f
    val extended = remember(
        effectivePreset,
        dotDensity,
        colorScheme,
        isDark,
        fontPreset,
        fontStyleMode,
        coolJazzGlow
    ) {
        DotDialExtendedStyle(
            preset = effectivePreset,
            dotDensity = dotDensity,
            hairlineBorder = colorScheme.outline,
            dotGridColor = if (isDark) colorScheme.onSurface.copy(alpha = 0.16f) else colorScheme.onSurface.copy(alpha = 0.14f),
            accentGlow = colorScheme.primary,
            isDark = isDark,
            fontPreset = fontPreset,
            fontStyleMode = fontStyleMode,
            coolJazzGlow = coolJazzGlow
        )
    }

    val typography = remember(
        fontPreset,
        fontStyleMode,
        fontScale,
        coolJazzGlow,
        isDark,
        customFontPath
    ) {
        buildDynamicTypography(
            fontPreset = fontPreset,
            styleMode = fontStyleMode,
            fontScale = fontScale,
            coolJazzGlow = coolJazzGlow,
            isDarkTheme = isDark,
            customFontPath = customFontPath
        )
    }

    CompositionLocalProvider(LocalDotDialStyle provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

/**
 * Computes whether system status bar (first) and navigation bar (second) should use dark icons
 * based on the app's actual rendered top/bottom background luminance rather than system dark mode.
 */
fun resolveSystemBarsUseDarkIcons(
    settings: DialerSettings,
    themeBackground: Color,
    isThemeDark: Boolean
): Pair<Boolean, Boolean> {
    val topDark: Boolean
    val bottomDark: Boolean
    when (settings.uiBackgroundMode) {
        UiBackgroundMode.DOT_MATRIX -> {
            val baseColor = when {
                settings.amoledBlackEnabled && isThemeDark -> Color.Black
                isThemeDark && settings.dotMatrixBgHex != 0L -> Color(settings.dotMatrixBgHex)
                !isThemeDark && settings.autoDayNightThemeEnabled -> themeBackground
                settings.dotMatrixBgHex != 0L -> Color(settings.dotMatrixBgHex)
                else -> themeBackground
            }
            val dark = baseColor.luminance() < 0.45f
            topDark = dark
            bottomDark = dark
        }
        UiBackgroundMode.COLOR_PLATE -> {
            if (settings.colorPlateStyle == ColorPlateStyle.SOLID) {
                val solid = if (settings.amoledBlackEnabled) Color.Black else Color(settings.solidBgHex)
                val dark = solid.luminance() < 0.45f
                topDark = dark
                bottomDark = dark
            } else {
                val startColor = Color(settings.gradientStartHex)
                val endColor = if (settings.amoledBlackEnabled) Color.Black else Color(settings.gradientEndHex)
                topDark = startColor.luminance() < 0.45f
                bottomDark = endColor.luminance() < 0.45f
            }
        }
        UiBackgroundMode.WALLPAPER -> {
            if (!settings.wallpaperPath.isNullOrBlank()) {
                val dark = isThemeDark || settings.wallpaperDimStrength >= 0.35f
                topDark = dark
                bottomDark = dark
            } else {
                val startColor = Color(settings.gradientStartHex)
                val endColor = if (settings.amoledBlackEnabled) Color.Black else Color(settings.gradientEndHex)
                topDark = startColor.luminance() < 0.45f
                bottomDark = endColor.luminance() < 0.45f
            }
        }
    }
    return !topDark to !bottomDark
}
