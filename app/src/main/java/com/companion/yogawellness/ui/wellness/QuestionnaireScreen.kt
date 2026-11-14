package com.companion.yogawellness.ui.wellness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Meter
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.RadioRow
import com.companion.yogawellness.ui.components.SecondaryButton
import com.companion.yogawellness.ui.components.Section
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.Space
import com.companion.yogawellness.wellness.CrisisLines
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.Questionnaires
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
fun formatEpochDay(day: Long): String = LocalDate.ofEpochDay(day).format(DATE)

/** Fortnightly PHQ-9 / GAD-7 self-reflection (§9.3). Raw scores only; never clinical labels. */
@Composable
fun QuestionnaireScreen(type: QuestionnaireType, onDone: () -> Unit, vm: WellnessViewModel = viewModel()) {
    val answers = remember { mutableStateListOf<Int?>().apply { repeat(type.items.size) { add(null) } } }
    var showCrisis by rememberSaveable { mutableStateOf(false) }
    var crisisShownOnce by rememberSaveable { mutableStateOf(false) }
    var saved by remember { mutableStateOf<QuestionnaireEntity?>(null) }
    val history by vm.history(type).collectAsStateWithLifecycle()
    val answered = answers.count { it != null }
    val c = AppTheme.colors

    AppScreen(title = type.title, onBack = onDone, largeTitle = false) {
        val result = saved
        if (result != null) {
            QuestionnaireResult(type, result, history, Questionnaires.needsCrisisSupport(type, answers), onDone) { showCrisis = true }
        } else {
            Column(Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.S), verticalArrangement = Arrangement.spacedBy(Space.S)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$answered of ${type.items.size} answered", style = MaterialTheme.typography.labelLarge, color = c.textSecondary, modifier = Modifier.weight(1f))
                    Text("Stored only on this phone", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
                }
                Meter(answered / type.items.size.toFloat(), c.accent, height = 3.dp)
            }
            Section(divider = false, spacing = Space.L) {
                Notice(Questionnaires.NON_DIAGNOSTIC)
                Text(Questionnaires.STEM, style = MaterialTheme.typography.headlineSmall, color = c.text)
            }
            type.items.forEachIndexed { i, item ->
                Question(i + 1, type.items.size, item, answers[i]) { value ->
                    answers[i] = value
                    if (Questionnaires.needsCrisisSupport(type, answers) && !crisisShownOnce) {
                        showCrisis = true
                        crisisShownOnce = true
                    }
                }
            }
            Section(spacing = Space.M) {
                PrimaryButton(
                    "Save my answers",
                    { vm.saveQuestionnaire(type, answers.toList()) { saved = it } },
                    Modifier.fillMaxWidth(),
                    enabled = Questionnaires.isComplete(type, answers)
                )
                Text(Questionnaires.ATTRIBUTION, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
        }
    }

    if (showCrisis) CrisisSupportSheet { showCrisis = false }
}

@Composable
private fun Question(number: Int, total: Int, text: String, selected: Int?, onSelect: (Int) -> Unit) {
    val c = AppTheme.colors
    Section(gutter = false, spacing = 0.dp) {
        Column(Modifier.padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.S), verticalArrangement = Arrangement.spacedBy(Space.XS)) {
            Text("$number of $total", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
            Text(text, style = MaterialTheme.typography.bodyLarge, color = c.text)
        }
        Column(Modifier.selectableGroup()) {
            Questionnaires.OPTIONS.forEachIndexed { value, label ->
                RadioRow(label, selected == value, { onSelect(value) }) {
                    Text(
                        "$value",
                        style = NumericStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
                        color = c.textTertiary,
                        modifier = Modifier.semantics { contentDescription = "$value points" }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionnaireResult(
    type: QuestionnaireType,
    result: QuestionnaireEntity,
    history: List<QuestionnaireEntity>,
    showSupport: Boolean,
    onDone: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val c = AppTheme.colors
    Section(divider = false, spacing = Space.M) {
        Text("Self-reflection score", style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${result.totalScore}", style = NumericStyle.copy(fontSize = 48.sp, lineHeight = 52.sp), color = c.text)
            Text(" / ${type.maxScore}", style = MaterialTheme.typography.titleMedium, color = c.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(Questionnaires.NON_DIAGNOSTIC, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Questionnaires.nextDueEpochDay(type, result.epochDay)?.let {
            Text("Next ${type.shortName} reflection: ${formatEpochDay(it)}", style = MaterialTheme.typography.bodyMedium, color = c.text)
        }
        if (showSupport) {
            SecondaryButton(
                "Talk to someone — Tele-MANAS ${CrisisLines.TELE_MANAS_SHORT}", onOpenSupport, Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Call
            )
        }
    }
    if (history.size > 1) {
        Section(title = "Your scores over time", spacing = Space.M) {
            HistoryBars(history, type.maxScore)
            Column {
                history.takeLast(6).reversed().forEachIndexed { i, q ->
                    if (i > 0) Hairline()
                    Row(Modifier.padding(vertical = 10.dp)) {
                        Text(formatEpochDay(q.epochDay), style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
                        Text("${q.totalScore} / ${type.maxScore}", style = NumericStyle.copy(fontSize = 14.sp), color = c.text)
                    }
                }
            }
        }
    }
    Section { PrimaryButton("Done", onDone, Modifier.fillMaxWidth()) }
}

@Composable
private fun HistoryBars(history: List<QuestionnaireEntity>, max: Int) {
    val items = history.takeLast(8)
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .semantics { contentDescription = "Bar chart of your last ${items.size} scores" }
        ) {
            val n = items.size
            val gap = 8.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            items.forEachIndexed { i, q ->
                val x = i * (bw + gap)
                val r = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                drawRoundRect(c.control, Offset(x, 0f), Size(bw, size.height), r)
                val h = size.height * q.totalScore / max.toFloat()
                if (h > 0f) drawRoundRect(if (i == n - 1) c.text else c.lineStrong, Offset(x, size.height - h), Size(bw, h), r)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            items.forEach {
                Text(
                    LocalDate.ofEpochDay(it.epochDay).format(SHORT),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
