package com.companion.yogawellness.ui.codex

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.CodexText
import com.companion.yogawellness.codex.CodexView
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.components.Callout
import com.companion.yogawellness.ui.components.ClinicalTopBar
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.practice.animationsDisabled
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
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

@OptIn(ExperimentalLayoutApi::class)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ClinicalTopBar(
                title = item.englishName,
                subtitle = "${item.number}. ${item.sanskritName}",
                onBack = onBack,
                bottom = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPill(item.category.shortTitle)
                        GlassPill(item.difficulty.label, iconTint = Brand.Gold)
                        GlassPill("Hold about ${item.recommendedHoldSec} s", icon = Icons.Rounded.Timer)
                        if (item.isLive) GlassPill("Live-scored", icon = Icons.Rounded.Videocam)
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "${item.number}. ${item.sanskritName}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            Callout("Daily-life benefit: ${item.everydayBenefit}", Icons.Rounded.WbSunny, AppTheme.colors.info)

            // Demo + controls
            PanelCard(
                color = AppTheme.colors.heroStart,
                borderColor = null,
                contentPadding = PaddingValues(10.dp),
                spacing = 10.dp
            ) {
                SkeletonDemoCanvas(
                    item = item,
                    playing = playing,
                    speed = if (halfSpeed) 0.5f else 1f,
                    yawDeg = yaw,
                    showAngles = showAngles,
                    modifier = Modifier.fillMaxWidth(),
                    breathTime = breathTime
                )
                // FlowRows so the controls wrap instead of clipping at large font sizes.
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { playing = !playing },
                        shape = RoundedCornerShape(14.dp),
                        color = Brand.Mint,
                        contentColor = Brand.Navy900,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(if (playing) "Pause at peak" else "Play", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Segmented(listOf("0.5×", "1×"), if (halfSpeed) 0 else 1) { halfSpeed = it == 0 }
                }
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Segmented(CodexView.entries.map { it.label }, CodexView.entries.indexOf(view)) { viewName = CodexView.entries[it].name }
                    Row(
                        Modifier
                            .heightIn(min = 48.dp)
                            .toggleable(value = showAngles, role = Role.Switch, onValueChange = { showAngles = it })
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Show angles", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Switch(
                            checked = showAngles, onCheckedChange = null,
                            colors = SwitchDefaults.colors(checkedTrackColor = Brand.Teal500, checkedThumbColor = Color.White)
                        )
                    }
                }
            }

            PanelCard(spacing = 10.dp, elevation = 6.dp) {
                Overline("Breathe", color = AppTheme.colors.inkMuted)
                BreathPacer(item.breath, breathTime)
            }

            DetailSection("Key angles", Icons.Rounded.Straighten, MaterialTheme.colorScheme.primary) {
                item.callouts.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .size(width = 54.dp, height = 34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brand.Gold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${c.targetDeg}°", style = MaterialTheme.typography.labelLarge, color = Color(0xFF92400E))
                        }
                        Text("${c.label}: ${c.targetDeg}° — ${c.joint.description}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            DetailSection("Steps", Icons.Rounded.FormatListNumbered, Brand.Practice) {
                item.steps.forEachIndexed { i, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(Brand.Practice.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = Brand.Practice)
                        }
                        Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            DetailSection("Common mistakes", Icons.Rounded.ErrorOutline, ScoreColors.Red) {
                item.commonMistakes.forEach { BulletRow(it, Icons.Rounded.Close, ScoreColors.Red) }
            }
            if (item.modifications.isNotEmpty()) {
                DetailSection("Make it easier", Icons.Rounded.Accessibility, ScoreColors.Green) {
                    item.modifications.forEach { BulletRow(it, Icons.Rounded.Check, ScoreColors.Green) }
                }
            }
            Callout(item.safety, Icons.Rounded.WarningAmber, ScoreColors.Amber, title = "Safety") {
                Text(CodexText.GLOBAL_DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val live = item.liveAsana
            if (live != null) {
                GradientButton("Practice this live", { onPracticeLive(live) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Videocam)
            } else {
                // Deliberately not a button: these poses are never scored or recorded.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(CodexText.FOLLOW_ALONG_NOTE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Pill segmented control on the dark demo card. */
@Composable
private fun Segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(i) })
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Brand.Navy900 else Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, icon: ImageVector, accent: Color, content: @Composable () -> Unit) {
    PanelCard(spacing = 12.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(icon, accent, size = 34.dp)
            Text(
                title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
        }
        content()
    }
}

@Composable
private fun BulletRow(text: String, icon: ImageVector, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(top = 2.dp).size(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PoseNotFound(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ClinicalTopBar(title = "Learn", onBack = onBack) }
    ) { inner ->
        Column(Modifier.padding(inner).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(Icons.Rounded.Search, MaterialTheme.colorScheme.primary, size = 52.dp)
            Text("Pose not found", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            GradientButton("Back to Learn", onBack, Modifier.fillMaxWidth())
        }
    }
}
