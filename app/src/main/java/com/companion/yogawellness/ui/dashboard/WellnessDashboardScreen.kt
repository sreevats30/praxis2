package com.companion.yogawellness.ui.dashboard

import android.content.ActivityNotFoundException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.R
import com.companion.yogawellness.stats.RankStatsEngine
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.PanelHeader
import com.companion.yogawellness.ui.components.ScreenColumn
import com.companion.yogawellness.ui.components.SecondaryButton
import com.companion.yogawellness.ui.components.SectionLabel
import com.companion.yogawellness.ui.components.Stat
import com.companion.yogawellness.ui.components.Tag
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.components.VerticalHairline
import com.companion.yogawellness.ui.profile.DemoBannerBar
import com.companion.yogawellness.ui.profile.ProfileSelectorBar
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
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
    val c = AppTheme.colors
    AppScreen(
        title = "Insights",
        large = true,
        onBack = onBack,
        // No report to export until there is real data (a new account starts empty).
        actions = {
            if (data != null && !data.isEmpty) {
                IconButton(onClick = onExport) { Icon(Icons.Outlined.FileDownload, contentDescription = "Export weekly PDF report") }
            }
        }
    ) { padding ->
        ScreenColumn(padding) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Last ${DashboardBuilder.WINDOW_DAYS / 7} weeks · analysed on this phone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
                demoLabel?.let { Tag(it, tone = Tone.WARNING) }
            }
            profiles()
            error?.let { Notice(it, Icons.Outlined.ErrorOutline, tone = Tone.NEGATIVE) }
            when {
                data == null -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.textTertiary)
                        Text("Calculating…", style = MaterialTheme.typography.bodyMedium, color = c.textTertiary)
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
    Panel(spacing = 6.dp) {
        Text("Your insights start here", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.text)
        Text(
            "Log a few daily check-ins and practice sessions to see your trends — or load a demo persona in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary
        )
    }
}

@Composable
fun DashboardContent(d: DashboardData, onExport: () -> Unit) {
    SectionLabel("This week")
    StatGrid(d)
    SectionLabel("Trend")
    TrendCard(d.weeks)
    SectionLabel("Left vs right")
    SymmetryCard(d)
    if (d.insights.isNotEmpty()) SectionLabel("Statistics")
    d.insights.forEach { InsightCardView(it) }
    SectionLabel("Self-reflection")
    ReflectionCard(d)
    SecondaryButton("Export PDF report", onExport, Modifier.fillMaxWidth().padding(top = 8.dp), icon = Icons.Outlined.FileDownload)
}

@Composable
private fun StatGrid(d: DashboardData) {
    val h = d.headline
    val q = h.meanQuality
    val p = h.previousWeekQuality
    val delta = if (q != null && p != null) "${if (q >= p) "+" else "−"}${f0(kotlin.math.abs(q - p))} vs last week" else "average"
    Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("Practice", f0(h.practiceMinutes), Modifier.weight(1f), unit = "min", caption = "${h.sessions} sessions")
            VerticalHairline()
            Stat(
                "Alignment", q?.let { f0(it) } ?: "–", Modifier.weight(1f), unit = q?.let { "%" }, caption = delta,
                valueColor = AppTheme.colors.text
            )
        }
        Hairline()
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("Check-ins", "${h.checkInDays} / 7", Modifier.weight(1f), caption = "days logged")
            VerticalHairline()
            val t = d.tension
            Stat(
                "Body tension", t?.let { "${f1(it.meanBefore)} → ${f1(it.meanAfter)}" } ?: "–", Modifier.weight(1f),
                caption = t?.let { "before → after practice" } ?: "not logged"
            )
        }
    }
}

@Composable
private fun TrendCard(weeks: List<WeeklyRow>) {
    val context = LocalContext.current
    val colors = if (isSystemInDarkTheme()) ChartColors.Dark else ChartColors.Light
    val density = LocalDensity.current
    val typeface = remember { runCatching { ResourcesCompat.getFont(context, R.font.inter_regular) }.getOrNull() }
    val renderer = remember(colors, density, typeface) {
        TrendChartRenderer(colors, density.density, density.density * density.fontScale, typeface)
    }
    val labels = weeks.map { LocalDate.ofEpochDay(it.weekStartEpochDay).format(SHORT) }
    var selected by remember(weeks) { mutableStateOf<Int?>(null) }
    var showTable by remember { mutableStateOf(false) }
    val c = AppTheme.colors

    Panel(spacing = 8.dp) {
        PanelHeader("Weekly trend", subtitle = "${weeks.size} weeks · tap a week for its values")
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(272.dp)
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
        if (sel != null && sel < weeks.size) {
            val w = weeks[sel]
            Text(
                "Week of ${labels[sel]}: quality ${w.practiceQuality?.let { f0(it) } ?: "–"} · stress ${w.meanStress?.let { f1(it) } ?: "–"}" +
                    " · mood ${w.meanMood?.let { f1(it) } ?: "–"} · ${f0(w.practiceMinutes)} min practice",
                style = MaterialTheme.typography.bodySmall,
                color = c.text
            )
        }
        Hairline()
        TextButton(onClick = { showTable = !showTable }, contentPadding = PaddingValues(0.dp)) {
            Text(if (showTable) "Hide table" else "Show as table", style = MaterialTheme.typography.labelLarge, color = c.accent)
        }
        if (showTable) TrendTable(weeks, labels)
    }
}

@Composable
private fun TrendTable(weeks: List<WeeklyRow>, labels: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TableRow("Week", "Quality", "Stress", "Mood", "Min", header = true)
        Hairline()
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
                style = if (header) MaterialTheme.typography.labelMedium else NumericStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Normal),
                color = if (header) AppTheme.colors.textTertiary else AppTheme.colors.text
            )
        }
    }
}

@Composable
private fun SymmetryCard(d: DashboardData) {
    val c = AppTheme.colors
    Panel(spacing = 14.dp) {
        if (d.symmetry.isEmpty()) {
            Text("Practise both sides of Tree, Warrior II or Triangle to compare them.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            return@Panel
        }
        Row {
            Text("Left", style = MaterialTheme.typography.labelMedium, color = c.textTertiary, modifier = Modifier.weight(1f))
            Text("Mean best hold", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
            Text("Right", style = MaterialTheme.typography.labelMedium, color = c.textTertiary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        val maxHold = d.symmetry.maxOfOrNull { maxOf(it.leftHoldSec, it.rightHoldSec) }?.takeIf { it > 0 } ?: 1.0
        d.symmetry.forEachIndexed { i, s ->
            if (i > 0) Hairline()
            SymmetryRowView(s, maxHold)
        }
    }
}

@Composable
private fun SymmetryRowView(s: SymmetryRow, maxHold: Double) {
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.asana.sanskritName, style = MaterialTheme.typography.titleSmall, color = c.text)
                Text(s.asana.englishName, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            s.holdSymmetryPct?.let { Tag("${f0(it)}% symmetric", tone = if (it >= 85) Tone.POSITIVE else Tone.WARNING) }
        }
        // Mirrored bars from a centre line: left grows leftwards, right rightwards.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .semantics { contentDescription = "Left ${f1(s.leftHoldSec)} seconds, right ${f1(s.rightHoldSec)} seconds" }
        ) {
            val mid = size.width / 2
            val gap = 2.dp.toPx()
            val r = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(c.sunken, Offset(0f, 0f), Size(mid - gap, size.height), r)
            drawRoundRect(c.sunken, Offset(mid + gap, 0f), Size(mid - gap, size.height), r)
            val lw = ((mid - gap) * (s.leftHoldSec / maxHold)).toFloat()
            val rw = ((mid - gap) * (s.rightHoldSec / maxHold)).toFloat()
            if (lw > 0) drawRoundRect(c.accent, Offset(mid - gap - lw, 0f), Size(lw, size.height), r)
            if (rw > 0) drawRoundRect(c.textSecondary, Offset(mid + gap, 0f), Size(rw, size.height), r)
        }
        Text(
            "Mean best hold: left ${f1(s.leftHoldSec)} s · right ${f1(s.rightHoldSec)} s" +
                if (s.leftScore != null && s.rightScore != null) " · alignment ${f0(s.leftScore)}% / ${f0(s.rightScore)}%" else "",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary
        )
    }
}

@Composable
private fun InsightCardView(card: InsightCard) {
    val r = card.result
    val c = AppTheme.colors
    val (label, tone) = when {
        !r.sufficient || r.rho == null || r.pValue == null -> "Need more days" to Tone.NEUTRAL
        r.pValue < RankStatsEngine.SIGNIFICANCE -> "Pattern found" to Tone.POSITIVE
        else -> "No clear pattern" to Tone.NEUTRAL
    }
    Panel(spacing = 10.dp) {
        PanelHeader(card.title, subtitle = "Spearman ρ · permutation test", trailing = { Tag(label, tone = tone) })
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            StatInline("ρ", r.rho?.let { String.format(Locale.US, "%+.2f", it) } ?: "n/a")
            r.pValue?.let { StatInline("p", if (it < 0.001) "< 0.001" else String.format(Locale.US, "%.3f", it)) }
            StatInline("n", "${r.n} days")
        }
        // The finding reads first; the standing caveats follow in smaller type (the PDF keeps the full text).
        val caveatAt = card.text.indexOf(RankStatsEngine.CAVEAT_CAUSATION)
        Text(
            if (caveatAt > 0) card.text.substring(0, caveatAt).trim() else card.text,
            style = MaterialTheme.typography.bodyMedium,
            color = c.text
        )
        if (caveatAt > 0) {
            Text(
                card.text.substring(caveatAt).replace("Note: ", ""),
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary
            )
        }
    }
}

@Composable
private fun StatInline(symbol: String, value: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(symbol, style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.textTertiary)
        Text(value, style = NumericStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = AppTheme.colors.text)
    }
}

@Composable
private fun ReflectionCard(d: DashboardData) {
    val c = AppTheme.colors
    Panel(spacing = 12.dp) {
        listOf(QuestionnaireType.PHQ9 to d.phq, QuestionnaireType.GAD7 to d.gad).forEachIndexed { i, (type, list) ->
            if (i > 0) Hairline()
            Row(verticalAlignment = Alignment.Top) {
                Text(type.shortName, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(0.3f))
                if (list.isEmpty()) {
                    Text("${type.shortName}: not completed yet", style = MaterialTheme.typography.bodyMedium, color = c.textTertiary, modifier = Modifier.weight(0.7f))
                } else {
                    Row(Modifier.weight(0.7f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        list.takeLast(4).forEach { q ->
                            Column {
                                Text("${q.totalScore}/${type.maxScore}", style = NumericStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize), color = c.text)
                                Text(LocalDate.ofEpochDay(q.epochDay).format(SHORT), style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                            }
                        }
                    }
                }
            }
        }
        Hairline()
        Text("Scores are for noticing change over time, not a diagnosis.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
    }
}
