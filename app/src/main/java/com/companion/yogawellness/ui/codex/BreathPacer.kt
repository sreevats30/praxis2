package com.companion.yogawellness.ui.codex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.codex.BreathPattern
import com.companion.yogawellness.codex.BreathStep
import com.companion.yogawellness.codex.PoseTimeline

fun breathWord(step: BreathStep): String = when (step) {
    BreathStep.INHALE -> "Inhale"
    BreathStep.HOLD -> "Hold"
    BreathStep.EXHALE -> "Exhale"
}

/**
 * A ring that expands over the inhale, holds, and contracts over the exhale (§5.4). The ring is
 * redrawn every frame from [time]; the word and the screen-reader text only change when the
 * breath step changes.
 */
@Composable
fun BreathPacer(pattern: BreathPattern, time: FloatState, modifier: Modifier = Modifier) {
    val step by remember(pattern) { derivedStateOf { PoseTimeline.breathStep(time.floatValue, pattern) } }
    val word = breathWord(step)
    val ring = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier.clearAndSetSemantics { contentDescription = "Breath pacer: $word. ${pattern.format()}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Canvas(Modifier.size(64.dp)) {
            val level = PoseTimeline.breathLevel(time.floatValue, pattern) // -1 empty … +1 full
            val maxR = size.minDimension / 2f
            drawCircle(track, maxR)
            drawCircle(ring.copy(alpha = 0.25f), maxR * (0.62f + 0.38f * (level + 1f) / 2f) + 6f)
            drawCircle(ring.copy(alpha = 0.85f), maxR * (0.55f + 0.45f * (level + 1f) / 2f))
            drawCircle(Color.White.copy(alpha = 0.9f), maxR * 0.12f)
        }
        Column {
            Text(word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(pattern.format(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
