package com.companion.yogawellness.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.db.PoseRecordEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.PoseGlyph
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.SectionLabel
import com.companion.yogawellness.ui.components.Stat
import com.companion.yogawellness.ui.components.Tag
import com.companion.yogawellness.ui.components.VerticalHairline
import com.companion.yogawellness.ui.practice.scoreColor
import com.companion.yogawellness.ui.profile.DemoBannerBar
import com.companion.yogawellness.ui.profile.ProfileSelectorBar
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.wellness.HistoryAggregator
import com.companion.yogawellness.wellness.HistorySummary
import com.companion.yogawellness.wellness.SessionWithPoses
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)

/** Device-locale date and time, without seconds ("12 Nov 2024, 8:26 am"). */
fun formatSessionTime(startedAtMs: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault())
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
        header = {
            ProfileSelectorBar()
            DemoBannerBar()
        }
    )
}

@Composable
fun SessionHistoryContent(
    state: HistoryUiState,
    onStartPractice: () -> Unit,
    onDelete: (Long) -> Unit,
    header: @Composable () -> Unit = {}
) {
    var confirmDelete by remember { mutableStateOf<PracticeSessionEntity?>(null) }
    val expanded = remember { mutableStateListOf<Long>() }

    AppScreen(title = "History", large = true) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { header() }
            }
            if (!state.loading && state.sessions.isEmpty()) {
                item(key = "empty") { EmptyHistory(onStartPractice) }
            } else if (state.sessions.isNotEmpty()) {
                item(key = "summary") { SummaryCard(state.summary) }
                item(key = "sessionsHeader") { SectionLabel("Sessions") }
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
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; onDelete(s.id) }) { Text("Delete", color = AppTheme.colors.negative) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel", color = AppTheme.colors.text) } }
        )
    }
}

@Composable
private fun EmptyHistory(onStartPractice: () -> Unit) {
    Panel(spacing = 8.dp) {
        Text("No sessions yet", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.text)
        Text(
            "Start a practice — every pose you hold for 2 seconds is saved here.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary
        )
        PrimaryButton("Start practice", onStartPractice, Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun SummaryCard(s: HistorySummary) {
    Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("Sessions", "${s.sessions}", Modifier.weight(1f))
            VerticalHairline()
            Stat("Time practised", HistoryAggregator.formatDuration(s.totalMinutes * 60), Modifier.weight(1f))
        }
        Hairline()
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("Best hold", s.bestHoldSec?.let { f1(it) } ?: "–", Modifier.weight(1f), unit = s.bestHoldSec?.let { "s" })
            VerticalHairline()
            Stat("Mean alignment", s.meanAlignment?.let { "${it.toInt()}" } ?: "–", Modifier.weight(1f), unit = s.meanAlignment?.let { "%" })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionCard(item: SessionWithPoses, expanded: Boolean, onToggle: () -> Unit, onDelete: (() -> Unit)?) {
    val s = item.session
    val c = AppTheme.colors
    val manual = s.source == PracticeSessionEntity.SOURCE_MANUAL
    val whenText = formatSessionTime(s.startedAtMs)
    Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse" else "Expand", onClick = onToggle)
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(whenText, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = c.textTertiary)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(HistoryAggregator.formatDuration(s.activeSec))
                    if (manual) {
                        Tag("Manual")
                    } else {
                        s.meanScore?.let { ScoreTag("${it.toInt()}% align", it) }
                        HistoryAggregator.stability(item.poses)?.let { Tag("${it.toInt()}% stab") }
                        Tag("${item.poses.size} ${if (item.poses.size == 1) "pose" else "poses"}")
                    }
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete session from $whenText", tint = c.textTertiary)
                }
            }
        }
        if (expanded) {
            Hairline()
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (manual) Text("Logged by hand — no pose data or score.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                item.poses.forEach { PoseRow(it) }
            }
        }
    }
}

/** Neutral tag with a small score-colour dot (colour is never the only signal: the number is there). */
@Composable
private fun ScoreTag(text: String, score: Double) {
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.colors.sunken)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(scoreColor(score)))
        Text(text, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textSecondary)
    }
}

@Composable
private fun PoseRow(p: PoseRecordEntity) {
    val c = AppTheme.colors
    val asana = Asana.entries.firstOrNull { it.name == p.asana }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(c.sunken), contentAlignment = Alignment.Center) {
            asana?.let { PoseGlyph(it, Modifier.size(38.dp)) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(poseTitle(p), style = MaterialTheme.typography.titleSmall, color = c.text)
            Text("Held ${f1(p.lockedSec)} s (≥75%: ${f1(p.qualitySec)} s)", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            val parts = listOfNotNull(p.meanScore?.let { "Align ${it.toInt()}%" }, p.meanStability?.let { "Stab ${it.toInt()}%" })
            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            coachingLine(p)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textTertiary) }
        }
    }
}

/** `Worst joint: Front Knee — "Bend your front knee deeper toward 90 degrees"` (stored text as-is). */
fun coachingLine(p: PoseRecordEntity): String? = when {
    p.worstJoint != null && p.topCue != null -> "Worst joint: ${p.worstJoint} — \"${p.topCue}\""
    p.worstJoint != null -> "Worst joint: ${p.worstJoint}"
    p.topCue != null -> "Most frequent cue: \"${p.topCue}\""
    else -> null
}
