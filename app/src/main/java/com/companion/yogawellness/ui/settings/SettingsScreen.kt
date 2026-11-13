package com.companion.yogawellness.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.ui.consent.NON_DIAGNOSTIC_STATEMENT
import com.companion.yogawellness.ui.wellness.WellnessViewModel
import com.companion.yogawellness.wellness.Persona

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onDataDeleted: () -> Unit, vm: WellnessViewModel = viewModel()) {
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmPersona by remember { mutableStateOf<Persona?>(null) }
    var confirmRemoveDemo by remember { mutableStateOf(false) }
    val viewing by vm.activeProfileId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & privacy") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
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
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Privacy", style = MaterialTheme.typography.titleMedium)
                    Text(NON_DIAGNOSTIC_STATEMENT, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Everything is processed and stored on this phone. The app has no internet permission and is excluded from backups.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Demo data", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Load 42 days of synthetic data to explore the dashboard and history. " +
                            "Adds synthetic data you can browse. Your own data is not changed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Viewing: ${ProfileIds.displayName(viewing)}", style = MaterialTheme.typography.bodySmall)
                    Persona.entries.forEach { p ->
                        OutlinedButton(onClick = { confirmPersona = p }, modifier = Modifier.fillMaxWidth()) { Text(p.label) }
                    }
                    TextButton(onClick = { confirmRemoveDemo = true }, modifier = Modifier.fillMaxWidth()) { Text("Remove demo data") }
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delete All My Data", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Erases check-ins, questionnaire answers, practice history, recorded pose clips, exported reports and settings.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete All My Data") }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete everything?") },
            text = { Text("This permanently erases all data stored by this app on this phone. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteAllData(onDataDeleted) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
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
                }) { Text("Load") }
            },
            dismissButton = { TextButton(onClick = { confirmPersona = null }) { Text("Cancel") } }
        )
    }
    if (confirmRemoveDemo) {
        AlertDialog(
            onDismissRequest = { confirmRemoveDemo = false },
            title = { Text("Remove demo data?") },
            text = { Text("Removes the three synthetic personas. Your own data is not affected.") },
            confirmButton = {
                TextButton(onClick = { confirmRemoveDemo = false; vm.removeDemoData {} }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveDemo = false }) { Text("Cancel") } }
        )
    }
}
