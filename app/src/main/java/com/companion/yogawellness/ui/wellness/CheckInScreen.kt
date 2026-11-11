package com.companion.yogawellness.ui.wellness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

/** Daily check-in, under 30 seconds (§9.2): mood, stress, sleep + optional pre/post tension. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(onDone: () -> Unit, vm: WellnessViewModel = viewModel()) {
    val existing by vm.todayCheckIn.collectAsStateWithLifecycle()
    var mood by rememberSaveable { mutableIntStateOf(6) }
    var stress by rememberSaveable { mutableIntStateOf(5) }
    var sleep by rememberSaveable { mutableIntStateOf(6) }
    var pre by rememberSaveable { mutableStateOf<Int?>(null) }
    var post by rememberSaveable { mutableStateOf<Int?>(null) }
    var loaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(existing) {
        val e = existing
        if (e != null && !loaded) {
            mood = e.mood; stress = e.stress; sleep = e.sleepQuality; pre = e.preTension; post = e.postTension
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily check-in") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Takes under 30 seconds. Just move the sliders.", style = MaterialTheme.typography.bodyMedium)
            ScaleSlider("Mood", mood, "Low", "Great") { mood = it }
            ScaleSlider("Stress", stress, "Calm", "Very stressed") { stress = it }
            ScaleSlider("Sleep quality", sleep, "Poor", "Excellent") { sleep = it }
            TensionPicker("Body tension before practice (optional)", pre) { pre = it }
            TensionPicker("Body tension after practice (optional)", post) { post = it }
            if (pre != null && post != null) {
                Text(
                    "Self-rated tension went from $pre to $post after today's practice.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Button(
                onClick = { vm.saveCheckIn(mood, stress, sleep, pre, post, onDone) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (existing != null) "Update today's check-in" else "Save check-in") }
        }
    }
}

@Composable
private fun ScaleSlider(label: String, value: Int, low: String, high: String, onChange: (Int) -> Unit) {
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$value / 10", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8
        )
        Row {
            Text(low, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(high, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TensionPicker(label: String, value: Int?, onChange: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..10).forEach { n ->
                FilterChip(
                    selected = value == n,
                    onClick = { onChange(if (value == n) null else n) },
                    label = { Text("$n") }
                )
            }
        }
    }
}
