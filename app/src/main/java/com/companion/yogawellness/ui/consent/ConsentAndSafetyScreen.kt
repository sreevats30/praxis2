package com.companion.yogawellness.ui.consent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.wellness.CrisisLines

const val NON_DIAGNOSTIC_STATEMENT =
    "This app is a wellness and self-reflection companion, not a medical or diagnostic device."
const val PHYSICAL_SAFETY_STATEMENT =
    "Practice on a non-slip surface. Stop immediately if you feel any sharp pain, dizziness, or joint strain."

/** First-run consent, non-diagnostic disclaimer and physical safety screen (§9.1). */
@Composable
fun ConsentAndSafetyScreen(onAccepted: () -> Unit) {
    var notMedical by rememberSaveable { mutableStateOf(false) }
    var dataConsent by rememberSaveable { mutableStateOf(false) }
    var safety by rememberSaveable { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Welcome", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(NON_DIAGNOSTIC_STATEMENT, style = MaterialTheme.typography.titleMedium)

            Section("Practise safely") {
                Bullet(PHYSICAL_SAFETY_STATEMENT)
                Bullet("Clear a space around you and keep your phone propped up, not held.")
                Bullet("If you are pregnant, injured, recovering from surgery or have a health condition, check with a doctor before practising.")
                Bullet("Hold poses only as long as is comfortable. Chair Pose is capped at 30 seconds.")
            }
            Section("Your data stays on this phone") {
                Bullet("The camera image is analysed on this phone to estimate your pose. Frames are never saved or sent anywhere.")
                Bullet("The app has no internet permission. Check-ins, questionnaire answers and practice history are stored only on this device and are excluded from backups.")
                Bullet("You can erase everything at any time with \"Delete All My Data\" in Settings.")
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "If you are going through a difficult time, India's Tele-MANAS helpline is free and confidential: " +
                        "${CrisisLines.TELE_MANAS_SHORT} or ${CrisisLines.TELE_MANAS_TOLL_FREE}. In an emergency, call ${CrisisLines.EMERGENCY}.",
                    Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ConsentRow(notMedical, { notMedical = it }, "I understand this app does not diagnose or treat any condition.")
            ConsentRow(dataConsent, { dataConsent = it }, "I agree to on-device camera processing and to storing my data only on this device.")
            ConsentRow(safety, { safety = it }, "I will practise safely and stop if anything hurts.")

            Button(
                onClick = onAccepted,
                enabled = notMedical && dataConsent && safety,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Agree and continue") }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•  ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ConsentRow(checked: Boolean, onChange: (Boolean) -> Unit, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
