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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Score band colours shared by the overlay, cards and charts. */
object ScoreColors {
    val Green = Color(0xFF17A05D)
    val Amber = Color(0xFFE89B0C)
    val Red = Color(0xFFE5484D)
    val Neutral = Color(0xFF94A3B8)
}

/** Brand palette: deep clinical navy into teal, with a mint/cyan "vital" accent (matches the launcher icon). */
object Brand {
    val Navy950 = Color(0xFF050F1E)
    val Navy900 = Color(0xFF071A2F)
    val Navy800 = Color(0xFF0B2A4A)
    val Navy700 = Color(0xFF123A60)
    val Teal700 = Color(0xFF075E66)
    val Teal600 = Color(0xFF0B7A83)
    val Teal500 = Color(0xFF14A3A8)
    val Teal400 = Color(0xFF2DD4BF)
    val Mint = Color(0xFF5EEAD4)
    val Cyan = Color(0xFF22D3EE)
    val Gold = Color(0xFFFBBF24)

    val Mood = Color(0xFF7C5CFA)
    val Stress = Color(0xFFF97316)
    val Sleep = Color(0xFF4F6BED)
    val Mind = Color(0xFFDB2777)
    val Practice = Color(0xFF0B9FA8)
}

/** Colours Material's scheme has no slot for: hero gradients, semantic accents, hairlines. */
@Immutable
data class ClinicalColors(
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val accent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val hairline: Color,
    val cardBorder: Color,
    val inkMuted: Color,
    val glass: Color,
    val glassBorder: Color
) {
    val heroBrush: Brush
        get() = Brush.linearGradient(listOf(heroStart, heroMid, heroEnd))
    val ctaBrush: Brush
        get() = Brush.horizontalGradient(listOf(Brand.Teal600, Brand.Teal500))
}

private val LightClinical = ClinicalColors(
    heroStart = Brand.Navy900,
    heroMid = Brand.Navy800,
    heroEnd = Brand.Teal700,
    accent = Brand.Teal500,
    success = ScoreColors.Green,
    warning = ScoreColors.Amber,
    danger = Color(0xFFD92D20),
    info = Color(0xFF2563EB),
    hairline = Color(0xFFE3E9F0),
    cardBorder = Color(0xFFE1E8F0),
    inkMuted = Color(0xFF64748B),
    glass = Color(0x1FFFFFFF),
    glassBorder = Color(0x33FFFFFF)
)

private val DarkClinical = LightClinical.copy(
    heroStart = Brand.Navy950,
    heroMid = Brand.Navy900,
    heroEnd = Color(0xFF064B52),
    accent = Brand.Mint,
    danger = Color(0xFFF97066),
    info = Color(0xFF60A5FA),
    hairline = Color(0xFF1C2B40),
    cardBorder = Color(0xFF1E2E44),
    inkMuted = Color(0xFF94A3B8)
)

private val LocalClinicalColors = staticCompositionLocalOf { LightClinical }

private val LightColors = lightColorScheme(
    primary = Brand.Teal600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F5F1),
    onPrimaryContainer = Color(0xFF00302E),
    secondary = Brand.Navy700,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8F7),
    onSecondaryContainer = Brand.Navy900,
    tertiary = Brand.Mood,
    onTertiary = Color.White,
    background = Color(0xFFF3F6FA),
    onBackground = Color(0xFF0F1B2D),
    surface = Color.White,
    onSurface = Color(0xFF0F1B2D),
    surfaceVariant = Color(0xFFEDF2F7),
    onSurfaceVariant = Color(0xFF4A5A6E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFEBF0F5),
    surfaceContainerHighest = Color(0xFFE4EAF1),
    outline = Color(0xFFB8C4D2),
    outlineVariant = Color(0xFFDCE3EB),
    error = Color(0xFFD92D20),
    onError = Color.White,
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A271A)
)

private val DarkColors = darkColorScheme(
    primary = Brand.Mint,
    onPrimary = Color(0xFF00332E),
    primaryContainer = Color(0xFF0B4A4F),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF9CC3F5),
    onSecondary = Brand.Navy900,
    secondaryContainer = Brand.Navy700,
    onSecondaryContainer = Color(0xFFDCE8F7),
    tertiary = Color(0xFFB4A2FF),
    onTertiary = Color(0xFF26145E),
    background = Color(0xFF060E1A),
    onBackground = Color(0xFFE6EDF5),
    surface = Color(0xFF0C1828),
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = Color(0xFF15243A),
    onSurfaceVariant = Color(0xFFA9B7C9),
    surfaceContainerLowest = Color(0xFF07111F),
    surfaceContainerLow = Color(0xFF0C1828),
    surfaceContainer = Color(0xFF101E31),
    surfaceContainerHigh = Color(0xFF15243A),
    surfaceContainerHighest = Color(0xFF1B2C44),
    outline = Color(0xFF3B4D66),
    outlineVariant = Color(0xFF1E2E44),
    error = Color(0xFFF97066),
    onError = Color(0xFF55160C),
    errorContainer = Color(0xFF55160C),
    onErrorContainer = Color(0xFFFEE4E2)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun YogaWellnessTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalClinicalColors provides if (darkTheme) DarkClinical else LightClinical) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** Access to the extended clinical palette: `AppTheme.colors.heroBrush`. */
object AppTheme {
    val colors: ClinicalColors
        @Composable @ReadOnlyComposable get() = LocalClinicalColors.current
}
