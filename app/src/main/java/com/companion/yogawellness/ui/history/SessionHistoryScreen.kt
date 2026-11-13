package com.companion.yogawellness.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.WellnessRepository
import com.companion.yogawellness.data.db.PoseRecordEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import com.companion.yogawellness.ui.practice.scoreColor
import com.companion.yogawellness.ui.profile.DemoBannerBar
import com.companion.yogawellness.ui.profile.ProfileSelectorBar
import com.companion.yogawellness.wellness.HistoryAggregator
import com.companion.yogawellness.wellness.HistorySummary
import com.companion.yogawellness.wellness.SessionWithPoses
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)

fun formatSessionTime(startedAtMs: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(startedAtMs))

/** "Virabhadrasana II · Left leg forward" (side only for sided poses). */
fun poseTitle(p: PoseRecordEntity): String {
    val asana = Asana.entries.firstOrNull { it.name == p.asana }
    val side = PoseSide.entries.firstOrNull { it.name == p.side }
    val name = asana?.sanskritName ?: p.asana
    return if (asana?.hasSides == true && side != null) "$name · ${side.label}" else name
}

@Composable
fun SessionHistoryScreen(
    onStartPractice: () -> Unit,
    vm: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    SessionHistoryContent(
        state = state,
        onStartPractice = onStartPractice,
        onDelete = vm::deleteSession,
        onLogManual = vm::logManual,
        header = {
            ProfileSelectorBar()
            DemoBannerBar()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryContent(
    state: HistoryUiState,
    onStartPractice: () -> Unit,
    onDelete: (Long) -> Unit,
    onLogManual: (Int) -> Unit,
    header: @Composable () -> Unit = {}
) {
    var confirmDelete by remember { mutableStateOf<PracticeSessionEntity?>(null) }
    var showManual by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateListOf<Long>() }

    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { header() }
            }
            if (!state.loading && state.sessions.isEmpty()) {
                item(key = "empty") {
                    EmptyHistory(onStartPractice, if (state.canEdit) ({ showManual = true }) else null)
                }
            } else if (state.sessions.isNotEmpty()) {
                item(key = "summary") { SummaryCard(state.summary) }
                if (state.canEdit) {
                    item(key = "manual") {
                        OutlinedButton(onClick = { showManual = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Log manual practice")
                        }
                    }
                }
                items(state.sessions, key = { it.session.id }) { item ->
                    val id = item.session.id
                    SessionCard(
                        item = item,
                        expanded = id in expanded,
                        onToggle = { if (id in expanded) expanded.remove(id) else expanded.add(id) },
                        onDelete = if (state.canEdit) ({ confirmDelete = item.session }) else null
                    )
                }
            }
        }
    }

    confirmDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this session?") },
            text = { Text("${formatSessionTime(s.startedAtMs)} and its poses will be removed. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = null; onDelete(s.id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
    if (showManual) {
        ManualPracticeDialog(onDismiss = { showManual = false }, onSave = { showManual = false; onLogManual(it) })
    }
}

@Composable
private fun EmptyHistory(onStartPractice: () -> Unit, onLogManual: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "No sessions yet. Start a practice — every pose you hold for 2 seconds is saved here.",
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onStartPractice, modifier = Modifier.fillMaxWidth()) { Text("Start practice") }
        if (onLogManual != null) {
            OutlinedButton(onClick = onLogManual, modifier = Modifier.fillMaxWidth()) { Text("Log manual practice") }
        }
    }
}

@Composable
private fun SummaryCard(s: HistorySummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("All practice", style = MaterialTheme.typography.titleMedium)
            Text("${s.sessions} ${if (s.sessions == 1) "session" else "sessions"} · ${HistoryAggregator.formatDuration(s.totalMinutes * 60)} practised")
            Text("Best hold: " + (s.bestHoldSec?.let { "${f1(it)} s" } ?: "–"))
            Text("Mean alignment: " + (s.meanAlignment?.let { "${it.toInt()}%" } ?: "–"))
        }
    }
}

@Composable
private fun InfoChip(text: String, dot: Color? = null) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = dot?.let { BorderStroke(1.dp, it) }
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(8.dp).background(dot, CircleShape))
                Text(" ", style = MaterialTheme.typography.labelMedium)
            }
            Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionCard(item: SessionWithPoses, expanded: Boolean, onToggle: () -> Unit, onDelete: (() -> Unit)?) {
    val s = item.session
    val manual = s.source == PracticeSessionEntity.SOURCE_MANUAL
    val when_ = formatSessionTime(s.startedAtMs)
    Card(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse" else "Expand", onClick = onToggle)
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(when_, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip(HistoryAggregator.formatDuration(s.activeSec))
                    if (manual) {
                        InfoChip("Manual")
                    } else {
                        s.meanScore?.let { InfoChip("${it.toInt()}% align", dot = scoreColor(it)) }
                        HistoryAggregator.stability(item.poses)?.let { InfoChip("${it.toInt()}% stab") }
                    }
                }
                if (!manual) {
                    Text(
                        "${item.poses.size} ${if (item.poses.size == 1) "pose" else "poses"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete session from $when_") }
            }
        }
        if (expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider()
                if (manual) Text("Logged by hand — no pose data or score.", style = MaterialTheme.typography.bodySmall)
                item.poses.forEach { PoseRow(it) }
            }
        }
    }
}

@Composable
private fun PoseRow(p: PoseRecordEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(poseTitle(p), style = MaterialTheme.typography.titleSmall)
        Text("Held ${f1(p.lockedSec)} s (≥75%: ${f1(p.qualitySec)} s)", style = MaterialTheme.typography.bodyMedium)
        val parts = listOfNotNull(p.meanScore?.let { "Align ${it.toInt()}%" }, p.meanStability?.let { "Stab ${it.toInt()}%" })
        if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
        coachingLine(p)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** `Worst joint: Front Knee — "Bend your front knee deeper toward 90 degrees"` (stored text as-is). */
fun coachingLine(p: PoseRecordEntity): String? = when {
    p.worstJoint != null && p.topCue != null -> "Worst joint: ${p.worstJoint} — \"${p.topCue}\""
    p.worstJoint != null -> "Worst joint: ${p.worstJoint}"
    p.topCue != null -> "Most frequent cue: \"${p.topCue}\""
    else -> null
}

@Composable
private fun ManualPracticeDialog(onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val minutes = text.toIntOrNull()
    val valid = minutes != null && minutes in WellnessRepository.MANUAL_MINUTES_RANGE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log manual practice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("For practice done without the camera. Only the minutes are saved — no score.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(3) },
                    label = { Text("Minutes (1–180)") },
                    singleLine = true,
                    isError = text.isNotEmpty() && !valid,
                    supportingText = if (text.isNotEmpty() && !valid) ({ Text("Enter 1 to 180 minutes") }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = { TextButton(onClick = { minutes?.let(onSave) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
