package com.companion.yogawellness.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.db.PoseRecordEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import com.companion.yogawellness.ui.components.AppLazyScreen
import com.companion.yogawellness.ui.components.EmptyState
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.LoadingState
import com.companion.yogawellness.ui.components.PoseGlyph
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.SectionHeader
import com.companion.yogawellness.ui.components.Stat
import com.companion.yogawellness.ui.components.TextAction
import com.companion.yogawellness.ui.practice.scoreColor
import com.companion.yogawellness.ui.profile.DemoBannerBar
import com.companion.yogawellness.ui.profile.ProfileSelectorBar
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.Radius
import com.companion.yogawellness.ui.theme.Space
import com.companion.yogawellness.wellness.HistoryAggregator
import com.companion.yogawellness.wellness.HistorySummary
import com.companion.yogawellness.wellness.SessionWithPoses
import java.time.Instant
import java.time.LocalDate
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
            DemoBannerBar(Modifier.padding(horizontal = Space.Gutter))
        }
    )
}

private val TIME = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val DAY = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
private val DAY_YEAR = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)

/** "Today", "Yesterday" or the weekday and date (with the year when it isn't this year). */
fun dayHeading(epochDay: Long, today: LocalDate = LocalDate.now()): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return when {
        d == today -> "Today"
        d == today.minusDays(1) -> "Yesterday"
        d.year == today.year -> d.format(DAY)
        else -> d.format(DAY_YEAR)
    }
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
    val days = remember(state.sessions) { state.sessions.groupBy { it.session.epochDay }.toList() }

    AppLazyScreen(title = "History", subtitle = "Every session is saved automatically, on this phone only.") {
        item(key = "header") { Column { header() } }
        when {
            state.loading && state.sessions.isEmpty() -> item(key = "loading") {
                LoadingState("Loading sessions…", Modifier.padding(horizontal = Space.Gutter))
            }
            state.sessions.isEmpty() -> item(key = "empty") {
                EmptyState(
                    "No sessions yet",
                    "Start a practice — every pose you hold for 2 seconds is saved here.",
                    Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.L)
                ) { PrimaryButton("Start practice", onStartPractice, compact = true) }
            }
            else -> {
                item(key = "summary") { Summary(state.summary) }
                days.forEach { (day, sessions) ->
                    item(key = "day-$day") { DayHeader(dayHeading(day)) }
                    sessions.forEachIndexed { i, entry ->
                        val id = entry.session.id
                        item(key = id) {
                            Column {
                                if (i > 0) Hairline(inset = Space.Gutter)
                                SessionRow(
                                    item = entry,
                                    expanded = id in expanded,
                                    onToggle = { if (id in expanded) expanded.remove(id) else expanded.add(id) },
                                    onDelete = if (state.canEdit) ({ confirmDelete = entry.session }) else null
                                )
                            }
                        }
                    }
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
private fun Summary(s: HistorySummary) {
    Column(
        Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.M, bottom = Space.S),
        verticalArrangement = Arrangement.spacedBy(Space.L)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.L)) {
            Stat("Sessions", "${s.sessions}", Modifier.weight(1f))
            Stat("Time practised", HistoryAggregator.formatDuration(s.totalMinutes * 60), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.L)) {
            Stat("Best hold", s.bestHoldSec?.let { f1(it) } ?: "–", Modifier.weight(1f), unit = s.bestHoldSec?.let { "s" })
            Stat("Mean alignment", s.meanAlignment?.let { "${it.toInt()}" } ?: "–", Modifier.weight(1f), unit = s.meanAlignment?.let { "%" })
        }
    }
}

@Composable
private fun DayHeader(text: String) {
    Column(Modifier.fillMaxWidth().padding(top = Space.XL)) {
        Hairline()
        SectionHeader(text, Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = 20.dp, bottom = Space.XS))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionRow(item: SessionWithPoses, expanded: Boolean, onToggle: () -> Unit, onDelete: (() -> Unit)?) {
    val s = item.session
    val c = AppTheme.colors
    val manual = s.source == PracticeSessionEntity.SOURCE_MANUAL
    val whenText = formatSessionTime(s.startedAtMs)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse" else "Expand", onClick = onToggle)
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .padding(horizontal = Space.Gutter, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.L)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    Instant.ofEpochMilli(s.startedAtMs).atZone(ZoneId.systemDefault()).format(TIME),
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text
                )
                // Separate texts, so each value is its own readable unit.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val meta = mutableListOf(HistoryAggregator.formatDuration(s.activeSec))
                    if (!manual) {
                        meta += "${item.poses.size} ${if (item.poses.size == 1) "pose" else "poses"}"
                        HistoryAggregator.stability(item.poses)?.let { meta += "${it.toInt()}% stability" }
                    }
                    meta.forEachIndexed { i, m ->
                        if (i > 0) Text("·", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                        Text(m, style = MaterialTheme.typography.bodySmall, color = c.textTertiary, maxLines = 1)
                    }
                }
            }
            if (manual) {
                Text("Manual", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
            } else {
                s.meanScore?.let { score ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${score.toInt()}%", style = NumericStyle.copy(fontSize = 20.sp), color = scoreColor(score))
                        Text("alignment", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                    }
                }
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            Column(
                Modifier.fillMaxWidth().padding(start = Space.Gutter, end = Space.Gutter, bottom = Space.L),
                verticalArrangement = Arrangement.spacedBy(Space.L)
            ) {
                if (manual) Text("Logged by hand — no pose data or score.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                item.poses.forEach { PoseRow(it) }
                if (onDelete != null) {
                    TextAction(
                        "Delete session", onDelete,
                        Modifier
                            .offset(x = (-Space.S))
                            .semantics { contentDescription = "Delete session from $whenText" },
                        color = c.negative
                    )
                }
            }
        }
    }
}

@Composable
private fun PoseRow(p: PoseRecordEntity) {
    val c = AppTheme.colors
    val asana = Asana.entries.firstOrNull { it.name == p.asana }
    Row(horizontalArrangement = Arrangement.spacedBy(Space.M)) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(Radius.M)).background(c.control), contentAlignment = Alignment.Center) {
            asana?.let { PoseGlyph(it, Modifier.size(36.dp)) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(poseTitle(p), style = MaterialTheme.typography.titleSmall, color = c.text)
            Text("Held ${f1(p.lockedSec)} s (≥75%: ${f1(p.qualitySec)} s)", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            val parts = listOfNotNull(p.meanScore?.let { "${it.toInt()}% alignment" }, p.meanStability?.let { "${it.toInt()}% stability" })
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
