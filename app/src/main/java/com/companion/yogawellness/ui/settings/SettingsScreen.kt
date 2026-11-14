package com.companion.yogawellness.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.BuildConfig
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.ListRow
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.RowIcon
import com.companion.yogawellness.ui.components.ScreenColumn
import com.companion.yogawellness.ui.components.SectionLabel
import com.companion.yogawellness.ui.components.Tag
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.consent.NON_DIAGNOSTIC_STATEMENT
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.wellness.WellnessViewModel
import com.companion.yogawellness.wellness.Persona

@Composable
fun SettingsScreen(onBack: () -> Unit, onDataDeleted: () -> Unit, vm: WellnessViewModel = viewModel()) {
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmPersona by remember { mutableStateOf<Persona?>(null) }
    var confirmRemoveDemo by remember { mutableStateOf(false) }
    val viewing by vm.activeProfileId.collectAsStateWithLifecycle()
    val c = AppTheme.colors

    AppScreen(title = "Settings", onBack = onBack) { padding ->
        ScreenColumn(padding, spacing = 8.dp) {
            SectionLabel("Privacy")
            Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
                ListRow("Processed on this phone", supporting = "Pose estimation runs on-device; camera frames are never saved", leading = { RowIcon(Icons.Outlined.Memory) })
                Hairline(inset = 52.dp)
                ListRow("No internet access", supporting = "The app has no network permission", leading = { RowIcon(Icons.Outlined.CloudOff) })
                Hairline(inset = 52.dp)
                ListRow("Excluded from backups", supporting = "Your data never leaves this device", leading = { RowIcon(Icons.Outlined.VisibilityOff) })
            }
            Text(
                "$NON_DIAGNOSTIC_STATEMENT Everything is processed and stored on this phone. The app has no internet permission and is excluded from backups.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            SectionLabel("Demo data", trailing = { Tag("Viewing: ${ProfileIds.displayName(viewing)}", tone = if (ProfileIds.isDemo(viewing)) Tone.WARNING else Tone.NEUTRAL) })
            Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
                Persona.entries.forEachIndexed { i, p ->
                    if (i > 0) Hairline(inset = 52.dp)
                    ListRow(
                        p.label,
                        supporting = p.description,
                        leading = { RowIcon(Icons.Outlined.Science) },
                        trailing = if (viewing == p.profileId) ({ Tag("Viewing", tone = Tone.POSITIVE) }) else null,
                        onClick = { confirmPersona = p }
                    )
                }
                Hairline(inset = 52.dp)
                ListRow("Remove demo data", leading = { RowIcon(Icons.Outlined.DeleteSweep) }, onClick = { confirmRemoveDemo = true }, chevron = false)
            }
            Text(
                "Each persona loads 42 days of synthetic data so you can explore Insights and History. " +
                    "Your own data is not changed.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            SectionLabel("Data")
            Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
                ListRow(
                    "Delete All My Data",
                    leading = { RowIcon(Icons.Outlined.DeleteForever, c.negative) },
                    titleColor = c.negative,
                    onClick = { confirmDelete = true },
                    chevron = false
                )
            }
            Text(
                "Erases check-ins, questionnaire answers, practice history, recorded pose clips, exported reports and settings.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            SectionLabel("About")
            Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
                ListRow("Version", leading = { RowIcon(Icons.Outlined.Info) }, trailing = {
                    Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium, color = c.textTertiary)
                })
                Hairline(inset = 52.dp)
                ListRow("Pose estimation", supporting = "MediaPipe Pose Landmarker, running fully on-device", leading = { RowIcon(Icons.Outlined.Memory) })
                Hairline(inset = 52.dp)
                ListRow("Typeface", supporting = "Inter (SIL Open Font License)", leading = { RowIcon(Icons.Outlined.TextFields) })
            }
            Text(
                "PHQ-9 and GAD-7 are shown as non-diagnostic self-reflection scores — never as clinical labels.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete everything?") },
            text = { Text("This permanently erases all data stored by this app on this phone. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteAllData(onDataDeleted) }) { Text("Delete", color = c.negative) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = c.text) } }
        )
    }
    confirmPersona?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmPersona = null },
            title = { Text("Load ${p.label}?") },
            text = { Text(p.description + "\n\nYour own data is not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmPersona = null
                    vm.loadPersona(p) {}
                }) { Text("Load", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmPersona = null }) { Text("Cancel", color = c.text) } }
        )
    }
    if (confirmRemoveDemo) {
        AlertDialog(
            onDismissRequest = { confirmRemoveDemo = false },
            title = { Text("Remove demo data?") },
            text = { Text("Removes the three synthetic personas. Your own data is not affected.") },
            confirmButton = {
                TextButton(onClick = { confirmRemoveDemo = false; vm.removeDemoData {} }) { Text("Remove", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveDemo = false }) { Text("Cancel", color = c.text) } }
        )
    }
}
