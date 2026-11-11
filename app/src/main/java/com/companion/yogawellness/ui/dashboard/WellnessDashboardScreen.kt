package com.companion.yogawellness.ui.dashboard

import android.content.ActivityNotFoundException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.companion.yogawellness.ui.wellness.formatEpochDay
import com.companion.yogawellness.wellness.DashboardData
import com.companion.yogawellness.wellness.InsightCard
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.WeeklyRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private fun f0(v: Double) = String.format(Locale.US, "%.0f", v)
private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessDashboardScreen(onBack: () -> Unit, vm: DashboardViewModel = viewModel()) {
    val data by vm.data.collectAsStateWithLifecycle()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wellness dashboard") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = export) { Icon(Icons.Default.PictureAsPdf, "Export weekly PDF report") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            vm.personaLabel?.let { Text("Demo data: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val d = data
            when {
                d == null -> Text("Loading…")
                d.isEmpty -> Text(
                    "Log a few daily check-ins and practice sessions to see your trends — or load a demo persona in Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> DashboardContent(d, export)
            }
        }
    }
}

@Composable
fun DashboardContent(d: DashboardData, onExport: () -> Unit) {
    StatTiles(d)
    TrendCard(d.weeks)
    SymmetryCard(d)
    d.insights.forEach { InsightCardView(it) }
    ReflectionCard(d)
    Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
        Text("  Export 1-page weekly report (PDF)")
    }
}

@Composable
private fun StatTiles(d: DashboardData) {
    val h = d.headline
    val quality = h.meanQuality?.let { q ->
        "${f0(q)}%" to h.previousWeekQuality?.let { p -> "${if (q >= p) "+" else ""}${f0(q - p)} vs last week" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Practice this week", "${f0(h.practiceMinutes)} min", "${h.sessions} sessions", Modifier.weight(1f))
            StatTile("Avg alignment", quality?.first ?: "–", quality?.second, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Check-ins", "${h.checkInDays} / 7", "days logged this week", Modifier.weight(1f))
            val t = d.tension
            StatTile(
                "Body tension", t?.let { "${f1(it.meanBefore)} → ${f1(it.meanAfter)}" } ?: "–",
                t?.let { "before → after practice" }, Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, sub: String?, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            sub?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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

    Card(colors = CardDefaults.cardColors(containerColor = Color(colors.surface))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Weekly trend", style = MaterialTheme.typography.titleMedium)
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
            if (sel != null && sel < weeks.size) {
                val w = weeks[sel]
                Text(
                    "Week of ${labels[sel]}: quality ${w.practiceQuality?.let { f0(it) } ?: "–"} · stress ${w.meanStress?.let { f1(it) } ?: "–"}" +
                        " · mood ${w.meanMood?.let { f1(it) } ?: "–"} · ${f0(w.practiceMinutes)} min practice",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("Tap a week to see its values.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { showTable = !showTable }) { Text(if (showTable) "Hide table" else "Show as table") }
            if (showTable) TrendTable(weeks, labels)
        }
    }
}

@Composable
private fun TrendTable(weeks: List<WeeklyRow>, labels: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TableRow("Week", "Quality", "Stress", "Mood", "Minutes", header = true)
        HorizontalDivider()
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
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SymmetryCard(d: DashboardData) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Left vs right", style = MaterialTheme.typography.titleMedium)
            if (d.symmetry.isEmpty()) {
                Text("Practise both sides of Tree, Warrior II or Triangle to compare them.", style = MaterialTheme.typography.bodySmall)
            }
            d.symmetry.forEach { s ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row {
                        Text(s.asana.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        s.holdSymmetryPct?.let { Text("${f0(it)}% symmetric", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
                    }
                    LinearProgressIndicator(progress = { ((s.holdSymmetryPct ?: 0.0) / 100).toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Mean best hold: left ${f1(s.leftHoldSec)} s · right ${f1(s.rightHoldSec)} s" +
                            if (s.leftScore != null && s.rightScore != null) " · alignment ${f0(s.leftScore)}% / ${f0(s.rightScore)}%" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightCardView(card: InsightCard) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(card.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ReflectionCard(d: DashboardData) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Self-reflection check-ins", style = MaterialTheme.typography.titleMedium)
            for ((type, list) in listOf(QuestionnaireType.PHQ9 to d.phq, QuestionnaireType.GAD7 to d.gad)) {
                Text(
                    if (list.isEmpty()) "${type.shortName}: not completed yet"
                    else "${type.shortName}: " + list.takeLast(4).joinToString(" → ") { "${it.totalScore}/${type.maxScore} (${formatEpochDay(it.epochDay)})" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text("Scores are for noticing change over time, not a diagnosis.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
