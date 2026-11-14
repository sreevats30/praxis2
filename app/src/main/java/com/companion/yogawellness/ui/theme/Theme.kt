package com.companion.yogawellness.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Design system — "Practice room"
 *
 * A dark, warm-neutral interface: the app is used with the phone propped up across a room, often in
 * low light, and the camera screen is dark by nature, so every screen shares one dark identity.
 *
 *  - Neutrals are warm (ink, not slate) so the UI reads calm rather than technical.
 *  - Ivory is the action colour: primary buttons and the selected option of a selector.
 *  - Jade means "on target": active state, progress, aligned joints, positive results.
 *  - Amber and coral are reserved for caution and problems. Data series have their own hues and are
 *    only used inside charts and meters.
 *  - Surfaces are mostly the page itself. Raised surfaces are kept for sheets, dialogs, media tiles
 *    and controls; content is separated by spacing and hairlines rather than cards.
 */

/** Score bands (alignment, joints, sessions). Also used over the camera feed. */
object ScoreColors {
    val Green = Color(0xFF6DBE98)
    val Amber = Color(0xFFE2AE52)
    val Red = Color(0xFFE8705E)
    val Neutral = Color(0xFF8C8A84)
}

@Immutable
data class AppColors(
    /** Page background. */
    val canvas: Color,
    /** Sheets, dialogs and the bottom navigation. */
    val raised: Color,
    /** Inputs, selector tracks, media tiles, unselected chips. */
    val control: Color,
    /** Pressed / emphasised control fill. */
    val controlHigh: Color,
    /** Hairline dividers. */
    val line: Color,
    /** Outlines of secondary buttons and unchecked controls. */
    val lineStrong: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    /** Ivory: primary-action fill and the selected option of a selector. */
    val inverse: Color,
    val onInverse: Color,
    /** Jade: on target, active, progress. */
    val accent: Color,
    val onAccent: Color,
    val accentMuted: Color,
    val warning: Color,
    val warningMuted: Color,
    val negative: Color,
    val negativeMuted: Color,
    /** Data series — the same hues as the trend chart. */
    val mood: Color,
    val stress: Color,
    val sleep: Color,
    val quality: Color
)

private val Ink = AppColors(
    canvas = Color(0xFF0E0E0D),
    raised = Color(0xFF171716),
    control = Color(0xFF1F1F1D),
    controlHigh = Color(0xFF292927),
    line = Color(0xFF262624),
    lineStrong = Color(0xFF3B3A37),
    text = Color(0xFFEEECE7),
    textSecondary = Color(0xFFABA9A2),
    textTertiary = Color(0xFF807E78),
    textDisabled = Color(0xFF57564F),
    inverse = Color(0xFFEEECE7),
    onInverse = Color(0xFF0E0E0D),
    accent = Color(0xFF6DBE98),
    onAccent = Color(0xFF0B1A13),
    accentMuted = Color(0xFF1A2A22),
    warning = Color(0xFFE2AE52),
    warningMuted = Color(0xFF2D2416),
    negative = Color(0xFFE8705E),
    negativeMuted = Color(0xFF301B17),
    mood = Color(0xFF6DBE98),
    stress = Color(0xFFE38B60),
    sleep = Color(0xFF8EA5E8),
    quality = Color(0xFFD4B77F)
)

private val LocalAppColors = staticCompositionLocalOf { Ink }

private fun scheme(c: AppColors) = darkColorScheme(
    primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentMuted, onPrimaryContainer = c.accent,
    secondary = c.textSecondary, onSecondary = c.canvas, secondaryContainer = c.control, onSecondaryContainer = c.text,
    tertiary = c.accent, onTertiary = c.onAccent,
    background = c.canvas, onBackground = c.text,
    surface = c.raised, onSurface = c.text, surfaceVariant = c.control, onSurfaceVariant = c.textSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = c.canvas, surfaceContainerLow = c.raised, surfaceContainer = c.raised,
    surfaceContainerHigh = c.raised, surfaceContainerHighest = c.control,
    inverseSurface = c.inverse, inverseOnSurface = c.onInverse,
    outline = c.lineStrong, outlineVariant = c.line,
    scrim = Color.Black.copy(alpha = 0.6f),
    error = c.negative, onError = c.canvas, errorContainer = c.negativeMuted, onErrorContainer = c.negative
)

/** Corner radii: 4 for small marks, 8 for anything you press or type into, 12 for media and sheets. */
object Radius {
    val S = 4.dp
    val M = 8.dp
    val L = 12.dp
}

/** 4-point spacing scale. [Gutter] is the page margin. */
object Space {
    val XXS = 2.dp
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 24.dp
    val XXL = 32.dp
    val Gutter = 20.dp
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.S),
    small = RoundedCornerShape(Radius.M),
    medium = RoundedCornerShape(Radius.M),
    large = RoundedCornerShape(Radius.L),
    extraLarge = RoundedCornerShape(Radius.L)
)

/** The app is dark-only: one identity across the camera screen and everything around it. */
@Composable
fun YogaWellnessTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides Ink) {
        MaterialTheme(colorScheme = scheme(Ink), typography = AppTypography, shapes = AppShapes, content = content)
    }
}

/** The app's colour tokens: `AppTheme.colors.textSecondary`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}
