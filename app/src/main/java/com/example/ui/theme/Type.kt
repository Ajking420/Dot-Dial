package com.example.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import java.io.File

val DotGothicFamily = FontFamily(
    Font(R.font.dotgothic16, FontWeight.Normal)
)

val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val CoolJazzFamily = FontFamily(
    Font(R.font.quicksand, FontWeight.Normal),
    Font(R.font.quicksand, FontWeight.Medium),
    Font(R.font.quicksand, FontWeight.Bold)
)

val NunitoRoundFamily = FontFamily(
    Font(R.font.nunito, FontWeight.Normal),
    Font(R.font.nunito, FontWeight.Bold)
)

val OrbitronGlyphFamily = FontFamily(
    Font(R.font.orbitron, FontWeight.Normal),
    Font(R.font.orbitron, FontWeight.Bold)
)

enum class DialerFontPreset(val id: String, val label: String) {
    NOTHING_DOT("nothing_dot", "NOTHING DOT"),
    COOL_JAZZ("cool_jazz", "COOL JAZZ"),
    SPACE_TECH("space_tech", "SPACE TECH"),
    MONO_MATRIX("mono_matrix", "MONO MATRIX"),
    NUNITO_FLOW("nunito_flow", "NUNITO SOFT"),
    ORBITRON_GLYPH("orbitron_glyph", "ORBITRON"),
    CUSTOM("custom", "CUSTOM TTF");

    companion object {
        fun fromId(id: String?): DialerFontPreset =
            entries.firstOrNull { it.id == id } ?: NOTHING_DOT
    }
}

enum class FontStyleMode(val id: String, val label: String) {
    NORMAL("normal", "Normal"),
    SEMI_ITALIC("semi_italic", "Semi-Italic"),
    ITALIC("italic", "Italic"),
    BOLD("bold", "Bold");

    companion object {
        fun fromId(id: String?): FontStyleMode =
            entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

fun resolveCustomTypefaceFamily(path: String?): FontFamily? {
    if (path.isNullOrBlank()) return null
    val file = File(path)
    if (!file.exists() || file.length() == 0L) return null
    return runCatching {
        val tf = Typeface.createFromFile(file)
        FontFamily(tf)
    }.getOrNull()
}

fun buildDynamicTypography(
    fontPreset: DialerFontPreset,
    styleMode: FontStyleMode,
    fontScale: Float,
    coolJazzGlow: Boolean,
    isDarkTheme: Boolean,
    customFontPath: String?
): Typography {
    val scale = fontScale.coerceIn(0.85f, 1.30f)
    val customFamily = resolveCustomTypefaceFamily(customFontPath)

    val displayFamily = when (fontPreset) {
        DialerFontPreset.NOTHING_DOT -> DotGothicFamily
        DialerFontPreset.COOL_JAZZ -> CoolJazzFamily
        DialerFontPreset.SPACE_TECH -> SpaceGroteskFamily
        DialerFontPreset.MONO_MATRIX -> JetBrainsMonoFamily
        DialerFontPreset.NUNITO_FLOW -> NunitoRoundFamily
        DialerFontPreset.ORBITRON_GLYPH -> OrbitronGlyphFamily
        DialerFontPreset.CUSTOM -> customFamily ?: SpaceGroteskFamily
    }

    val bodyFamily = when (fontPreset) {
        DialerFontPreset.COOL_JAZZ -> CoolJazzFamily
        DialerFontPreset.MONO_MATRIX -> JetBrainsMonoFamily
        DialerFontPreset.NUNITO_FLOW -> NunitoRoundFamily
        DialerFontPreset.CUSTOM -> customFamily ?: SpaceGroteskFamily
        else -> SpaceGroteskFamily
    }

    val resolvedStyle = when {
        styleMode == FontStyleMode.ITALIC -> FontStyle.Italic
        styleMode == FontStyleMode.SEMI_ITALIC -> FontStyle.Italic
        fontPreset == DialerFontPreset.COOL_JAZZ && styleMode == FontStyleMode.NORMAL -> FontStyle.Italic
        else -> FontStyle.Normal
    }

    val resolvedWeight = if (styleMode == FontStyleMode.BOLD) FontWeight.Bold else FontWeight.Normal
    val resolvedBoldWeight = FontWeight.Bold
    val letterSpacingAdjustFloat = if (styleMode == FontStyleMode.SEMI_ITALIC) 0.4f else 0f

    val glowShadow = if (fontPreset == DialerFontPreset.COOL_JAZZ && coolJazzGlow && isDarkTheme) {
        Shadow(
            color = Color.White.copy(alpha = 0.48f),
            offset = Offset(0f, 0f),
            blurRadius = 5f
        )
    } else null

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = resolvedWeight,
            fontStyle = resolvedStyle,
            fontSize = (40f * scale).sp,
            lineHeight = (46f * scale).sp,
            letterSpacing = (1.4f + letterSpacingAdjustFloat).sp,
            shadow = glowShadow
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = resolvedWeight,
            fontStyle = resolvedStyle,
            fontSize = (30f * scale).sp,
            lineHeight = (36f * scale).sp,
            letterSpacing = (1.1f + letterSpacingAdjustFloat).sp,
            shadow = glowShadow
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = resolvedWeight,
            fontStyle = resolvedStyle,
            fontSize = (23f * scale).sp,
            lineHeight = (29f * scale).sp,
            letterSpacing = (0.9f + letterSpacingAdjustFloat).sp,
            shadow = glowShadow
        ),
        titleLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = resolvedBoldWeight,
            fontStyle = resolvedStyle,
            fontSize = (20f * scale).sp,
            lineHeight = (25f * scale).sp,
            letterSpacing = 0.2.sp,
            shadow = glowShadow
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = if (styleMode == FontStyleMode.BOLD) FontWeight.Bold else FontWeight.Medium,
            fontStyle = resolvedStyle,
            fontSize = (16f * scale).sp,
            lineHeight = (22f * scale).sp,
            letterSpacing = 0.2.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = resolvedWeight,
            fontStyle = resolvedStyle,
            fontSize = (15f * scale).sp,
            lineHeight = (21f * scale).sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = resolvedWeight,
            fontStyle = resolvedStyle,
            fontSize = (13f * scale).sp,
            lineHeight = (18f * scale).sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = if (fontPreset == DialerFontPreset.COOL_JAZZ) CoolJazzFamily else JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = resolvedStyle,
            fontSize = (13f * scale).sp,
            lineHeight = (16f * scale).sp,
            letterSpacing = 0.8.sp
        ),
        labelMedium = TextStyle(
            fontFamily = if (fontPreset == DialerFontPreset.COOL_JAZZ) CoolJazzFamily else JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = resolvedStyle,
            fontSize = (11f * scale).sp,
            lineHeight = (14f * scale).sp,
            letterSpacing = 0.9.sp
        ),
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Normal,
            fontStyle = resolvedStyle,
            fontSize = (10f * scale).sp,
            lineHeight = (12f * scale).sp,
            letterSpacing = 0.7.sp
        )
    )
}
