package com.companion.yogawellness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.theme.YogaWellnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YogaWellnessTheme {
                Scaffold { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Yoga Wellness Companion", style = MaterialTheme.typography.headlineSmall)
                        Asana.entries.forEach { asana ->
                            Card(Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    "${asana.displayName} · ${asana.cameraAngle.instruction}",
                                    Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
