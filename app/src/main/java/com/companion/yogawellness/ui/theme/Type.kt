package com.companion.yogawellness.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.R

/*
 * Two voices, each with one job (both SIL OFL, bundled — the app has no internet permission):
 *  - Newsreader, a text serif, for the things you read as language: screen titles, the greeting,
 *    pose names, and the sentence-length findings on Insights. Sanskrit names use its italic.
 *  - Inter for everything you operate or measure: labels, controls, lists and numbers (always with
 *    tabular figures, so values that change never jitter).
 */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val Newsreader = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_medium, FontWeight.Medium),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic)
)

private fun sans(size: Double, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em
)

private fun serif(size: Double, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = -0.01) = TextStyle(
    fontFamily = Newsreader, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em
)

val AppTypography = Typography(
    // Serif — titles and statements
    displayLarge = serif(40.0, 46, tracking = -0.02),
    displayMedium = serif(34.0, 40, tracking = -0.02),
    displaySmall = serif(30.0, 36, tracking = -0.015),
    headlineLarge = serif(27.0, 33),
    headlineMedium = serif(24.0, 30),
    headlineSmall = serif(20.0, 26),
    // Sans — interface
    titleLarge = sans(18.0, 24, FontWeight.SemiBold, -0.006),
    titleMedium = sans(16.0, 22, FontWeight.SemiBold, -0.004),
    titleSmall = sans(14.0, 20, FontWeight.SemiBold),
    bodyLarge = sans(16.0, 24, FontWeight.Normal),
    bodyMedium = sans(14.0, 20, FontWeight.Normal),
    bodySmall = sans(13.0, 18, FontWeight.Normal),
    labelLarge = sans(14.0, 20, FontWeight.SemiBold),
    labelMedium = sans(12.0, 16, FontWeight.Medium),
    labelSmall = sans(11.0, 14, FontWeight.Medium, 0.01)
)

/** Section headings ("Check-in", "This week"): small, semibold, secondary ink, sentence case. */
val SectionStyle = sans(13.0, 18, FontWeight.SemiBold, 0.005)

/** Sanskrit names and other transliterated terms. */
val SanskritStyle = TextStyle(fontFamily = Newsreader, fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 20.sp)

/** Tabular figures for values that change (scores, timers, stats) so digits don't jitter. */
val NumericStyle = TextStyle(
    fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", letterSpacing = (-0.01).em
)
