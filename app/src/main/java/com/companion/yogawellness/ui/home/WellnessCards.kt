package com.companion.yogawellness.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.companion.yogawellness.ui.wellness.WellnessViewModel
import com.companion.yogawellness.ui.wellness.formatEpochDay
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.Questionnaires

/** Home-screen cards for the wellness module. */
@Composable
fun WellnessCards(
    vm: WellnessViewModel,
    onCheckIn: () -> Unit,
    onQuestionnaire: (QuestionnaireType) -> Unit,
    onSettings: () -> Unit,
    extra: @Composable () -> Unit = {}
) {
    val today by vm.todayCheckIn.collectAsStateWithLifecycle()
    val phq by vm.latestPhq.collectAsStateWithLifecycle()
    val gad by vm.latestGad.collectAsStateWithLifecycle()

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Daily check-in", style = MaterialTheme.typography.titleMedium)
            val t = today
            Text(
                if (t == null) "How are you today? Three sliders, under 30 seconds."
                else "Today: mood ${t.mood}, stress ${t.stress}, sleep ${t.sleepQuality} (out of 10)",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth()) { Text(if (t == null) "Check in" else "Update check-in") }
        }
    }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Fortnightly reflection", style = MaterialTheme.typography.titleMedium)
            Text("Every 14 days — non-diagnostic, for noticing changes over time.", style = MaterialTheme.typography.bodySmall)
            for ((type, latest) in listOf(QuestionnaireType.PHQ9 to phq, QuestionnaireType.GAD7 to gad)) {
                val due = Questionnaires.isDue(type, latest?.epochDay, vm.todayEpochDay)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val status = when {
                        latest == null -> "${type.shortName}: not done yet"
                        due -> "${type.shortName}: due now (last score ${latest.totalScore}/${type.maxScore})"
                        else -> "${type.shortName}: next on ${formatEpochDay(Questionnaires.nextDueEpochDay(type, latest.epochDay)!!)}"
                    }
                    Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                if (due) {
                    OutlinedButton(onClick = { onQuestionnaire(type) }, modifier = Modifier.fillMaxWidth()) { Text("Start ${type.shortName}") }
                }
            }
        }
    }

    extra()

    OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings & privacy") }
}
