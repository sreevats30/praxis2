package com.companion.yogawellness.ui.dashboard

import android.content.ActivityNotFoundException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.R
import com.companion.yogawellness.stats.RankStatsEngine
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.EmptyState
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.IconAction
import com.companion.yogawellness.ui.components.LoadingState
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.SecondaryButton
import com.companion.yogawellness.ui.components.Section
import com.companion.yogawellness.ui.components.Stat
import com.companion.yogawellness.ui.components.StatusLabel
import com.companion.yogawellness.ui.components.TextAction
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.profile.DemoBannerBar
import com.companion.yogawellness.ui.profile.ProfileSelectorBar
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.SanskritStyle
import com.companion.yogawellness.ui.theme.Space
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
            DemoBannerBar(Modifier.padding(horizontal = Space.Gutter))
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
        onBack = onBack,
        largeTitle = true,
        subtitle = "The last ${DashboardBuilder.WINDOW_DAYS / 7} weeks, analysed on this phone." +
            (demoLabel?.let { " Showing $it." } ?: ""),
        // No report to export until there is real data (a new account starts empty).
        actions = {
            if (data != null && !data.isEmpty) IconAction(Icons.Outlined.FileDownload, "Export weekly PDF report", onExport)
        }
    ) {
        profiles()
        error?.let { Notice(it, Modifier.padding(horizontal = Space.Gutter, vertical = Space.S), tone = Tone.NEGATIVE, title = "Export failed") }
        when {
            data == null -> LoadingState("Calculating…", Modifier.padding(horizontal = Space.Gutter))
            data.isEmpty -> EmptyState(
                "Your insights start here",
                "Log a few daily check-ins and practice sessions to see your trends — or load a demo persona in Settings.",
                Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.L)
            )
            else -> DashboardContent(data, onExport)
        }
    }
}

@Composable
fun DashboardContent(d: DashboardData, onExport: () -> Unit) {
    Section(title = "This week", divider = false) { StatGrid(d) }
    Section(title = "Weekly trend") { TrendChart(d.weeks) }
    if (d.insights.isNotEmpty()) {
        Section(title = "Patterns", spacing = Space.XL) {
            d.insights.forEachIndexed { i, card ->
                if (i > 0) Hairline()
                InsightView(card)
            }
            // The caveats are the same for every finding: stated once, after them.
            Text(
                "Spearman rank correlation with a ${RankStatsEngine.DEFAULT_SHUFFLES}-shuffle permutation test. " +
                    RankStatsEngine.CAVEAT_CAUSATION + " " + RankStatsEngine.CAVEAT_AUTOCORRELATION.removePrefix("Note: "),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textTertiary
            )
        }
    }
    Section(title = "Left and right sides", spacing = Space.L) { Symmetry(d) }
    Section(title = "Self-reflection", spacing = 0.dp) { Reflections(d) }
    Section {
        SecondaryButton("Export PDF report", onExport, Modifier.fillMaxWidth(), icon = Icons.Outlined.FileDownload)
        Text(
            "A one-page summary of this week to keep or share. It is created on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textTertiary
        )
    }
}

@Composable
private fun StatGrid(d: DashboardData) {
    val h = d.headline
    val q = h.meanQuality
    val p = h.previousWeekQuality
    val delta = if (q != null && p != null) "${if (q >= p) "+" else "−"}${f0(kotlin.math.abs(q - p))} vs last week" else "average"
    Column(verticalArrangement = Arrangement.spacedBy(Space.XL)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.L)) {
            Stat("Practice", f0(h.practiceMinutes), Modifier.weight(1f), unit = "min", caption = "${h.sessions} sessions")
            Stat("Alignment", q?.let { f0(it) } ?: "–", Modifier.weight(1f), unit = q?.let { "%" }, caption = delta)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.L)) {
            Stat("Check-ins", "${h.checkInDays} / 7", Modifier.weight(1f), caption = "days logged")
            val t = d.tension
            Stat(
                "Body tension", t?.let { "${f1(it.meanBefore)} → ${f1(it.meanAfter)}" } ?: "–", Modifier.weight(1f),
                caption = t?.let { "before → after practice" } ?: "not logged"
            )
        }
    }
}

@Composable
private fun TrendChart(weeks: List<WeeklyRow>) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val typeface = remember { runCatching { ResourcesCompat.getFont(context, R.font.inter_regular) }.getOrNull() }
    val renderer = remember(density, typeface) {
        TrendChartRenderer(ChartColors.Dark, density.density, density.density * density.fontScale, typeface)
    }
    val labels = weeks.map { LocalDate.ofEpochDay(it.weekStartEpochDay).format(SHORT) }
    var selected by remember(weeks) { mutableStateOf<Int?>(null) }
    var showTable by remember { mutableStateOf(false) }
    val c = AppTheme.colors

    Text("${weeks.size} weeks · tap a week to read its values", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(256.dp)
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
    TextAction(if (showTable) "Hide table" else "Show as table", { showTable = !showTable }, Modifier.offset(x = (-Space.S)))
    if (showTable) TrendTable(weeks, labels)
}

@Composable
private fun TrendTable(weeks: List<WeeklyRow>, labels: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.S)) {
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
        cells.forEachIndexed { i, text ->
            Text(
                text, Modifier.weight(if (i == 0) 1.3f else 1f),
                style = if (header) MaterialTheme.typography.labelMedium else NumericStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
                color = if (header) AppTheme.colors.textTertiary else AppTheme.colors.text,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End
            )
        }
    }
}

/** The finding as a sentence (serif), its statistics as figures, and the caveats in small type. */
@Composable
private fun InsightView(card: InsightCard) {
    val r = card.result
    val c = AppTheme.colors
    val (label, tone) = when {
        !r.sufficient || r.rho == null || r.pValue == null -> "Need more days" to Tone.NEUTRAL
        r.pValue < RankStatsEngine.SIGNIFICANCE -> "Pattern found" to Tone.POSITIVE
        else -> "No clear pattern" to Tone.NEUTRAL
    }
    // The PDF keeps the full text; here the statistics move out of the sentence into their own row.
    val caveatAt = card.text.indexOf(RankStatsEngine.CAVEAT_CAUSATION)
    val finding = (if (caveatAt > 0) card.text.substring(0, caveatAt) else card.text).replace(STATS_IN_TEXT, "").trim()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(card.title, style = MaterialTheme.typography.titleSmall, color = c.textSecondary, modifier = Modifier.weight(1f))
            StatusLabel(label, tone)
        }
        Text(finding, style = MaterialTheme.typography.headlineSmall, color = c.text)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.XL)) {
            StatInline("ρ", r.rho?.let { String.format(Locale.US, "%+.2f", it) } ?: "n/a")
            r.pValue?.let { StatInline("p", if (it < 0.001) "< 0.001" else String.format(Locale.US, "%.3f", it)) }
            StatInline("n", "${r.n} days")
        }
    }
}

/** "(ρ = -0.81, p < 0.001, n = 42 days)" inside a generated sentence. */
private val STATS_IN_TEXT = Regex("""\s*\(ρ = [^)]*\)""")

@Composable
private fun StatInline(symbol: String, value: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(symbol, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textTertiary)
        Text(value, style = NumericStyle.copy(fontSize = 15.sp), color = AppTheme.colors.text)
    }
}

@Composable
private fun Symmetry(d: DashboardData) {
    val c = AppTheme.colors
    if (d.symmetry.isEmpty()) {
        Text("Practise both sides of Tree, Warrior II or Triangle to compare them.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        return
    }
    Text("Mean best hold on each side. Bars grow outwards from the centre.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
    val maxHold = d.symmetry.maxOfOrNull { maxOf(it.leftHoldSec, it.rightHoldSec) }?.takeIf { it > 0 } ?: 1.0
    d.symmetry.forEach { SymmetryRowView(it, maxHold) }
}

@Composable
private fun SymmetryRowView(s: SymmetryRow, maxHold: Double) {
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.S)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(s.asana.englishName, style = MaterialTheme.typography.titleSmall, color = c.text)
                Text(s.asana.sanskritName, style = SanskritStyle.copy(fontSize = 14.sp), color = c.textTertiary)
            }
            s.holdSymmetryPct?.let { StatusLabel("${f0(it)}% symmetric", if (it >= 85) Tone.POSITIVE else Tone.WARNING) }
        }
        // Mirrored bars from a centre line: left grows leftwards, right rightwards.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .semantics { contentDescription = "Left ${f1(s.leftHoldSec)} seconds, right ${f1(s.rightHoldSec)} seconds" }
        ) {
            val mid = size.width / 2
            val gap = 2.dp.toPx()
            val r = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(c.control, Offset(0f, 0f), Size(mid - gap, size.height), r)
            drawRoundRect(c.control, Offset(mid + gap, 0f), Size(mid - gap, size.height), r)
            val lw = ((mid - gap) * (s.leftHoldSec / maxHold)).toFloat()
            val rw = ((mid - gap) * (s.rightHoldSec / maxHold)).toFloat()
            if (lw > 0) drawRoundRect(c.text, Offset(mid - gap - lw, 0f), Size(lw, size.height), r)
            if (rw > 0) drawRoundRect(c.textTertiary, Offset(mid + gap, 0f), Size(rw, size.height), r)
        }
        Row {
            Text(
                "Left ${f1(s.leftHoldSec)} s" + (s.leftScore?.let { " · ${f0(it)}%" } ?: ""),
                style = NumericStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal), color = c.textSecondary, modifier = Modifier.weight(1f)
            )
            Text(
                "Right ${f1(s.rightHoldSec)} s" + (s.rightScore?.let { " · ${f0(it)}%" } ?: ""),
                style = NumericStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal), color = c.textSecondary
            )
        }
    }
}

@Composable
private fun Reflections(d: DashboardData) {
    val c = AppTheme.colors
    listOf(QuestionnaireType.PHQ9 to d.phq, QuestionnaireType.GAD7 to d.gad).forEachIndexed { i, (type, list) ->
        if (i > 0) Hairline()
        Row(Modifier.fillMaxWidth().padding(vertical = Space.M), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(0.38f)) {
                Text(type.shortName, style = MaterialTheme.typography.titleSmall, color = c.text)
                Text("out of ${type.maxScore}", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            if (list.isEmpty()) {
                Text("Not completed yet", style = MaterialTheme.typography.bodyMedium, color = c.textTertiary, modifier = Modifier.weight(0.62f))
            } else {
                Row(Modifier.weight(0.62f), horizontalArrangement = Arrangement.spacedBy(Space.L)) {
                    list.takeLast(4).forEach { q ->
                        Column {
                            Text("${q.totalScore}", style = NumericStyle.copy(fontSize = 17.sp), color = c.text)
                            Text(LocalDate.ofEpochDay(q.epochDay).format(SHORT), style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                        }
                    }
                }
            }
        }
    }
    Text(
        "Scores are for noticing change over time, not a diagnosis.",
        style = MaterialTheme.typography.bodySmall,
        color = c.textTertiary,
        modifier = Modifier.padding(top = Space.S)
    )
}
