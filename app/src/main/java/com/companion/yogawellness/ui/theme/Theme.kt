package com.companion.yogawellness.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Score band colours shared by the overlay, cards and charts. */
object ScoreColors {
    val Green = Color(0xFF2E9E5B)
    val Amber = Color(0xFFE0A100)
    val Red = Color(0xFFD64545)
    val Neutral = Color(0xFF9AA5A8)
}

private val Teal = Color(0xFF0F5257)
private val TealLight = Color(0xFF7FC4C0)
private val Sand = Color(0xFFF4F1E8)
private val Clay = Color(0xFFB5654A)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE8E5),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = Clay,
    onSecondary = Color.White,
    background = Sand,
    onBackground = Color(0xFF1B1C1A),
    surface = Color(0xFFFFFCF6),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFE6E2D8),
    onSurfaceVariant = Color(0xFF4A4740)
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF0F5257),
    onPrimaryContainer = Color(0xFFCDE8E5),
    secondary = Color(0xFFF0A58C),
    background = Color(0xFF121413),
    surface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFF2A2D2B)
)

@Composable
fun YogaWellnessTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
