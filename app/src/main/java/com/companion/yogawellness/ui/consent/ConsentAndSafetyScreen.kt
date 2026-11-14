package com.companion.yogawellness.ui.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.ui.components.BrandMark
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.RowIcon
import com.companion.yogawellness.ui.components.SectionLabel
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.theme.AppTheme
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
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandMark(size = 52.dp)
            Text("Yoga Wellness Companion", style = MaterialTheme.typography.headlineMedium, color = c.text)
            Text(
                "Posture coaching and self-reflection check-ins that run entirely on this phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary
            )
        }

        Panel(contentPadding = PaddingValues(vertical = 4.dp), spacing = 0.dp) {
            Feature(Icons.Outlined.SelfImprovement, "Live posture feedback", "Joint angles and an alignment score for four standing poses, with voice cues.")
            Hairline(inset = 52.dp)
            Feature(Icons.Outlined.Psychology, "Check-ins", "Mood, stress and sleep each day; PHQ-9 and GAD-7 every 14 days.")
            Hairline(inset = 52.dp)
            Feature(Icons.Outlined.Insights, "Weekly insights", "Trends with statistics that state their uncertainty, and a one-page PDF report.")
        }

        Notice(NON_DIAGNOSTIC_STATEMENT, Icons.Outlined.Info, title = "Not a medical device")

        SectionLabel("Practise safely")
        Panel(spacing = 10.dp) {
            Bullet(PHYSICAL_SAFETY_STATEMENT)
            Bullet("Clear a space around you and keep your phone propped up, not held.")
            Bullet("If you are pregnant, injured, recovering from surgery or have a health condition, check with a doctor before practising.")
            Bullet("Hold poses only as long as is comfortable. Chair Pose is capped at 30 seconds.")
        }

        SectionLabel("Your data stays on this phone")
        Panel(spacing = 10.dp) {
            Bullet("The camera image is analysed on this phone to estimate your pose. Frames are never saved or sent anywhere.")
            Bullet("The app has no internet permission. Check-ins, questionnaire answers and practice history are stored only on this device and are excluded from backups.")
            Bullet("You can erase everything at any time with \"Delete All My Data\" in Settings.")
        }

        SectionLabel("Support")
        Panel {
            Text(
                "If you are going through a difficult time, India's Tele-MANAS helpline is free and confidential: " +
                    "${CrisisLines.TELE_MANAS_SHORT} or ${CrisisLines.TELE_MANAS_TOLL_FREE}. In an emergency, call ${CrisisLines.EMERGENCY}.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.text
            )
        }

        SectionLabel("Confirm to continue")
        Panel(contentPadding = PaddingValues(vertical = 4.dp), spacing = 0.dp) {
            ConsentRow(notMedical, { notMedical = it }, "I understand this app does not diagnose or treat any condition.")
            Hairline(inset = 56.dp)
            ConsentRow(dataConsent, { dataConsent = it }, "I agree to on-device camera processing and to storing my data only on this device.")
            Hairline(inset = 56.dp)
            ConsentRow(safety, { safety = it }, "I will practise safely and stop if anything hurts.")
        }

        Spacer(Modifier.height(4.dp))
        PrimaryButton("Agree and continue", onAccepted, Modifier.fillMaxWidth(), enabled = notMedical && dataConsent && safety)
        Text(
            "You can review these statements any time in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.padding(top = 1.dp)) { RowIcon(icon, AppTheme.colors.accent) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.text)
            Text(text, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 8.dp).size(4.dp).clip(CircleShape).background(AppTheme.colors.textTertiary))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ConsentRow(checked: Boolean, onChange: (Boolean) -> Unit, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .heightIn(min = 56.dp)
            .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = CheckboxDefaults.colors(checkedColor = AppTheme.colors.accent, uncheckedColor = AppTheme.colors.borderStrong)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.text, modifier = Modifier.weight(1f))
    }
}
