package com.companion.yogawellness.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.data.AppPreferences
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.ToleranceProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onStartPractice: () -> Unit, extraContent: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var profile by remember { mutableStateOf(prefs.toleranceProfile) }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Yoga Wellness Companion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "A wellness and self-reflection companion — not a medical or diagnostic device. Everything stays on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Practice", style = MaterialTheme.typography.titleMedium)
                    Text("Prop the phone upright 2–3 m away, stand in Tadasana to calibrate, then move into a pose.")
                    Text("Alignment tolerance", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ToleranceProfile.entries.forEachIndexed { i, p ->
                            SegmentedButton(
                                selected = profile == p,
                                onClick = { profile = p; prefs.toleranceProfile = p },
                                shape = SegmentedButtonDefaults.itemShape(i, ToleranceProfile.entries.size)
                            ) { Text(p.label) }
                        }
                    }
                    Button(onClick = onStartPractice, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = null)
                        Text("  Start practice")
                    }
                }
            }

            extraContent()

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Supported poses", style = MaterialTheme.typography.titleMedium)
                    Asana.entries.forEach { a ->
                        Text("• ${a.displayName} — ${a.cameraAngle.instruction.lowercase()}", style = MaterialTheme.typography.bodyMedium)
                        Text("   ${a.safetyRule}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
