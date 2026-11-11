package com.companion.yogawellness.ui.wellness

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.Questionnaires
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
fun formatEpochDay(day: Long): String = LocalDate.ofEpochDay(day).format(DATE)

/** Fortnightly PHQ-9 / GAD-7 self-reflection (§9.3). Raw scores only; never clinical labels. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireScreen(type: QuestionnaireType, onDone: () -> Unit, vm: WellnessViewModel = viewModel()) {
    val answers = remember { mutableStateListOf<Int?>().apply { repeat(type.items.size) { add(null) } } }
    var showCrisis by rememberSaveable { mutableStateOf(false) }
    var crisisShownOnce by rememberSaveable { mutableStateOf(false) }
    var saved by remember { mutableStateOf<QuestionnaireEntity?>(null) }
    val history by vm.history(type).collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(type.title) },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val result = saved
            if (result != null) {
                QuestionnaireResult(type, result, history, Questionnaires.needsCrisisSupport(type, answers), onDone) { showCrisis = true }
            } else {
                Text(Questionnaires.NON_DIAGNOSTIC, style = MaterialTheme.typography.bodySmall)
                Text(Questionnaires.STEM, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                type.items.forEachIndexed { i, item ->
                    QuestionItem(i + 1, item, answers[i]) { value ->
                        answers[i] = value
                        if (Questionnaires.needsCrisisSupport(type, answers) && !crisisShownOnce) {
                            showCrisis = true
                            crisisShownOnce = true
                        }
                    }
                    HorizontalDivider()
                }
                Button(
                    onClick = { vm.saveQuestionnaire(type, answers.toList()) { saved = it } },
                    enabled = Questionnaires.isComplete(type, answers),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save my answers") }
                Text(Questionnaires.ATTRIBUTION, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    if (showCrisis) CrisisSupportSheet { showCrisis = false }
}

@Composable
private fun QuestionItem(number: Int, text: String, selected: Int?, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$number. $text", style = MaterialTheme.typography.bodyLarge)
        Questionnaires.OPTIONS.forEachIndexed { value, label ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(value) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == value, onClick = { onSelect(value) })
                Text(label, style = MaterialTheme.typography.bodyMedium)
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Self-Reflection Check-In Score", style = MaterialTheme.typography.titleMedium)
            Text("${result.totalScore} / ${type.maxScore}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(Questionnaires.NON_DIAGNOSTIC, style = MaterialTheme.typography.bodySmall)
            Questionnaires.nextDueEpochDay(type, result.epochDay)?.let {
                Text("Next ${type.shortName} reflection: ${formatEpochDay(it)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (showSupport) {
        OutlinedButton(onClick = onOpenSupport, modifier = Modifier.fillMaxWidth()) { Text("Talk to someone — Tele-MANAS 14416") }
    }
    if (history.size > 1) {
        Text("Your scores over time", style = MaterialTheme.typography.titleSmall)
        history.forEach { Text("${formatEpochDay(it.epochDay)} — ${it.totalScore} / ${type.maxScore}", style = MaterialTheme.typography.bodyMedium) }
    }
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}
