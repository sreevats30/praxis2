package com.companion.yogawellness.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.CameraAngle
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.MetricTile
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.components.Pill
import com.companion.yogawellness.ui.components.PoseGlyph
import com.companion.yogawellness.ui.components.ProgressTrack
import com.companion.yogawellness.ui.components.RingGauge
import com.companion.yogawellness.ui.components.SectionHeader
import com.companion.yogawellness.ui.components.SoftButton
import com.companion.yogawellness.ui.consent.NON_DIAGNOSTIC_STATEMENT
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.wellness.formatEpochDay
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.Questionnaires
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Today
// ---------------------------------------------------------------------------------------------

@Composable
internal fun TodayCard(today: DailyCheckInEntity?, activity: ActivitySummary, onCheckIn: () -> Unit) {
    PanelCard(elevation = 18.dp, spacing = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline("Today's check-in")
                Text(
                    if (today == null) "How are you today?" else "Your vitals today",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (activity.streakDays > 0) {
                Pill(
                    "${activity.streakDays}-day streak",
                    icon = Icons.Rounded.LocalFireDepartment,
                    color = Brand.Stress
                )
            }
        }
        if (today != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                VitalRing("Mood", today.mood, Brand.Mood, Icons.Rounded.SentimentSatisfied)
                VitalRing("Stress", today.stress, Brand.Stress, Icons.Rounded.Bolt)
                VitalRing("Sleep", today.sleepQuality, Brand.Sleep, Icons.Rounded.Bedtime)
            }
            SoftButton("Update check-in", onCheckIn, Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(Icons.Rounded.MonitorHeart, Brand.Mind, size = 48.dp, filled = true)
                Text(
                    "Mood, stress and sleep — three sliders, under 30 seconds. It powers your trends and insights.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            GradientButton("Check in now", onCheckIn, Modifier.fillMaxWidth(), icon = Icons.Rounded.MonitorHeart)
        }
        WeekStrip(activity)
    }
}

@Composable
private fun VitalRing(label: String, value: Int, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RingGauge(
            value / 10f, color, Modifier.size(78.dp), stroke = 7.dp,
            brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.55f), color))
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$value", style = NumericStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize), color = MaterialTheme.colorScheme.onSurface)
                Text("/10", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.inkMuted, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Seven day dots: filled = practised or checked in; today outlined. */
@Composable
private fun WeekStrip(activity: ActivitySummary) {
    val activeCount = activity.last7.count { it.active }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = "Active on $activeCount of the last 7 days" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Last 7 days", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted, modifier = Modifier.weight(1f))
            Text("$activeCount / 7 active", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            activity.last7.forEachIndexed { i, d ->
                val isToday = i == activity.last7.lastIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        LocalDate.ofEpochDay(d.epochDay).dayOfWeek.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary else AppTheme.colors.inkMuted
                    )
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (d.active) Brush.linearGradient(listOf(Brand.Teal500, Brand.Teal600))
                                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                            )
                            .border(
                                if (isToday) 2.dp else 1.dp,
                                if (isToday) MaterialTheme.colorScheme.primary else AppTheme.colors.cardBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (d.active) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Practice launcher
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PracticeLaunchCard(profile: ToleranceProfile, onProfile: (ToleranceProfile) -> Unit, onStart: () -> Unit) {
    val c = AppTheme.colors
    PanelCard(
        color = c.heroStart,
        brush = Brush.linearGradient(listOf(c.heroStart, c.heroMid, Brand.Teal700)),
        borderColor = null,
        elevation = 16.dp,
        contentPadding = PaddingValues(20.dp),
        spacing = 16.dp
    ) {
        Row {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("AI posture coach", icon = Icons.Rounded.AutoAwesome, iconTint = Brand.Gold)
                Text("Start guided practice", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(
                    "Live alignment scoring from 33 body landmarks, with a calm voice coach.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.74f)
                )
            }
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Brand.Teal400.copy(alpha = 0.28f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                PoseGlyph(Asana.VIRABHADRASANA_II, Modifier.size(96.dp))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPill("Live scoring", icon = Icons.Rounded.Speed)
            GlassPill("Voice coach", icon = Icons.Rounded.RecordVoiceOver)
            GlassPill("Frames never saved", icon = Icons.Rounded.Lock)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Alignment tolerance", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
            ToleranceSelector(profile, onProfile)
        }
        GradientButton(
            "Start practice", onStart, Modifier.fillMaxWidth(),
            icon = Icons.Rounded.PlayArrow,
            brush = Brush.horizontalGradient(listOf(Brand.Mint, Brand.Cyan)),
            contentColor = Brand.Navy900
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = Brand.Mint, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Text(
                "Prop the phone upright 2–3 m away, stand in Tadasana to calibrate, then move into a pose.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/** Segmented control on glass. */
@Composable
private fun ToleranceSelector(selected: ToleranceProfile, onSelect: (ToleranceProfile) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToleranceProfile.entries.forEach { p ->
            val on = p == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(p) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    p.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) Brand.Navy900 else Color.White.copy(alpha = 0.82f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// This week
// ---------------------------------------------------------------------------------------------

@Composable
internal fun WeekMetrics(a: ActivitySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("This week", overline = "Last 7 days")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                Icons.Rounded.Timer, "Practice", "${a.weekMinutes.roundToInt()} min", Brand.Practice, Modifier.weight(1f),
                sub = "${a.weekSessions} session" + if (a.weekSessions == 1) "" else "s"
            )
            MetricTile(
                Icons.Rounded.Accessibility, "Alignment", a.weekQuality?.let { "${it.roundToInt()}%" } ?: "–",
                AppTheme.colors.success, Modifier.weight(1f), sub = "avg score"
            )
            MetricTile(
                Icons.Rounded.LocalFireDepartment, "Streak", "${a.streakDays}", Brand.Stress, Modifier.weight(1f),
                sub = if (a.streakDays == 1) "day active" else "days active"
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Fortnightly reflections
// ---------------------------------------------------------------------------------------------

@Composable
internal fun ReflectionCard(
    type: QuestionnaireType,
    latest: QuestionnaireEntity?,
    todayEpochDay: Long,
    onStart: (QuestionnaireType) -> Unit
) {
    val due = Questionnaires.isDue(type, latest?.epochDay, todayEpochDay)
    val accent = if (type == QuestionnaireType.PHQ9) Brand.Mood else AppTheme.colors.info
    PanelCard(spacing = 14.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(if (type == QuestionnaireType.PHQ9) Icons.Rounded.Psychology else Icons.Rounded.Spa, accent, size = 46.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(type.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${type.items.size} questions · every ${type.intervalDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.inkMuted
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                latest == null -> Pill("Not started yet", color = MaterialTheme.colorScheme.primary)
                due -> Pill("Due now", icon = Icons.Rounded.Schedule, color = AppTheme.colors.warning)
                else -> Pill("Up to date", icon = Icons.Rounded.CheckCircle, color = AppTheme.colors.success)
            }
            Spacer(Modifier.weight(1f))
            latest?.let {
                Text("Last: ${formatEpochDay(it.epochDay)}", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted)
            }
        }
        if (latest != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "Self-reflection score",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${latest.totalScore}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(" / ${type.maxScore}", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted)
                }
                ProgressTrack(latest.totalScore.toFloat() / type.maxScore, accent, height = 6.dp)
            }
        }
        if (due) {
            SoftButton("Start ${type.shortName}", { onStart(type) }, Modifier.fillMaxWidth(), color = accent)
        } else if (latest != null) {
            Text(
                "Next ${type.shortName} on ${formatEpochDay(Questionnaires.nextDueEpochDay(type, latest.epochDay)!!)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.inkMuted
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Insights teaser
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InsightsTeaser(a: ActivitySummary, onOpen: () -> Unit) {
    PanelCard(onClick = onOpen, spacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(Icons.Rounded.Insights, MaterialTheme.colorScheme.primary, size = 46.dp, filled = true)
            Column(Modifier.weight(1f)) {
                Overline("Insights")
                Text("Wellness dashboard", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        val hasData = a.mood14.any { it != null } || a.stress14.any { it != null }
        if (hasData) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Last 14 days", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted, modifier = Modifier.weight(1f))
                    LegendDot("Mood", Brand.Mood)
                    LegendDot("Stress", Brand.Stress)
                }
                Sparklines(a.mood14, a.stress14)
            }
        } else {
            Text(
                "Check in daily to see your mood and stress trend here.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.inkMuted
            )
        }
        Text(
            "Weekly trends, left-vs-right balance, honest statistics and a 1-page PDF report.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Sparklines(mood: List<Int?>, stress: List<Int?>) {
    val surface = MaterialTheme.colorScheme.surface
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { contentDescription = "Mood and stress over the last 14 days" }
    ) {
        val n = maxOf(mood.size, stress.size)
        if (n < 2) return@Canvas
        val padY = 6.dp.toPx()
        fun x(i: Int) = size.width * i / (n - 1)
        fun y(v: Int) = padY + (size.height - 2 * padY) * (1f - (v - 1) / 9f)
        for ((series, color) in listOf(stress to Brand.Stress, mood to Brand.Mood)) {
            val path = Path()
            var pen = false
            series.forEachIndexed { i, v ->
                if (v == null) { pen = false; return@forEachIndexed }
                if (pen) path.lineTo(x(i), y(v)) else path.moveTo(x(i), y(v))
                pen = true
            }
            drawPath(path, color, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val last = series.indexOfLast { it != null }
            if (last >= 0) {
                val p = Offset(x(last), y(series[last]!!))
                drawCircle(surface, 5.dp.toPx(), p)
                drawCircle(color, 3.5.dp.toPx(), p)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pose library
// ---------------------------------------------------------------------------------------------

@Composable
internal fun PoseCarousel(onPose: (Asana) -> Unit = {}) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(Asana.entries.toList()) { a -> PoseCard(a) { onPose(a) } }
    }
}

@Composable
private fun PoseCard(asana: Asana, onClick: () -> Unit) {
    val c = AppTheme.colors
    PanelCard(Modifier.width(184.dp), onClick = onClick, contentPadding = PaddingValues(10.dp), spacing = 10.dp, elevation = 6.dp) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(c.heroStart, c.heroMid, Brand.Teal700))),
            contentAlignment = Alignment.Center
        ) {
            PoseGlyph(asana, Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(94.dp))
            Pill(
                if (asana.isScored) "Scored" else "Calibration",
                Modifier.align(Alignment.TopStart).padding(8.dp),
                color = Color.White,
                container = Color.White.copy(alpha = 0.14f)
            )
        }
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Column {
                Text(asana.sanskritName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(asana.englishName, style = MaterialTheme.typography.labelMedium, color = c.inkMuted)
            }
            Pill(
                if (asana.cameraAngle == CameraAngle.FRONT) "Face camera" else "Side-on / 45°",
                icon = Icons.Rounded.CameraAlt,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                asana.safetyRule,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                minLines = 3
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Learn this pose", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Practice history
// ---------------------------------------------------------------------------------------------

@Composable
internal fun HistoryCard(onOpen: () -> Unit) {
    PanelCard(onClick = onOpen, spacing = 8.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(Icons.Rounded.History, Brand.Practice, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Practice history", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Every pose you hold for 2 seconds is saved, with its alignment, hold time and top coaching cue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.inkMuted
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Open history", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Footer
// ---------------------------------------------------------------------------------------------

@Composable
internal fun PrivacyFooter(onSettings: () -> Unit) {
    PanelCard(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), elevation = 0.dp, spacing = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Shield, MaterialTheme.colorScheme.primary, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text("Private by design", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("No internet permission · excluded from backups", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted)
            }
        }
        Text(NON_DIAGNOSTIC_STATEMENT, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SoftButton("Settings & privacy", onSettings, Modifier.fillMaxWidth(), icon = Icons.Rounded.Settings)
        Text(
            "Yoga Wellness Companion · " + String.format(Locale.US, "v%s", com.companion.yogawellness.BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
