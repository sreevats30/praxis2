package com.companion.yogawellness.ui.wellness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.ScreenColumn
import com.companion.yogawellness.ui.components.SecondaryButton
import com.companion.yogawellness.ui.components.SectionLabel
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
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

    AppScreen(title = type.title, onBack = onDone) { padding ->
        Column(Modifier.padding(padding)) {
            if (saved == null) {
                LinearProgressIndicator(
                    progress = { answered / type.items.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = c.accent,
                    trackColor = c.sunken,
                    drawStopIndicator = {}
                )
            }
            ScreenColumn(PaddingValues(top = 12.dp), Modifier.weight(1f)) {
                val result = saved
                if (result != null) {
                    QuestionnaireResult(type, result, history, Questionnaires.needsCrisisSupport(type, answers), onDone) { showCrisis = true }
                } else {
                    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$answered of ${type.items.size} answered", style = MaterialTheme.typography.labelLarge, color = c.textSecondary, modifier = Modifier.weight(1f))
                        Text("Stored only on this phone", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
                    }
                    Notice(Questionnaires.NON_DIAGNOSTIC, Icons.Outlined.Info)
                    Text(
                        Questionnaires.STEM,
                        style = MaterialTheme.typography.titleMedium,
                        color = c.text,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    type.items.forEachIndexed { i, item ->
                        QuestionCard(i + 1, type.items.size, item, answers[i]) { value ->
                            answers[i] = value
                            if (Questionnaires.needsCrisisSupport(type, answers) && !crisisShownOnce) {
                                showCrisis = true
                                crisisShownOnce = true
                            }
                        }
                    }
                    PrimaryButton(
                        "Save my answers",
                        { vm.saveQuestionnaire(type, answers.toList()) { saved = it } },
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = Questionnaires.isComplete(type, answers)
                    )
                    Text(
                        Questionnaires.ATTRIBUTION,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }

    if (showCrisis) CrisisSupportSheet { showCrisis = false }
}

@Composable
private fun QuestionCard(number: Int, total: Int, text: String, selected: Int?, onSelect: (Int) -> Unit) {
    val c = AppTheme.colors
    Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$number of $total", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
            Text(text, style = MaterialTheme.typography.bodyLarge, color = c.text)
        }
        Column(Modifier.selectableGroup()) {
            Questionnaires.OPTIONS.forEachIndexed { value, label ->
                if (value > 0) Hairline(inset = 52.dp)
                AnswerRow(label, value, selected == value) { onSelect(value) }
            }
        }
    }
}

@Composable
private fun AnswerRow(label: String, points: Int, on: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = on, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = on, onClick = null, modifier = Modifier.padding(horizontal = 12.dp),
            colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.borderStrong)
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
        Text(
            "$points",
            style = MaterialTheme.typography.labelMedium,
            color = c.textTertiary,
            modifier = Modifier.semantics { contentDescription = "$points points" }
        )
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
    Panel(spacing = 14.dp) {
        Text("Self-Reflection Check-In Score", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${result.totalScore}", style = NumericStyle.copy(fontSize = 44.sp), color = c.text)
            Text(" / ${type.maxScore}", style = MaterialTheme.typography.titleMedium, color = c.textTertiary, modifier = Modifier.padding(bottom = 6.dp))
        }
        Hairline()
        Text(Questionnaires.NON_DIAGNOSTIC, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        Questionnaires.nextDueEpochDay(type, result.epochDay)?.let {
            Text("Next ${type.shortName} reflection: ${formatEpochDay(it)}", style = MaterialTheme.typography.bodyMedium, color = c.text)
        }
    }
    if (showSupport) {
        SecondaryButton(
            "Talk to someone — Tele-MANAS ${CrisisLines.TELE_MANAS_SHORT}", onOpenSupport, Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Call
        )
    }
    if (history.size > 1) {
        SectionLabel("Your scores over time")
        Panel(spacing = 12.dp) {
            HistoryBars(history, type.maxScore)
            history.takeLast(6).reversed().forEachIndexed { i, q ->
                if (i > 0) Hairline()
                Row {
                    Text(formatEpochDay(q.epochDay), style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
                    Text("${q.totalScore} / ${type.maxScore}", style = MaterialTheme.typography.labelLarge, color = c.text)
                }
            }
        }
    }
    PrimaryButton("Done", onDone, Modifier.fillMaxWidth().padding(top = 8.dp))
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
                drawRoundRect(c.sunken, Offset(x, 0f), Size(bw, size.height), r)
                val h = size.height * q.totalScore / max.toFloat()
                if (h > 0f) drawRoundRect(if (i == n - 1) c.accent else c.borderStrong, Offset(x, size.height - h), Size(bw, h), r)
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
