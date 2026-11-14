package com.companion.yogawellness.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.R

/** Inter (SIL OFL), bundled — the app has no internet permission. One family throughout. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private fun style(size: Double, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em
)

val AppTypography = Typography(
    displayLarge = style(48.0, 56, FontWeight.SemiBold, -0.025),
    displayMedium = style(40.0, 48, FontWeight.SemiBold, -0.022),
    displaySmall = style(34.0, 40, FontWeight.SemiBold, -0.02),
    headlineLarge = style(30.0, 36, FontWeight.SemiBold, -0.02),
    headlineMedium = style(28.0, 34, FontWeight.SemiBold, -0.018),
    headlineSmall = style(22.0, 28, FontWeight.SemiBold, -0.012),
    titleLarge = style(19.0, 24, FontWeight.SemiBold, -0.008),
    titleMedium = style(16.0, 22, FontWeight.SemiBold, -0.004),
    titleSmall = style(14.0, 20, FontWeight.SemiBold),
    bodyLarge = style(16.0, 24, FontWeight.Normal),
    bodyMedium = style(14.0, 20, FontWeight.Normal),
    bodySmall = style(12.5, 17, FontWeight.Normal, 0.004),
    labelLarge = style(14.0, 20, FontWeight.Medium),
    labelMedium = style(12.0, 16, FontWeight.Medium, 0.006),
    labelSmall = style(11.0, 14, FontWeight.Medium, 0.02)
)

/** Tabular figures for values that change (scores, timers, stats) so digits don't jitter. */
val NumericStyle = TextStyle(
    fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", letterSpacing = (-0.01).em
)
