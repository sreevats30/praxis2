package com.companion.yogawellness.ui.wellness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.ScaleSelector
import com.companion.yogawellness.ui.components.Section
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.Space

/** Daily check-in, under 30 seconds (§9.2): mood, stress, sleep + optional pre/post tension. */
@Composable
fun CheckInScreen(onDone: () -> Unit, vm: WellnessViewModel = viewModel()) {
    val existing by vm.todayCheckIn.collectAsStateWithLifecycle()
    // Zero state: nothing is pre-filled. Each scale is unrated until the user sets it.
    var mood by rememberSaveable { mutableStateOf<Int?>(null) }
    var stress by rememberSaveable { mutableStateOf<Int?>(null) }
    var sleep by rememberSaveable { mutableStateOf<Int?>(null) }
    var pre by rememberSaveable { mutableStateOf<Int?>(null) }
    var post by rememberSaveable { mutableStateOf<Int?>(null) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    val c = AppTheme.colors

    LaunchedEffect(existing) {
        val e = existing
        if (e != null && !loaded) {
            mood = e.mood; stress = e.stress; sleep = e.sleepQuality; pre = e.preTension; post = e.postTension
            loaded = true
        }
    }

    AppScreen(
        title = "Daily check-in",
        onBack = onDone,
        largeTitle = true,
        headline = "How are you today?",
        subtitle = "${formatEpochDay(vm.todayEpochDay)} · rate each from 1 to 10. There are no right answers."
    ) {
        Section(divider = false, spacing = Space.XL) {
            ScaleRow("Mood", mood, "Low", "Great", c.mood, ::moodWord) { mood = it }
            ScaleRow("Stress", stress, "Calm", "Very stressed", c.stress, ::stressWord) { stress = it }
            ScaleRow("Sleep quality", sleep, "Poor", "Excellent", c.sleep, ::sleepWord) { sleep = it }
        }

        Section(title = "Body tension", spacing = Space.L) {
            Text(
                "Optional. Rate how tense your body feels before and after practice; tap a number again to clear it.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary
            )
            TensionPicker("Before practice", pre) { pre = it }
            TensionPicker("After practice", post) { post = it }
            val p = pre
            val q = post
            if (p != null && q != null) {
                val d = q - p
                Notice(
                    "Self-rated tension went from $p to $q after today's practice.",
                    tone = if (d < 0) Tone.POSITIVE else if (d > 0) Tone.WARNING else Tone.NEUTRAL,
                    title = if (d == 0) "No change" else "${if (d > 0) "+" else "−"}${kotlin.math.abs(d)} after practice"
                )
            }
        }

        val m = mood
        val st = stress
        val sl = sleep
        val complete = m != null && st != null && sl != null
        Section(spacing = Space.S) {
            PrimaryButton(
                if (existing != null) "Update today's check-in" else "Save check-in",
                { if (m != null && st != null && sl != null) vm.saveCheckIn(m, st, sl, pre, post, onDone) },
                Modifier.fillMaxWidth(),
                enabled = complete
            )
            if (!complete) {
                Text("Rate mood, stress and sleep to save.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
        }
    }
}

private fun moodWord(v: Int) = when (v) { in 1..2 -> "Very low"; in 3..4 -> "Low"; in 5..6 -> "Okay"; in 7..8 -> "Good"; else -> "Great" }
private fun stressWord(v: Int) = when (v) { in 1..2 -> "Calm"; in 3..4 -> "Mild"; in 5..6 -> "Moderate"; in 7..8 -> "High"; else -> "Very high" }
private fun sleepWord(v: Int) = when (v) { in 1..2 -> "Very poor"; in 3..4 -> "Poor"; in 5..6 -> "Fair"; in 7..8 -> "Good"; else -> "Excellent" }

@Composable
private fun ScaleRow(label: String, value: Int?, low: String, high: String, color: Color, word: (Int) -> String, onChange: (Int) -> Unit) {
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = c.text)
                Text(value?.let(word) ?: "Not rated yet", style = MaterialTheme.typography.bodySmall, color = if (value != null) c.textSecondary else c.textTertiary)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value?.toString() ?: "–", style = NumericStyle.copy(fontSize = 26.sp), color = if (value != null) c.text else c.textTertiary)
                Text(" /10", style = MaterialTheme.typography.bodySmall, color = c.textTertiary, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        ScaleSelector(value, { v -> v?.let(onChange) }, color, label)
        Row {
            Text(low, style = MaterialTheme.typography.labelSmall, color = c.textTertiary, modifier = Modifier.weight(1f))
            Text(high, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
        }
    }
}

@Composable
private fun TensionPicker(label: String, value: Int?, onChange: (Int?) -> Unit) {
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.S)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
            Text(value?.let { "$it /10" } ?: "Not rated", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        }
        ScaleSelector(value, onChange, c.inverse, label, rangeSemantics = false, clearable = true, ramp = false)
    }
}
