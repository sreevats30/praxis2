package com.companion.yogawellness.ui.dashboard

import android.content.ActivityNotFoundException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.stats.RankStatsEngine
import com.companion.yogawellness.ui.components.Callout
import com.companion.yogawellness.ui.components.ClinicalTopBar
import com.companion.yogawellness.ui.components.GlassIconButton
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.MetricTile
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.components.Pill
import com.companion.yogawellness.ui.components.Reveal
import com.companion.yogawellness.ui.profile.DemoBannerBar
import com.companion.yogawellness.ui.profile.ProfileSelectorBar
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.wellness.formatEpochDay
import com.companion.yogawellness.wellness.DashboardBuilder
import com.companion.yogawellness.wellness.DashboardData
import com.companion.yogawellness.wellness.InsightCard
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.SymmetryRow
import com.companion.yogawellness.wellness.WeeklyRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private fun f0(v: Double) = String.format(Locale.US, "%.0f", v)
private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)

@Composable
fun WellnessDashboardScreen(onBack: (() -> Unit)? = null, vm: DashboardViewModel = viewModel()) {
    val profileData by vm.profileData.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val export = {
        vm.exportReport(
            onShare = { intent ->
                try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { error = "No app available to share the PDF" }
            },
            onError = { error = it }
        )
    }
    DashboardScreenContent(
        profileData?.data, profileData?.demoLabel, error, onBack, export,
        profiles = {
            ProfileSelectorBar()
            DemoBannerBar()
        }
    )
}

/**
 * Stateless dashboard: [data] null while the statistics are computed; [demoLabel] non-null while a
 * demo persona is being viewed; [profiles] hosts the profile chips and demo banner.
 */
@Composable
fun DashboardScreenContent(
    data: DashboardData?,
    demoLabel: String?,
    error: String?,
    onBack: (() -> Unit)?,
    onExport: () -> Unit,
    profiles: @Composable () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ClinicalTopBar(
                title = "Wellness dashboard",
                subtitle = "Last ${DashboardBuilder.WINDOW_DAYS / 7} weeks · analysed on this phone",
                onBack = onBack,
                // No report to export until there is real data (a new account starts empty).
                actions = {
                    if (data != null && !data.isEmpty) GlassIconButton(Icons.Rounded.PictureAsPdf, "Export weekly PDF report", onExport)
                },
                bottom = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPill("On-device", icon = Icons.Rounded.Memory)
                        demoLabel?.let { GlassPill(it, icon = Icons.Rounded.Science, iconTint = Brand.Gold) }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            profiles()
            error?.let { Callout(it, Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error) }
            when {
                data == null -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Crunching your numbers…", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.inkMuted)
                    }
                }
                data.isEmpty -> EmptyState()
                else -> DashboardContent(data, onExport)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    PanelCard(spacing = 14.dp) {
        IconBadge(Icons.AutoMirrored.Rounded.ShowChart, MaterialTheme.colorScheme.primary, size = 56.dp, filled = true)
        Text("Your insights start here", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Log a few daily check-ins and practice sessions to see your trends — or load a demo persona in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DashboardContent(d: DashboardData, onExport: () -> Unit) {
    Reveal(0) { StatTiles(d) }
    Reveal(1) { TrendCard(d.weeks) }
    Reveal(2) { SymmetryCard(d) }
    if (d.insights.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            Overline("Statistics", color = AppTheme.colors.inkMuted)
            Text("What your data suggests", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
    }
    d.insights.forEachIndexed { i, card -> Reveal(3 + i) { InsightCardView(card) } }
    ReflectionCard(d)
    GradientButton("Export weekly PDF report", onExport, Modifier.fillMaxWidth(), icon = Icons.Rounded.PictureAsPdf)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun StatTiles(d: DashboardData) {
    val h = d.headline
    val q = h.meanQuality
    val p = h.previousWeekQuality
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                Icons.Rounded.Timer, "Practice this week", "${f0(h.practiceMinutes)} min", Brand.Practice, Modifier.weight(1f),
                sub = "${h.sessions} sessions"
            )
            MetricTile(
                Icons.Rounded.Accessibility, "Avg alignment", q?.let { "${f0(it)}%" } ?: "–", AppTheme.colors.success, Modifier.weight(1f),
                sub = if (q != null && p != null) "vs last week" else null,
                delta = if (q != null && p != null) "${if (q >= p) "+" else ""}${f0(q - p)}" else null,
                deltaPositive = if (q != null && p != null) q >= p else null
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                Icons.Rounded.EventAvailable, "Check-ins", "${h.checkInDays} / 7", Brand.Mood, Modifier.weight(1f),
                sub = "days logged this week"
            )
            val t = d.tension
            MetricTile(
                Icons.Rounded.SelfImprovement, "Body tension", t?.let { "${f1(it.meanBefore)} → ${f1(it.meanAfter)}" } ?: "–",
                Brand.Stress, Modifier.weight(1f), sub = t?.let { "before → after practice" }
            )
        }
    }
}

@Composable
private fun TrendCard(weeks: List<WeeklyRow>) {
    val colors = if (isSystemInDarkTheme()) ChartColors.Dark else ChartColors.Light
    val density = LocalDensity.current
    val renderer = remember(colors, density) { TrendChartRenderer(colors, density.density, density.density * density.fontScale) }
    val labels = weeks.map { LocalDate.ofEpochDay(it.weekStartEpochDay).format(SHORT) }
    var selected by remember(weeks) { mutableStateOf<Int?>(null) }
    var showTable by remember { mutableStateOf(false) }

    PanelCard(color = Color(colors.surface), spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.AutoMirrored.Rounded.ShowChart, MaterialTheme.colorScheme.primary, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Overline("${weeks.size} weeks", color = AppTheme.colors.inkMuted)
                Text("Weekly trend", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .semantics {
                    contentDescription = "Weekly practice quality, stress and mood over ${weeks.size} weeks. Tap a week for values."
                }
                .pointerInput(weeks) {
                    detectTapGestures { pos ->
                        val i = renderer.indexAt(pos.x, size.width.toFloat(), weeks.size)
                        selected = if (i == selected) null else i
                    }
                }
        ) {
            drawIntoCanvas { renderer.draw(it.nativeCanvas, size.width, size.height, weeks, labels, selected) }
        }
        val sel = selected
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(12.dp)
        ) {
            if (sel != null && sel < weeks.size) {
                val w = weeks[sel]
                Text(
                    "Week of ${labels[sel]}: quality ${w.practiceQuality?.let { f0(it) } ?: "–"} · stress ${w.meanStress?.let { f1(it) } ?: "–"}" +
                        " · mood ${w.meanMood?.let { f1(it) } ?: "–"} · ${f0(w.practiceMinutes)} min practice",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text("Tap a week to see its values.", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted)
            }
        }
        TextButton(onClick = { showTable = !showTable }) {
            Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (showTable) "Hide table" else "Show as table", style = MaterialTheme.typography.labelLarge)
        }
        if (showTable) TrendTable(weeks, labels)
    }
}

@Composable
private fun TrendTable(weeks: List<WeeklyRow>, labels: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TableRow("Week", "Quality", "Stress", "Mood", "Minutes", header = true)
        HorizontalDivider(color = AppTheme.colors.hairline)
        weeks.forEachIndexed { i, w ->
            TableRow(
                labels[i], w.practiceQuality?.let { f0(it) } ?: "–", w.meanStress?.let { f1(it) } ?: "–",
                w.meanMood?.let { f1(it) } ?: "–", f0(w.practiceMinutes)
            )
        }
    }
}

@Composable
private fun TableRow(vararg cells: String, header: Boolean = false) {
    Row {
        cells.forEach {
            Text(
                it, Modifier.weight(1f),
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) AppTheme.colors.inkMuted else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SymmetryCard(d: DashboardData) {
    PanelCard(spacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Rounded.Balance, AppTheme.colors.info, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Overline("Symmetry", color = AppTheme.colors.inkMuted)
                Text("Left vs right", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (d.symmetry.isEmpty()) {
            Text(
                "Practise both sides of Tree, Warrior II or Triangle to compare them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row {
                Text("◀ Left", style = MaterialTheme.typography.labelMedium, color = Brand.Practice, modifier = Modifier.weight(1f))
                Text("mean best hold", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.inkMuted)
                Text("Right ▶", style = MaterialTheme.typography.labelMedium, color = Brand.Mood, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
        val maxHold = d.symmetry.maxOfOrNull { maxOf(it.leftHoldSec, it.rightHoldSec) }?.takeIf { it > 0 } ?: 1.0
        d.symmetry.forEach { SymmetryRowView(it, maxHold) }
    }
}

@Composable
private fun SymmetryRowView(s: SymmetryRow, maxHold: Double) {
    val left = Brand.Practice
    val right = Brand.Mood
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.asana.sanskritName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(s.asana.englishName, style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.inkMuted)
            }
            s.holdSymmetryPct?.let {
                Pill(
                    "${f0(it)}% symmetric",
                    color = if (it >= 85) AppTheme.colors.success else AppTheme.colors.warning
                )
            }
        }
        // Butterfly bars: left grows leftwards from the centre line, right grows rightwards.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .semantics { contentDescription = "Left ${f1(s.leftHoldSec)} seconds, right ${f1(s.rightHoldSec)} seconds" }
        ) {
            val mid = size.width / 2
            val gap = 3.dp.toPx()
            val r = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(track, Offset(0f, 0f), Size(mid - gap, size.height), r)
            drawRoundRect(track, Offset(mid + gap, 0f), Size(mid - gap, size.height), r)
            val lw = ((mid - gap) * (s.leftHoldSec / maxHold)).toFloat()
            val rw = ((mid - gap) * (s.rightHoldSec / maxHold)).toFloat()
            if (lw > 0) drawRoundRect(Brush.horizontalGradient(listOf(left.copy(alpha = 0.55f), left), startX = mid - gap - lw, endX = mid - gap), Offset(mid - gap - lw, 0f), Size(lw, size.height), r)
            if (rw > 0) drawRoundRect(Brush.horizontalGradient(listOf(right, right.copy(alpha = 0.55f)), startX = mid + gap, endX = mid + gap + rw), Offset(mid + gap, 0f), Size(rw, size.height), r)
        }
        Text(
            "Mean best hold: left ${f1(s.leftHoldSec)} s · right ${f1(s.rightHoldSec)} s" +
                if (s.leftScore != null && s.rightScore != null) " · alignment ${f0(s.leftScore)}% / ${f0(s.rightScore)}%" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightCardView(card: InsightCard) {
    val r = card.result
    val (label, color) = when {
        !r.sufficient || r.rho == null || r.pValue == null -> "Need more days" to AppTheme.colors.inkMuted
        r.pValue < RankStatsEngine.SIGNIFICANCE -> "Pattern found" to AppTheme.colors.success
        else -> "No clear pattern" to AppTheme.colors.info
    }
    PanelCard(spacing = 10.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Rounded.Lightbulb, Brand.Gold, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(card.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Spearman ρ · permutation test", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.inkMuted)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(label, icon = if (label == "Pattern found") Icons.Rounded.CheckCircle else null, color = color)
            StatChip("ρ", r.rho?.let { String.format(Locale.US, "%+.2f", it) } ?: "n/a")
            r.pValue?.let { StatChip("p", if (it < 0.001) "< 0.001" else String.format(Locale.US, "%.3f", it)) }
            StatChip("n", "${r.n} days")
        }
        Text(card.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatChip(symbol: String, value: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(symbol, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReflectionCard(d: DashboardData) {
    PanelCard(spacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Rounded.CalendarMonth, Brand.Mood, size = 38.dp)
            Column {
                Overline("Fortnightly", color = AppTheme.colors.inkMuted)
                Text("Self-reflection check-ins", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        for ((type, list) in listOf(QuestionnaireType.PHQ9 to d.phq, QuestionnaireType.GAD7 to d.gad)) {
            val accent = if (type == QuestionnaireType.PHQ9) Brand.Mood else AppTheme.colors.info
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (type == QuestionnaireType.PHQ9) Icons.Rounded.Psychology else Icons.Rounded.Spa,
                        contentDescription = null, tint = accent, modifier = Modifier.size(18.dp)
                    )
                    Text(type.shortName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                if (list.isEmpty()) {
                    Text("${type.shortName}: not completed yet", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.inkMuted)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.takeLast(4).forEach { q ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accent.copy(alpha = 0.08f))
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${q.totalScore}/${type.maxScore}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(LocalDate.ofEpochDay(q.epochDay).format(SHORT), style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.inkMuted)
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(AppTheme.colors.inkMuted))
            Text("Scores are for noticing change over time, not a diagnosis.", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted)
        }
        d.phq.lastOrNull()?.let { Text("Latest PHQ-9 on ${formatEpochDay(it.epochDay)}", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.inkMuted) }
    }
}
