package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val NothingBlack = Color(0xFF000000)
val CarbonSurface = Color(0xFF0D0D0F)
val ElevatedSlate = Color(0xFF17171A)
val HighCarbon = Color(0xFF222227)

val NothingRed = Color(0xFFD71921)
val CrimsonPulse = Color(0xFFFF2A32)
val DeepCrimsonTint = Color(0xFF3A070A)

val ChalkWhite = Color(0xFFF5F5F7)
val PureWhite = Color(0xFFFFFFFF)
val ChalkSurface = Color(0xFFE9E9EE)
val ChalkElevated = Color(0xFFFFFFFF)
val InkBlack = Color(0xFF101014)

val DotMutedDark = Color(0xFF9C9CA4)
val DotMutedLight = Color(0xFF5A5A62)
val HairlineDark = Color(0xFF2B2B31)
val HairlineLight = Color(0xFFD2D2DA)

val StealthAmber = Color(0xFFFFC83D)
val StealthAmberBright = Color(0xFFFFE082)
val StealthAmberMuted = Color(0xFFD1A84B)
val MatrixEmerald = Color(0xFF2ED573)

enum class DialerThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val isDark: Boolean
) {
    NOTHING_DARK(
        id = "nothing_dark",
        title = "NOTHING DARK",
        subtitle = "Obsidian carbon with signature Nothing Red",
        isDark = true
    ),
    PURE_OLED_RED(
        id = "pure_oled_red",
        title = "PURE OLED RED",
        subtitle = "True #000000 black with high-contrast crimson",
        isDark = true
    ),
    WHITE_RED(
        id = "white_red",
        title = "WHITE & RED",
        subtitle = "Pure luminous white paired with crimson red",
        isDark = false
    ),
    CHALK_MATRIX(
        id = "chalk_matrix",
        title = "CHALK MATRIX",
        subtitle = "Daylight monochrome paper with red dot accent",
        isDark = false
    ),
    STEALTH_AMBER(
        id = "stealth_amber",
        title = "STEALTH AMBER",
        subtitle = "Night-vision charcoal & bright yellow amber",
        isDark = true
    ),
    CUSTOM(
        id = "custom",
        title = "CUSTOM THEME",
        subtitle = "Your personalized surface & accent palette",
        isDark = true
    );

    companion object {
        fun fromId(id: String?): DialerThemePreset =
            entries.firstOrNull { it.id == id } ?: NOTHING_DARK
    }
}
