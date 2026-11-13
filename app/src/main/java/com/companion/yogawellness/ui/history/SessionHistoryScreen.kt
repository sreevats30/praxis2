package com.companion.yogawellness.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.scoring.PoseSide
import com.companion.yogawellness.data.db.PoseRecordEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.components.ClinicalTopBar
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.components.PoseGlyph
import com.companion.yogawellness.ui.components.SoftButton
import com.companion.yogawellness.ui.practice.scoreColor
import com.companion.yogawellness.ui.profile.DemoBannerBar
import com.companion.yogawellness.ui.profile.ProfileSelectorBar
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.OverlineStyle
import com.companion.yogawellness.wellness.HistoryAggregator
import com.companion.yogawellness.wellness.HistorySummary
import com.companion.yogawellness.wellness.SessionWithPoses
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ClinicalTopBar(
                title = "Practice history",
                subtitle = "Every session, saved on this phone",
                bottom = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPill("Auto-saved", icon = Icons.Rounded.CloudDone)
                        GlassPill("Poses held ≥ 2 s", icon = Icons.Rounded.Timer)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { header() }
            }
            if (!state.loading && state.sessions.isEmpty()) {
                item(key = "empty") {
                    EmptyHistory(onStartPractice)
                }
            } else if (state.sessions.isNotEmpty()) {
                item(key = "summary") { SummaryCard(state.summary) }
                item(key = "sessionsHeader") {
                    Text(
                        "SESSIONS",
                        style = OverlineStyle,
                        color = AppTheme.colors.inkMuted,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
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
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = AppTheme.colors.danger) },
            title = { Text("Delete this session?") },
            text = { Text("${formatSessionTime(s.startedAtMs)} and its poses will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; onDelete(s.id) }) { Text("Delete", color = AppTheme.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EmptyHistory(onStartPractice: () -> Unit) {
    PanelCard(spacing = 14.dp) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(AppTheme.colors.heroStart, AppTheme.colors.heroMid, Brand.Teal700))),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                listOf(Asana.VRIKSHASANA, Asana.VIRABHADRASANA_II, Asana.UTKATASANA).forEach {
                    PoseGlyph(it, Modifier.size(if (it == Asana.VIRABHADRASANA_II) 110.dp else 80.dp))
                }
            }
        }
        Text("Your practice log starts here", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "No sessions yet. Start a practice — every pose you hold for 2 seconds is saved here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        GradientButton("Start practice", onStartPractice, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
    }
}

@Composable
private fun SummaryCard(s: HistorySummary) {
    val c = AppTheme.colors
    PanelCard(
        color = c.heroStart,
        brush = Brush.linearGradient(listOf(c.heroStart, c.heroMid, Brand.Teal700)),
        borderColor = null,
        spacing = 14.dp
    ) {
        Column {
            Overline("All practice", color = Brand.Mint)
            Text(
                "${s.sessions} ${if (s.sessions == 1) "session" else "sessions"} · ${HistoryAggregator.formatDuration(s.totalMinutes * 60)} practised",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStat(Icons.Rounded.Timer, "Best hold", s.bestHoldSec?.let { "${f1(it)} s" } ?: "–", Modifier.weight(1f))
            SummaryStat(Icons.Rounded.Accessibility, "Mean alignment", s.meanAlignment?.let { "${it.toInt()}%" } ?: "–", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryStat(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Brand.Mint, modifier = Modifier.size(20.dp))
        Column {
            Text(value, style = NumericStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = Color.White)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun InfoChip(text: String, dot: Color? = null, icon: ImageVector? = null) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (dot != null) Modifier.border(1.dp, dot.copy(alpha = 0.6f), CircleShape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (dot != null) Box(Modifier.size(8.dp).background(dot, CircleShape))
        icon?.let { Icon(it, contentDescription = null, tint = AppTheme.colors.inkMuted, modifier = Modifier.size(13.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Calendar-style date badge for a session card. */
@Composable
private fun DateBadge(startedAtMs: Long, accent: Color) {
    val date = Instant.ofEpochMilli(startedAtMs).atZone(ZoneId.systemDefault()).toLocalDate()
    Column(
        Modifier
            .size(width = 48.dp, height = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("${date.dayOfMonth}", style = NumericStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = accent)
        Text(
            date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
            style = OverlineStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = accent
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionCard(item: SessionWithPoses, expanded: Boolean, onToggle: () -> Unit, onDelete: (() -> Unit)?) {
    val s = item.session
    val manual = s.source == PracticeSessionEntity.SOURCE_MANUAL
    val when_ = formatSessionTime(s.startedAtMs)
    val accent = if (manual) AppTheme.colors.inkMuted else s.meanScore?.let { scoreColor(it) } ?: MaterialTheme.colorScheme.primary
    PanelCard(contentPadding = PaddingValues(0.dp), spacing = 0.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse" else "Expand", onClick = onToggle)
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateBadge(s.startedAtMs, accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(when_, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoChip(HistoryAggregator.formatDuration(s.activeSec), icon = Icons.Rounded.Timer)
                        if (manual) {
                            InfoChip("Manual", icon = Icons.Rounded.EditNote)
                        } else {
                            s.meanScore?.let { InfoChip("${it.toInt()}% align", dot = scoreColor(it)) }
                            HistoryAggregator.stability(item.poses)?.let { InfoChip("${it.toInt()}% stab") }
                        }
                    }
                    if (!manual) {
                        Text(
                            "${item.poses.size} ${if (item.poses.size == 1) "pose" else "poses"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.inkMuted
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = AppTheme.colors.inkMuted
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete session from $when_", tint = AppTheme.colors.inkMuted)
                }
            }
        }
        if (expanded) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(color = AppTheme.colors.hairline)
                if (manual) {
                    Text("Logged by hand — no pose data or score.", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted)
                }
                item.poses.forEach { PoseRow(it) }
            }
        }
    }
}

@Composable
private fun PoseRow(p: PoseRecordEntity) {
    val asana = Asana.entries.firstOrNull { it.name == p.asana }
    val color = scoreColor(p.meanScore)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(AppTheme.colors.heroStart, Brand.Teal700))),
            contentAlignment = Alignment.Center
        ) {
            asana?.let { PoseGlyph(it, Modifier.size(40.dp), glow = false) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(poseTitle(p), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Held ${f1(p.lockedSec)} s (≥75%: ${f1(p.qualitySec)} s)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val parts = listOfNotNull(p.meanScore?.let { "Align ${it.toInt()}%" }, p.meanStability?.let { "Stab ${it.toInt()}%" })
            if (parts.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (p.meanScore != null) Box(Modifier.size(8.dp).background(color, CircleShape))
                    Text(parts.joinToString(" · "), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            coachingLine(p)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted) }
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
