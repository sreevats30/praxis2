package com.companion.yogawellness.ui.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.ui.components.BulletList
import com.companion.yogawellness.ui.components.CheckRow
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.RowIcon
import com.companion.yogawellness.ui.components.Section
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Space
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
    val c = AppTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Wordmark and promise
        Column(
            Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = 56.dp, bottom = Space.XL),
            verticalArrangement = Arrangement.spacedBy(Space.M)
        ) {
            Text("Yoga Wellness Companion", style = MaterialTheme.typography.labelLarge, color = c.accent)
            Text("Practise with feedback. Notice how you feel.", style = MaterialTheme.typography.displaySmall, color = c.text)
            Text(
                "Posture coaching and self-reflection check-ins that run entirely on this phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary
            )
        }

        Section(spacing = Space.L) {
            Feature(Icons.Outlined.SelfImprovement, "Live posture feedback", "Joint angles and an alignment score for four standing poses, with voice cues.")
            Feature(Icons.Outlined.Psychology, "Check-ins", "Mood, stress and sleep each day; PHQ-9 and GAD-7 every 14 days.")
            Feature(Icons.Outlined.Insights, "Weekly insights", "Trends with statistics that state their uncertainty, and a one-page PDF report.")
            Notice(NON_DIAGNOSTIC_STATEMENT, Modifier.padding(top = Space.S), title = "Not a medical device")
        }

        Section(title = "Practise safely") {
            BulletList(
                listOf(
                    PHYSICAL_SAFETY_STATEMENT,
                    "Clear a space around you and keep your phone propped up, not held.",
                    "If you are pregnant, injured, recovering from surgery or have a health condition, check with a doctor before practising.",
                    "Hold poses only as long as is comfortable. Chair Pose is capped at 30 seconds."
                )
            )
        }

        Section(title = "Your data stays on this phone") {
            BulletList(
                listOf(
                    "The camera image is analysed on this phone to estimate your pose. Frames are never saved or sent anywhere.",
                    "The app has no internet permission. Check-ins, questionnaire answers and practice history are stored only on this device and are excluded from backups.",
                    "You can erase everything at any time with \"Delete All My Data\" in Settings."
                )
            )
        }

        Section(title = "Support") {
            Text(
                "If you are going through a difficult time, India's Tele-MANAS helpline is free and confidential: " +
                    "${CrisisLines.TELE_MANAS_SHORT} or ${CrisisLines.TELE_MANAS_TOLL_FREE}. In an emergency, call ${CrisisLines.EMERGENCY}.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.text
            )
        }

        Section(title = "Confirm to continue", gutter = false, spacing = 0.dp) {
            CheckRow(notMedical, { notMedical = it }, "I understand this app does not diagnose or treat any condition.")
            CheckRow(dataConsent, { dataConsent = it }, "I agree to on-device camera processing and to storing my data only on this device.")
            CheckRow(safety, { safety = it }, "I will practise safely and stop if anything hurts.")
            Column(Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.L), verticalArrangement = Arrangement.spacedBy(Space.M)) {
                PrimaryButton("Agree and continue", onAccepted, Modifier.fillMaxWidth(), enabled = notMedical && dataConsent && safety)
                Text("You can review these statements any time in Settings.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.L)) {
        Box(Modifier.padding(top = 1.dp)) { RowIcon(icon, AppTheme.colors.accent) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.text)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
        }
    }
}
