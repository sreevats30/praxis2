package com.companion.yogawellness.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.ListRow
import com.companion.yogawellness.ui.components.PoseGlyph
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.RowIcon
import com.companion.yogawellness.ui.components.RowIconInset
import com.companion.yogawellness.ui.components.Section
import com.companion.yogawellness.ui.components.SegmentMeter
import com.companion.yogawellness.ui.components.Segmented
import com.companion.yogawellness.ui.components.Stat
import com.companion.yogawellness.ui.components.StatusLabel
import com.companion.yogawellness.ui.components.TextAction
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.consent.NON_DIAGNOSTIC_STATEMENT
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.Radius
import com.companion.yogawellness.ui.theme.Space
import com.companion.yogawellness.ui.wellness.formatEpochDay
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.DayActivity
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.Questionnaires
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Check-in
// ---------------------------------------------------------------------------------------------

@Composable
internal fun CheckInSection(today: DailyCheckInEntity?, activity: ActivitySummary, onCheckIn: () -> Unit) {
    val c = AppTheme.colors
    Section(
        title = "Check-in",
        divider = false,
        trailing = if (today != null) ({ TextAction("Edit", onCheckIn) }) else null,
        spacing = Space.L
    ) {
        if (today == null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How are you today?", style = MaterialTheme.typography.headlineMedium, color = c.text)
                Text(
                    "Rate your mood, stress and sleep. It takes about 30 seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
            }
            PrimaryButton("Check in now", onCheckIn, compact = true)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.XL)) {
                ScaleStat("Mood", today.mood, c.mood, Modifier.weight(1f))
                ScaleStat("Stress", today.stress, c.stress, Modifier.weight(1f))
                ScaleStat("Sleep", today.sleepQuality, c.sleep, Modifier.weight(1f))
            }
        }
        WeekStrip(activity)
    }
}

/** A 1–10 self-rating: value over a ten-segment scale in the series colour. */
@Composable
private fun ScaleStat(label: String, value: Int, color: Color, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(Space.S)) {
        Stat(label, "$value", unit = "/10", valueSize = 26.sp)
        SegmentMeter(value, color)
    }
}

/**
 * The last seven days as a calendar strip. A filled jade day means you practised; an outlined day
 * means you only checked in; an empty day had neither. Today's weekday is emphasised.
 */
@Composable
private fun WeekStrip(activity: ActivitySummary) {
    val c = AppTheme.colors
    val active = activity.last7.count { it.active }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = Space.XS)
            .semantics(mergeDescendants = true) {
                contentDescription = "Active on $active of the last 7 days: " +
                    "${activity.last7.count { it.practised }} with practice, ${activity.last7.count { it.checkedIn }} with a check-in"
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Last 7 days", style = MaterialTheme.typography.labelMedium, color = c.textTertiary, modifier = Modifier.weight(1f))
            Text("$active of 7 days active", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            activity.last7.forEachIndexed { i, d -> DayCell(d, isToday = i == activity.last7.lastIndex) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.L)) {
            LegendMark("Practised") { Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent)) }
            LegendMark("Checked in") { Box(Modifier.size(8.dp).border(1.5.dp, c.accent, CircleShape)) }
        }
    }
}

@Composable
private fun DayCell(d: DayActivity, isToday: Boolean) {
    val c = AppTheme.colors
    val date = LocalDate.ofEpochDay(d.epochDay)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) c.text else c.textTertiary
        )
        val fill = if (d.practised) c.accent else if (d.checkedIn) Color.Transparent else c.control
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(fill)
                .then(if (!d.practised && d.checkedIn) Modifier.border(1.5.dp, c.accent, CircleShape) else Modifier)
                .then(if (isToday && !d.active) Modifier.border(1.dp, c.lineStrong, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${date.dayOfMonth}",
                style = NumericStyle.copy(fontSize = 12.sp, fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium),
                color = when {
                    d.practised -> c.onAccent
                    d.checkedIn -> c.accent
                    isToday -> c.text
                    else -> c.textTertiary
                }
            )
        }
    }
}

@Composable
private fun LegendMark(label: String, mark: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        mark()
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textTertiary)
    }
}

// ---------------------------------------------------------------------------------------------
// Practice
// ---------------------------------------------------------------------------------------------

private fun toleranceNote(p: ToleranceProfile): String = when (p) {
    ToleranceProfile.BEGINNER -> "Joint targets are 35% wider — forgiving while you learn."
    ToleranceProfile.STANDARD -> "The default joint targets."
    ToleranceProfile.ADVANCED -> "Joint targets are 25% tighter, for precise practice."
}

@Composable
internal fun PracticeSection(
    profile: ToleranceProfile,
    onProfile: (ToleranceProfile) -> Unit,
    onStart: () -> Unit,
    onPose: (Asana) -> Unit
) {
    val c = AppTheme.colors
    Section(title = "Practice", spacing = Space.L) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Live posture coaching", style = MaterialTheme.typography.headlineMedium, color = c.text)
            Text(
                "The camera scores your alignment joint by joint while a voice coach talks you through each correction.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary
            )
        }
        PoseTiles(onPose)
        Column(verticalArrangement = Arrangement.spacedBy(Space.S)) {
            Text("Alignment tolerance", style = MaterialTheme.typography.titleSmall, color = c.text)
            Segmented(
                ToleranceProfile.entries.map { it.label },
                ToleranceProfile.entries.indexOf(profile),
                { onProfile(ToleranceProfile.entries[it]) },
                Modifier.fillMaxWidth(),
                fill = true
            )
            Text(toleranceNote(profile), style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Start practice", onStart, Modifier.fillMaxWidth(), icon = Icons.Outlined.Videocam)
            Text(
                "Prop the phone upright 2–3 m away, then stand in Tadasana to calibrate.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary
            )
        }
    }
}

/** The four poses scored live; each opens its Learn page. */
@Composable
private fun PoseTiles(onPose: (Asana) -> Unit) {
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.S)) {
        Text("Scored live", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.S)) {
            Asana.entries.filter { it.isScored }.forEach { a ->
                val name = a.englishName.removeSuffix(" Pose")
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Radius.M))
                        .clickable(role = Role.Button, onClickLabel = "Learn ${a.englishName}") { onPose(a) },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(Radius.M)).background(c.control),
                        contentAlignment = Alignment.Center
                    ) { PoseGlyph(a, Modifier.fillMaxSize().padding(10.dp)) }
                    Text(name, style = MaterialTheme.typography.labelMedium, color = c.textSecondary, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// This week
// ---------------------------------------------------------------------------------------------

@Composable
internal fun WeekSection(a: ActivitySummary, onHistory: () -> Unit) {
    Section(title = "This week", gutter = false, spacing = Space.S) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.Gutter), horizontalArrangement = Arrangement.spacedBy(Space.L)) {
            Stat(
                "Practice", "${a.weekMinutes.roundToInt()}", Modifier.weight(1f), unit = "min",
                caption = "${a.weekSessions} session" + if (a.weekSessions == 1) "" else "s"
            )
            Stat("Alignment", a.weekQuality?.let { "${it.roundToInt()}" } ?: "–", Modifier.weight(1f), unit = a.weekQuality?.let { "%" }, caption = "average")
            Stat("Streak", "${a.streakDays}", Modifier.weight(1f), unit = if (a.streakDays == 1) "day" else "days", caption = "in a row")
        }
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
internal fun ReflectionsSection(
    phq: QuestionnaireEntity?,
    gad: QuestionnaireEntity?,
    todayEpochDay: Long,
    onStart: (QuestionnaireType) -> Unit
) {
    Section(title = "Reflections", gutter = false, spacing = 0.dp) {
        ReflectionRow(QuestionnaireType.PHQ9, phq, todayEpochDay, onStart)
        Hairline(inset = Space.Gutter)
        ReflectionRow(QuestionnaireType.GAD7, gad, todayEpochDay, onStart)
    }
}

@Composable
private fun ReflectionRow(type: QuestionnaireType, latest: QuestionnaireEntity?, today: Long, onStart: (QuestionnaireType) -> Unit) {
    val due = Questionnaires.isDue(type, latest?.epochDay, today)
    val supporting = when {
        latest == null -> "${type.shortName} · ${type.items.size} questions, every 14 days"
        due -> "${type.shortName} · last ${latest.totalScore}/${type.maxScore} on ${formatEpochDay(latest.epochDay)}"
        else -> "${type.shortName} · ${latest.totalScore}/${type.maxScore} · next on " +
            formatEpochDay(Questionnaires.nextDueEpochDay(type, latest.epochDay)!!)
    }
    ListRow(
        type.title.substringBefore(" ("),
        supporting = supporting,
        trailing = {
            when {
                latest == null -> StatusLabel("Start", Tone.ACCENT)
                due -> StatusLabel("Due", Tone.WARNING)
                else -> StatusLabel("Done", Tone.NEUTRAL)
            }
        },
        onClick = if (due) ({ onStart(type) }) else null,
        onClickLabel = "Start ${type.shortName}"
    )
}

// ---------------------------------------------------------------------------------------------
// Trends
// ---------------------------------------------------------------------------------------------

private fun meanOf(values: List<Int?>): Double? = values.filterNotNull().takeIf { it.isNotEmpty() }?.average()

@Composable
internal fun TrendsSection(a: ActivitySummary, onOpen: () -> Unit) {
    val c = AppTheme.colors
    val hasData = a.mood14.any { it != null } || a.stress14.any { it != null }
    Section(
        title = "Mood and stress",
        trailing = { TextAction("See all", onOpen, Modifier.semantics { contentDescription = "Open Insights" }, trailingIcon = Icons.AutoMirrored.Outlined.KeyboardArrowRight) },
        spacing = Space.L
    ) {
        if (!hasData) {
            Text("Check in daily and your two-week trend appears here.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            return@Section
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.XL)) {
            WeekAverage("Mood", a.mood14.takeLast(7), a.mood14.dropLast(7), c.mood, Modifier.weight(1f))
            WeekAverage("Stress", a.stress14.takeLast(7), a.stress14.dropLast(7), c.stress, Modifier.weight(1f))
        }
        Sparklines(a.mood14, a.stress14, c.mood, c.stress)
        Text("Self-rated 1–10 over the last 14 days. Week averages compare with the 7 days before.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
    }
}

/** This week's average with the change from the previous week, worded rather than colour-coded. */
@Composable
private fun WeekAverage(label: String, week: List<Int?>, previous: List<Int?>, color: Color, modifier: Modifier) {
    val c = AppTheme.colors
    val now = meanOf(week)
    val before = meanOf(previous)
    val change = if (now != null && before != null) now - before else null
    val caption = when {
        now == null -> "no check-ins this week"
        change == null -> "7-day average"
        abs(change) < 0.25 -> "steady vs last week"
        else -> "${if (change > 0) "up" else "down"} ${String.format(Locale.US, "%.1f", abs(change))} vs last week"
    }
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(width = 10.dp, height = 2.dp).background(color))
            Text(label, style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        }
        Text(now?.let { String.format(Locale.US, "%.1f", it) } ?: "–", style = NumericStyle.copy(fontSize = 22.sp), color = c.text)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
    }
}

@Composable
private fun Sparklines(mood: List<Int?>, stress: List<Int?>, moodColor: Color, stressColor: Color) {
    val grid = AppTheme.colors.line
    val canvas = AppTheme.colors.canvas
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { contentDescription = "Mood and stress over the last 14 days" }
    ) {
        val n = maxOf(mood.size, stress.size)
        if (n < 2) return@Canvas
        val pad = 4.dp.toPx()
        fun x(i: Int) = pad + (size.width - 2 * pad) * i / (n - 1)
        fun y(v: Int) = pad + (size.height - 2 * pad) * (1f - (v - 1) / 9f)
        // Week boundary: the dashed line splits this week from the one before.
        val split = x(n - 7) - (size.width - 2 * pad) / (n - 1) / 2
        drawLine(grid, Offset(split, 0f), Offset(split, size.height), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        drawLine(grid, Offset(0f, y(1)), Offset(size.width, y(1)), strokeWidth = 1f)
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
                drawCircle(canvas, 4.5.dp.toPx(), p)
                drawCircle(color, 3.dp.toPx(), p)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Footer
// ---------------------------------------------------------------------------------------------

@Composable
internal fun Footer() {
    Column {
        Hairline()
        Text(
            "$NON_DIAGNOSTIC_STATEMENT Everything you record stays on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textTertiary,
            modifier = Modifier.padding(horizontal = Space.Gutter, vertical = Space.XL)
        )
    }
}
