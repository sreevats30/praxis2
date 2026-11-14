package com.companion.yogawellness.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.CameraAngle
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.ListRow
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.PoseGlyph
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.RowIcon
import com.companion.yogawellness.ui.components.Segmented
import com.companion.yogawellness.ui.components.Stat
import com.companion.yogawellness.ui.components.Tag
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.components.VerticalHairline
import com.companion.yogawellness.ui.consent.NON_DIAGNOSTIC_STATEMENT
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.wellness.formatEpochDay
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.Questionnaires
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Check-in
// ---------------------------------------------------------------------------------------------

@Composable
internal fun CheckInPanel(today: DailyCheckInEntity?, activity: ActivitySummary, onCheckIn: () -> Unit) {
    val c = AppTheme.colors
    Panel(spacing = 16.dp) {
        if (today == null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("How are you today?", style = MaterialTheme.typography.titleMedium, color = c.text)
                Text("Mood, stress and sleep. Takes about 30 seconds.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            PrimaryButton("Check in now", onCheckIn, Modifier.fillMaxWidth())
        } else {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ScaleStat("Mood", today.mood, c.mood, Modifier.weight(1f))
                VerticalHairline()
                ScaleStat("Stress", today.stress, c.stress, Modifier.weight(1f))
                VerticalHairline()
                ScaleStat("Sleep", today.sleepQuality, c.sleep, Modifier.weight(1f))
            }
        }
        Hairline()
        WeekStrip(activity)
    }
}

/** A 1–10 self-rating: value over a ten-segment scale. */
@Composable
private fun ScaleStat(label: String, value: Int, color: Color, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat(label, "$value", unit = "/10")
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(10) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (i < value) color else AppTheme.colors.sunken)
                )
            }
        }
    }
}

/** Last seven days as slim bars: filled = practised or checked in. Today's label is emphasised. */
@Composable
private fun WeekStrip(activity: ActivitySummary) {
    val c = AppTheme.colors
    val active = activity.last7.count { it.active }
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "Active on $active of the last 7 days" },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Last 7 days", style = MaterialTheme.typography.labelMedium, color = c.textTertiary, modifier = Modifier.weight(1f))
            Text("$active of 7 days active", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            activity.last7.forEachIndexed { i, d ->
                val isToday = i == activity.last7.lastIndex
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (d.active) c.accent else c.sunken)
                    )
                    Text(
                        LocalDate.ofEpochDay(d.epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) c.text else c.textTertiary,
                        fontWeight = if (isToday) FontWeight.SemiBold else null
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Practice
// ---------------------------------------------------------------------------------------------

@Composable
internal fun PracticePanel(profile: ToleranceProfile, onProfile: (ToleranceProfile) -> Unit, onStart: () -> Unit) {
    val c = AppTheme.colors
    Panel(spacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(c.sunken),
                contentAlignment = Alignment.Center
            ) { PoseGlyph(Asana.VIRABHADRASANA_II, Modifier.size(48.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Posture practice", style = MaterialTheme.typography.titleMedium, color = c.text)
                Text(
                    "Live alignment scoring and voice cues for Tree, Warrior II, Triangle and Chair.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Alignment tolerance", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
            Segmented(
                ToleranceProfile.entries.map { it.label },
                ToleranceProfile.entries.indexOf(profile),
                { onProfile(ToleranceProfile.entries[it]) },
                Modifier.fillMaxWidth(),
                fill = true
            )
        }
        PrimaryButton("Start practice", onStart, Modifier.fillMaxWidth())
        Text(
            "Prop the phone upright 2–3 m away, then stand in Tadasana to calibrate.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textTertiary
        )
    }
}

// ---------------------------------------------------------------------------------------------
// This week
// ---------------------------------------------------------------------------------------------

@Composable
internal fun WeekPanel(a: ActivitySummary, onHistory: () -> Unit) {
    Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat(
                "Practice", "${a.weekMinutes.roundToInt()}", Modifier.weight(1f), unit = "min",
                caption = "${a.weekSessions} session" + if (a.weekSessions == 1) "" else "s"
            )
            VerticalHairline()
            Stat("Alignment", a.weekQuality?.let { "${it.roundToInt()}" } ?: "–", Modifier.weight(1f), unit = a.weekQuality?.let { "%" }, caption = "average")
            VerticalHairline()
            Stat("Streak", "${a.streakDays}", Modifier.weight(1f), unit = if (a.streakDays == 1) "day" else "days", caption = "in a row")
        }
        Hairline()
        ListRow(
            "Practice history",
            supporting = "Every pose held for 2 s, with alignment and cues",
            leading = { RowIcon(Icons.Outlined.History) },
            onClick = onHistory
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Reflections
// ---------------------------------------------------------------------------------------------

@Composable
internal fun ReflectionsPanel(
    phq: QuestionnaireEntity?,
    gad: QuestionnaireEntity?,
    todayEpochDay: Long,
    onStart: (QuestionnaireType) -> Unit
) {
    Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
        ReflectionRow(QuestionnaireType.PHQ9, phq, todayEpochDay, onStart)
        Hairline(inset = 16.dp)
        ReflectionRow(QuestionnaireType.GAD7, gad, todayEpochDay, onStart)
    }
}

@Composable
private fun ReflectionRow(type: QuestionnaireType, latest: QuestionnaireEntity?, today: Long, onStart: (QuestionnaireType) -> Unit) {
    val due = Questionnaires.isDue(type, latest?.epochDay, today)
    val supporting = when {
        latest == null -> "${type.shortName} · ${type.items.size} questions · not started"
        due -> "${type.shortName} · last ${latest.totalScore}/${type.maxScore} on ${formatEpochDay(latest.epochDay)}"
        else -> "${type.shortName} · ${latest.totalScore}/${type.maxScore} · next on " +
            formatEpochDay(Questionnaires.nextDueEpochDay(type, latest.epochDay)!!)
    }
    ListRow(
        type.title.substringBefore(" ("),
        supporting = supporting,
        trailing = {
            when {
                latest == null -> Tag("Start", tone = Tone.ACCENT)
                due -> Tag("Due", tone = Tone.WARNING)
                else -> Tag("Done", tone = Tone.POSITIVE)
            }
        },
        onClick = if (due) ({ onStart(type) }) else null,
        onClickLabel = "Start ${type.shortName}"
    )
}

// ---------------------------------------------------------------------------------------------
// Trends
// ---------------------------------------------------------------------------------------------

@Composable
internal fun TrendsPanel(a: ActivitySummary, onOpen: () -> Unit) {
    val c = AppTheme.colors
    val hasData = a.mood14.any { it != null } || a.stress14.any { it != null }
    Panel(onClick = onOpen, spacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Mood and stress", style = MaterialTheme.typography.titleMedium, color = c.text)
                Text("Last 14 days · self-rated 1–10", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            if (hasData) {
                Legend("Mood", c.mood)
                Legend("Stress", c.stress, Modifier.padding(start = 12.dp))
            }
        }
        if (hasData) {
            Sparklines(a.mood14, a.stress14, c.mood, c.stress)
        } else {
            Text("Check in daily to see your trend here.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
        Text("Open Insights for weekly trends, statistics and the PDF report", style = MaterialTheme.typography.labelLarge, color = c.accent)
    }
}

@Composable
private fun Legend(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(width = 10.dp, height = 2.dp).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textSecondary)
    }
}

@Composable
private fun Sparklines(mood: List<Int?>, stress: List<Int?>, moodColor: Color, stressColor: Color) {
    val grid = AppTheme.colors.border
    val surface = AppTheme.colors.surface
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { contentDescription = "Mood and stress over the last 14 days" }
    ) {
        val n = maxOf(mood.size, stress.size)
        if (n < 2) return@Canvas
        val pad = 4.dp.toPx()
        fun x(i: Int) = size.width * i / (n - 1)
        fun y(v: Int) = pad + (size.height - 2 * pad) * (1f - (v - 1) / 9f)
        for (v in listOf(1, 5, 10)) drawLine(grid, Offset(0f, y(v)), Offset(size.width, y(v)), strokeWidth = 1f)
        for ((series, color) in listOf(stress to stressColor, mood to moodColor)) {
            val path = Path()
            var pen = false
            series.forEachIndexed { i, v ->
                if (v == null) { pen = false; return@forEachIndexed }
                if (pen) path.lineTo(x(i), y(v)) else path.moveTo(x(i), y(v))
                pen = true
            }
            drawPath(path, color, style = Stroke(1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val last = series.indexOfLast { it != null }
            if (last >= 0) {
                val p = Offset(x(last), y(series[last]!!))
                drawCircle(surface, 4.dp.toPx(), p)
                drawCircle(color, 3.dp.toPx(), p)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Poses
// ---------------------------------------------------------------------------------------------

/** Lets a row scroll edge to edge inside a padded column. */
private fun Modifier.bleed(h: Dp) = layout { measurable, constraints ->
    val extra = h.roundToPx() * 2
    val p = measurable.measure(constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra))
    layout(p.width - extra, p.height) { p.place(-h.roundToPx(), 0) }
}

@Composable
internal fun PoseRow(onPose: (Asana) -> Unit) {
    LazyRow(
        Modifier.bleed(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(Asana.entries.toList()) { a -> PoseCard(a) { onPose(a) } }
    }
}

@Composable
private fun PoseCard(asana: Asana, onClick: () -> Unit) {
    val c = AppTheme.colors
    Panel(Modifier.width(156.dp), onClick = onClick, contentPadding = PaddingValues(10.dp), spacing = 10.dp) {
        Box(
            Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(8.dp)).background(c.sunken),
            contentAlignment = Alignment.Center
        ) { PoseGlyph(asana, Modifier.size(92.dp)) }
        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(asana.sanskritName, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1)
            Text(asana.englishName, style = MaterialTheme.typography.bodySmall, color = c.textTertiary, maxLines = 1)
        }
        Row(Modifier.padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tag(if (asana.isScored) "Scored" else "Calibration", tone = if (asana.isScored) Tone.ACCENT else Tone.NEUTRAL)
            Tag(if (asana.cameraAngle == CameraAngle.FRONT) "Front" else "Side-on")
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Footer
// ---------------------------------------------------------------------------------------------

@Composable
internal fun Footer() {
    Text(
        "$NON_DIAGNOSTIC_STATEMENT Everything you record stays on this phone.",
        style = MaterialTheme.typography.bodySmall,
        color = AppTheme.colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp)
    )
}
