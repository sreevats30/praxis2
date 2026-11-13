package com.companion.yogawellness.ui.wellness

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.ui.components.ClinicalTopBar
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.components.Pill
import com.companion.yogawellness.ui.components.Reveal
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
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

    LaunchedEffect(existing) {
        val e = existing
        if (e != null && !loaded) {
            mood = e.mood; stress = e.stress; sleep = e.sleepQuality; pre = e.preTension; post = e.postTension
            loaded = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ClinicalTopBar(
                title = "Daily check-in",
                subtitle = formatEpochDay(vm.todayEpochDay),
                onBack = onDone,
                bottom = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPill("Under 30 seconds", icon = Icons.Rounded.Timer)
                        if (existing != null) GlassPill("Logged today", icon = Icons.Rounded.CheckCircle)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Just move the sliders — there are no right answers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Reveal(0) { ScaleCard("Mood", Icons.Rounded.SentimentSatisfied, Brand.Mood, mood, "Low", "Great", ::moodWord) { mood = it } }
            Reveal(1) { ScaleCard("Stress", Icons.Rounded.Bolt, Brand.Stress, stress, "Calm", "Very stressed", ::stressWord) { stress = it } }
            Reveal(2) { ScaleCard("Sleep quality", Icons.Rounded.Bedtime, Brand.Sleep, sleep, "Poor", "Excellent", ::sleepWord) { sleep = it } }
            Reveal(3) {
                PanelCard(spacing = 14.dp, elevation = 6.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Rounded.SelfImprovement, Brand.Practice, size = 40.dp)
                        Column {
                            Overline("Optional", color = AppTheme.colors.inkMuted)
                            Text("Body tension", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    TensionPicker("Before practice", pre) { pre = it }
                    TensionPicker("After practice", post) { post = it }
                    val p = pre
                    val q = post
                    if (p != null && q != null) TensionDelta(p, q)
                }
            }
            val m = mood
            val st = stress
            val sl = sleep
            val complete = m != null && st != null && sl != null
            GradientButton(
                if (existing != null) "Update today's check-in" else "Save check-in",
                { if (m != null && st != null && sl != null) vm.saveCheckIn(m, st, sl, pre, post, onDone) },
                Modifier.fillMaxWidth(),
                enabled = complete,
                icon = Icons.Rounded.CheckCircle
            )
            if (!complete) {
                Text(
                    "Rate mood, stress and sleep to save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.inkMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun moodWord(v: Int) = when (v) { in 1..2 -> "Very low"; in 3..4 -> "Low"; in 5..6 -> "Okay"; in 7..8 -> "Good"; else -> "Great" }
private fun stressWord(v: Int) = when (v) { in 1..2 -> "Calm"; in 3..4 -> "Mild"; in 5..6 -> "Moderate"; in 7..8 -> "High"; else -> "Very high" }
private fun sleepWord(v: Int) = when (v) { in 1..2 -> "Very poor"; in 3..4 -> "Poor"; in 5..6 -> "Fair"; in 7..8 -> "Good"; else -> "Excellent" }

@Composable
private fun ScaleCard(
    label: String,
    icon: ImageVector,
    color: Color,
    value: Int?,
    low: String,
    high: String,
    word: (Int) -> String,
    onChange: (Int) -> Unit
) {
    PanelCard(spacing = 6.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(icon, color, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    value?.let(word) ?: "Not rated yet",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (value != null) color else AppTheme.colors.inkMuted
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value?.toString() ?: "–",
                    style = NumericStyle.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize),
                    color = if (value != null) MaterialTheme.colorScheme.onSurface else AppTheme.colors.inkMuted
                )
                Text(" / 10", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
        // Unrated: an empty track with no thumb; tapping or dragging anywhere sets the value.
        val faint = color.copy(alpha = 0.16f)
        Slider(
            value = value?.toFloat() ?: 1f,
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = if (value != null) color else Color.Transparent,
                activeTrackColor = if (value != null) color else faint,
                inactiveTrackColor = faint,
                activeTickColor = if (value != null) Color.White.copy(alpha = 0.6f) else color.copy(alpha = 0.45f),
                inactiveTickColor = color.copy(alpha = 0.45f)
            ),
            modifier = if (value == null) Modifier.semantics { stateDescription = "$label not rated yet" } else Modifier
        )
        Row {
            Text(low, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted, modifier = Modifier.weight(1f))
            Text(high, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted)
        }
    }
}

@Composable
private fun TensionPicker(label: String, value: Int?, onChange: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(value?.let { "$it / 10" } ?: "Tap to rate", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..10).forEach { n ->
                val on = value == n
                val heat = lerpColor(Brand.Teal500, Brand.Stress, (n - 1) / 9f)
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.82f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) Brush.verticalGradient(listOf(heat, heat.copy(alpha = 0.8f))) else Brush.verticalGradient(listOf(heat.copy(alpha = 0.10f), heat.copy(alpha = 0.10f))))
                        .border(1.dp, if (on) heat else Color.Transparent, RoundedCornerShape(10.dp))
                        .selectable(selected = on, role = Role.RadioButton) { onChange(if (on) null else n) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$n",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f
)

@Composable
private fun TensionDelta(pre: Int, post: Int) {
    val d = post - pre
    val (icon, color) = when {
        d < 0 -> Icons.AutoMirrored.Rounded.TrendingDown to AppTheme.colors.success
        d > 0 -> Icons.AutoMirrored.Rounded.TrendingUp to AppTheme.colors.warning
        else -> Icons.AutoMirrored.Rounded.TrendingFlat to AppTheme.colors.inkMuted
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.09f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Text(
            "Self-rated tension went from $pre to $post after today's practice.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Pill(if (d > 0) "+$d" else "$d", color = color)
    }
}
