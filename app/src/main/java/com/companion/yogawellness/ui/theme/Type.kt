package com.companion.yogawellness.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.R

/** Display face: Plus Jakarta Sans (OFL, bundled — the app has no internet permission). */
val Jakarta = FontFamily(
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold)
)

/** Text face: Inter (OFL, bundled). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private fun display(size: Int, line: Int, weight: FontWeight, tracking: Double = -0.01) = TextStyle(
    fontFamily = Jakarta, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em
)

private fun text(size: Double, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em
)

val AppTypography = Typography(
    displayLarge = display(52, 58, FontWeight.ExtraBold, -0.03),
    displayMedium = display(42, 48, FontWeight.ExtraBold, -0.025),
    displaySmall = display(34, 40, FontWeight.ExtraBold, -0.02),
    headlineLarge = display(30, 36, FontWeight.Bold, -0.02),
    headlineMedium = display(26, 32, FontWeight.Bold, -0.015),
    headlineSmall = display(22, 28, FontWeight.Bold),
    titleLarge = display(19, 25, FontWeight.Bold),
    titleMedium = display(16, 22, FontWeight.SemiBold, 0.0),
    titleSmall = display(14, 20, FontWeight.SemiBold, 0.0),
    bodyLarge = text(16.0, 24),
    bodyMedium = text(14.0, 21),
    bodySmall = text(12.5, 18),
    labelLarge = text(14.0, 20, FontWeight.SemiBold, 0.005),
    labelMedium = text(12.0, 16, FontWeight.Medium, 0.01),
    labelSmall = text(11.0, 14, FontWeight.SemiBold, 0.02)
)

/** Small-caps style section label ("TODAY'S CHECK-IN"). */
val OverlineStyle = TextStyle(
    fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.12.em
)

/** Tabular numerals for live values that change every frame (no jitter). */
val NumericStyle = TextStyle(
    fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum", letterSpacing = (-0.02).em
)
