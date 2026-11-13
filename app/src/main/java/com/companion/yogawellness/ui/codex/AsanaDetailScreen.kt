package com.companion.yogawellness.ui.codex

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.CodexText
import com.companion.yogawellness.codex.CodexView
import com.companion.yogawellness.codex.Difficulty
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.practice.animationsDisabled
import com.companion.yogawellness.ui.theme.ScoreColors

/** Route entry: looks the pose up by id; an unknown id shows "Pose not found". */
@Composable
fun AsanaDetailScreen(id: String, onBack: () -> Unit, onPracticeLive: (Asana) -> Unit) {
    val item = remember(id) { AsanaCatalog32.byId(id) }
    if (item == null) {
        PoseNotFound(onBack)
    } else {
        // Reduced motion: no auto-play; the static peak pose is shown with a visible Play button.
        AsanaDetailContent(item, initialPlaying = !animationsDisabled(), onBack = onBack, onPracticeLive = onPracticeLive)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AsanaDetailContent(
    item: AsanaCodexItem,
    initialPlaying: Boolean,
    onBack: () -> Unit,
    onPracticeLive: (Asana) -> Unit
) {
    val reducedMotion = animationsDisabled()
    var playing by rememberSaveable(item.id) { mutableStateOf(initialPlaying) }
    var halfSpeed by rememberSaveable(item.id) { mutableStateOf(false) }
    var viewName by rememberSaveable(item.id) { mutableStateOf(item.defaultView.name) }
    var showAngles by rememberSaveable(item.id) { mutableStateOf(true) }
    val view = CodexView.valueOf(viewName)
    val yaw by animateFloatAsState(view.yawDeg, if (reducedMotion) snap() else tween(300), label = "yaw")
    val breathTime = remember(item.id) { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.englishName) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${item.number}. ${item.sanskritName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
                Text(item.englishName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(item.category.shortTitle, MaterialTheme.colorScheme.outline)
                    Tag(item.difficulty.label, if (item.difficulty == Difficulty.INTERMEDIATE) ScoreColors.Amber else MaterialTheme.colorScheme.outline)
                    Tag("Hold about ${item.recommendedHoldSec} s", MaterialTheme.colorScheme.outline)
                    if (item.isLive) Tag("Live-scored", ScoreColors.Green)
                }
            }
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Text(
                    "Daily-life benefit: ${item.everydayBenefit}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp).fillMaxWidth()
                )
            }

            SkeletonDemoCanvas(
                item = item,
                playing = playing,
                speed = if (halfSpeed) 0.5f else 1f,
                yawDeg = yaw,
                showAngles = showAngles,
                modifier = Modifier.fillMaxWidth(),
                breathTime = breathTime
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(onClick = { playing = !playing }) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                    Text(if (playing) "Pause at peak" else "Play", Modifier.padding(start = 6.dp))
                }
                FilterChip(selected = halfSpeed, onClick = { halfSpeed = true }, label = { Text("0.5×") })
                FilterChip(selected = !halfSpeed, onClick = { halfSpeed = false }, label = { Text("1×") })
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CodexView.entries.forEach { v ->
                    FilterChip(selected = view == v, onClick = { viewName = v.name }, label = { Text(v.label) })
                }
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .toggleable(value = showAngles, role = Role.Switch, onValueChange = { showAngles = it })
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(checked = showAngles, onCheckedChange = null)
                    Text("Show angles")
                }
            }
            BreathPacer(item.breath, breathTime)

            Section("Key angles") {
                item.callouts.forEach { c ->
                    Text("• ${c.label}: ${c.targetDeg}° — ${c.joint.description}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Section("Steps") {
                item.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium) }
            }
            Section("Common mistakes") {
                item.commonMistakes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
            if (item.modifications.isNotEmpty()) {
                Section("Make it easier") {
                    item.modifications.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            Surface(color = ScoreColors.Amber.copy(alpha = 0.16f), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Safety", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(item.safety, style = MaterialTheme.typography.bodyMedium)
                    Text(CodexText.GLOBAL_DISCLAIMER, style = MaterialTheme.typography.bodySmall)
                }
            }
            val live = item.liveAsana
            if (live != null) {
                Button(onClick = { onPracticeLive(live) }, modifier = Modifier.fillMaxWidth()) { Text("Practice this live") }
            } else {
                // Deliberately not a button: these poses are never scored or recorded.
                Text(
                    CodexText.FOLLOW_ALONG_NOTE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() }
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoseNotFound(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learn") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { inner ->
        Column(Modifier.padding(inner).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pose not found", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onBack) { Text("Back to Learn") }
        }
    }
}
