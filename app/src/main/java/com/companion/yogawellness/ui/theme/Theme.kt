package com.companion.yogawellness.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Score bands (alignment, joints). Mid tones so they read on white cards and over the camera. */
object ScoreColors {
    val Green = Color(0xFF16A34A)
    val Amber = Color(0xFFD97706)
    val Red = Color(0xFFDC2626)
    val Neutral = Color(0xFF94A3B8)
}

/**
 * Semantic colour tokens. Neutral surfaces carry the UI; the single accent marks primary actions
 * and selection; status and data colours are used only where they carry meaning.
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    /** Recessed fills: inputs, segmented-control tracks, thumbnail tiles. */
    val sunken: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val positive: Color,
    val positiveSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val negative: Color,
    val negativeSoft: Color,
    /** Data series — the same hues as the trend chart. */
    val mood: Color,
    val stress: Color,
    val sleep: Color,
    val isDark: Boolean
)

private val Light = AppColors(
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFFFFFFF),
    sunken = Color(0xFFEFF1F4),
    border = Color(0xFFE3E6EB),
    borderStrong = Color(0xFFC9CED6),
    text = Color(0xFF111418),
    textSecondary = Color(0xFF4A5260),
    textTertiary = Color(0xFF6B7380),
    accent = Color(0xFF0B6E6A),
    onAccent = Color.White,
    accentSoft = Color(0xFFE2F1EF),
    onAccentSoft = Color(0xFF064A47),
    positive = Color(0xFF15803D),
    positiveSoft = Color(0xFFE6F4EA),
    warning = Color(0xFF9A5B06),
    warningSoft = Color(0xFFFBF1DE),
    negative = Color(0xFFBE2A2A),
    negativeSoft = Color(0xFFFCE9E9),
    mood = Color(0xFF14946B),
    stress = Color(0xFFE0612D),
    sleep = Color(0xFF2A6FD6),
    isDark = false
)

private val Dark = AppColors(
    background = Color(0xFF0E1013),
    surface = Color(0xFF171A1F),
    sunken = Color(0xFF1F2329),
    border = Color(0xFF2A2F36),
    borderStrong = Color(0xFF3B424C),
    text = Color(0xFFF1F3F5),
    textSecondary = Color(0xFFB4BAC4),
    textTertiary = Color(0xFF8B929D),
    accent = Color(0xFF4CB8B1),
    onAccent = Color(0xFF002321),
    accentSoft = Color(0xFF143331),
    onAccentSoft = Color(0xFFA9E2DE),
    positive = Color(0xFF4CC07A),
    positiveSoft = Color(0xFF14301F),
    warning = Color(0xFFE5A62A),
    warningSoft = Color(0xFF33270F),
    negative = Color(0xFFF07272),
    negativeSoft = Color(0xFF3A1A1A),
    mood = Color(0xFF34C08E),
    stress = Color(0xFFF07A45),
    sleep = Color(0xFF5B95EC),
    isDark = true
)

private val LocalAppColors = staticCompositionLocalOf { Light }

private fun scheme(c: AppColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, onPrimaryContainer = c.onAccentSoft,
        secondary = c.textSecondary, onSecondary = c.background, secondaryContainer = c.sunken, onSecondaryContainer = c.text,
        tertiary = c.accent, onTertiary = c.onAccent,
        background = c.background, onBackground = c.text,
        surface = c.surface, onSurface = c.text, surfaceVariant = c.sunken, onSurfaceVariant = c.textSecondary,
        surfaceContainerLowest = c.background, surfaceContainerLow = c.surface, surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface, surfaceContainerHighest = c.sunken,
        outline = c.borderStrong, outlineVariant = c.border,
        error = c.negative, onError = c.background, errorContainer = c.negativeSoft, onErrorContainer = c.negative
    )
} else {
    lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, onPrimaryContainer = c.onAccentSoft,
        secondary = c.textSecondary, onSecondary = c.surface, secondaryContainer = c.sunken, onSecondaryContainer = c.text,
        tertiary = c.accent, onTertiary = c.onAccent,
        background = c.background, onBackground = c.text,
        surface = c.surface, onSurface = c.text, surfaceVariant = c.sunken, onSurfaceVariant = c.textSecondary,
        surfaceContainerLowest = c.surface, surfaceContainerLow = c.surface, surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface, surfaceContainerHighest = c.sunken,
        outline = c.borderStrong, outlineVariant = c.border,
        error = c.negative, onError = Color.White, errorContainer = c.negativeSoft, onErrorContainer = c.negative
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun YogaWellnessTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) Dark else Light
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme(colors), typography = AppTypography, shapes = AppShapes, content = content)
    }
}

/** The app's colour tokens: `AppTheme.colors.textSecondary`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}
