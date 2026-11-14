package com.companion.yogawellness.ui.wellness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.ScreenColumn
import com.companion.yogawellness.ui.components.SectionLabel
import com.companion.yogawellness.ui.components.Tag
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import kotlin.math.roundToInt

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

    AppScreen(title = "Daily check-in", onBack = onDone) { padding ->
        ScreenColumn(padding) {
            Text(
                "${formatEpochDay(vm.todayEpochDay)} · rate each from 1 to 10. There are no right answers.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
                ScaleRow("Mood", mood, "Low", "Great", ::moodWord) { mood = it }
                Hairline()
                ScaleRow("Stress", stress, "Calm", "Very stressed", ::stressWord) { stress = it }
                Hairline()
                ScaleRow("Sleep quality", sleep, "Poor", "Excellent", ::sleepWord) { sleep = it }
            }

            SectionLabel("Body tension (optional)")
            Panel(spacing = 16.dp) {
                TensionPicker("Before practice", pre) { pre = it }
                TensionPicker("After practice", post) { post = it }
                val p = pre
                val q = post
                if (p != null && q != null) {
                    Hairline()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Self-rated tension went from $p to $q after today's practice.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.text,
                            modifier = Modifier.weight(1f)
                        )
                        val d = q - p
                        Tag(if (d > 0) "+$d" else "$d", tone = if (d < 0) Tone.POSITIVE else if (d > 0) Tone.WARNING else Tone.NEUTRAL)
                    }
                }
            }

            val m = mood
            val st = stress
            val sl = sleep
            val complete = m != null && st != null && sl != null
            PrimaryButton(
                if (existing != null) "Update today's check-in" else "Save check-in",
                { if (m != null && st != null && sl != null) vm.saveCheckIn(m, st, sl, pre, post, onDone) },
                Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = complete
            )
            if (!complete) {
                Text(
                    "Rate mood, stress and sleep to save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

private fun moodWord(v: Int) = when (v) { in 1..2 -> "Very low"; in 3..4 -> "Low"; in 5..6 -> "Okay"; in 7..8 -> "Good"; else -> "Great" }
private fun stressWord(v: Int) = when (v) { in 1..2 -> "Calm"; in 3..4 -> "Mild"; in 5..6 -> "Moderate"; in 7..8 -> "High"; else -> "Very high" }
private fun sleepWord(v: Int) = when (v) { in 1..2 -> "Very poor"; in 3..4 -> "Poor"; in 5..6 -> "Fair"; in 7..8 -> "Good"; else -> "Excellent" }

@Composable
private fun ScaleRow(label: String, value: Int?, low: String, high: String, word: (Int) -> String, onChange: (Int) -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = c.text)
                Text(value?.let(word) ?: "Not rated yet", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value?.toString() ?: "–", style = NumericStyle.copy(fontSize = 26.sp), color = if (value != null) c.text else c.textTertiary)
                Text(" /10", style = MaterialTheme.typography.labelLarge, color = c.textTertiary, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        // Unrated: an empty track with no thumb; tapping or dragging anywhere sets the value.
        Slider(
            value = value?.toFloat() ?: 1f,
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = if (value != null) c.accent else Color.Transparent,
                activeTrackColor = if (value != null) c.accent else c.sunken,
                inactiveTrackColor = c.sunken,
                activeTickColor = if (value != null) c.onAccent.copy(alpha = 0.7f) else c.borderStrong,
                inactiveTickColor = c.borderStrong
            ),
            modifier = if (value == null) Modifier.semantics { stateDescription = "$label not rated yet" } else Modifier
        )
        Row {
            Text(low, style = MaterialTheme.typography.labelMedium, color = c.textTertiary, modifier = Modifier.weight(1f))
            Text(high, style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        }
    }
}

@Composable
private fun TensionPicker(label: String, value: Int?, onChange: (Int?) -> Unit) {
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
            Text(value?.let { "$it /10" } ?: "Not rated", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..10).forEach { n ->
                val on = value == n
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.9f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) c.accent else c.sunken)
                        .selectable(selected = on, role = Role.RadioButton) { onChange(if (on) null else n) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("$n", style = MaterialTheme.typography.labelLarge, color = if (on) c.onAccent else c.textSecondary)
                }
            }
        }
    }
}
