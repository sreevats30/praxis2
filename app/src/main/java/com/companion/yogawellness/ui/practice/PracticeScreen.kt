package com.companion.yogawellness.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.feedback.CoachUtterance
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PreflightCheckId
import com.companion.yogawellness.pose.PreflightResult
import com.companion.yogawellness.replay.DemoClips
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.CameraAngle
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.PoseGlyph
import com.companion.yogawellness.ui.components.RingGauge
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.OverlineStyle
import com.companion.yogawellness.ui.theme.ScoreColors
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

/** HUD surfaces over the camera feed: deep navy glass with a hairline edge. */
private val HudGlass = Color(0xBF07111F)
private val HudEdge = Color.White.copy(alpha = 0.12f)

@Composable
fun PracticeScreen(onBack: () -> Unit, vm: PracticeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var showExplain by rememberSaveable { mutableStateOf(false) }
    var showSafety by rememberSaveable { mutableStateOf(true) }
    var showReplayPicker by remember { mutableStateOf(false) }

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            delay(2500)
            vm.clearMessage()
        }
    }

    LifecycleStartEffect(vm) {
        vm.onStart()
        onStopOrDispose { vm.onStop() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Brand.Navy950, Color.Black)))
    ) {
        if (!state.isReplaying) {
            if (hasCamera) {
                val analyzer = remember(vm) { ImageAnalysis.Analyzer { proxy -> vm.landmarker.detectLiveStream(proxy) } }
                CameraPreview(
                    useFrontCamera = state.useFrontCamera,
                    analyzer = analyzer,
                    onCameraUnavailable = { cameraError = it },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CameraPermissionPrompt(Modifier.align(Alignment.Center)) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
        PoseOverlay(state.output, mirror = state.useFrontCamera, modifier = Modifier.fillMaxSize())

        // Top scrim keeps the toolbar legible over bright rooms.
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                LiveChip(state.isReplaying, state.fps)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(HudGlass)
                        .border(1.dp, HudEdge, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudIconButton(
                        if (state.voiceEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                        if (state.voiceEnabled) "Mute voice coach" else "Unmute voice coach",
                        vm::toggleVoice, bare = true
                    )
                    if (!state.isReplaying) {
                        HudIconButton(
                            if (state.recordingSec != null) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
                            if (state.recordingSec != null) "Stop recording" else "Record 20 s pose clip",
                            vm::toggleRecording, bare = true,
                            tint = if (state.recordingSec != null) Color.White else ScoreColors.Red
                        )
                    }
                    HudIconButton(Icons.Rounded.Replay, "Replay a clip", { showReplayPicker = true }, bare = true)
                    HudIconButton(Icons.Rounded.Cameraswitch, "Switch camera", vm::toggleCamera, bare = true)
                }
            }
            if (showSafety) SafetyBanner { showSafety = false }
            state.replayName?.let { ReplayBanner(it, vm::stopReplay) }
            state.recordingSec?.let { HudPill(String.format(Locale.US, "Recording landmarks %.0f / 20 s", it), Icons.Rounded.FiberManualRecord, ScoreColors.Red) }
            state.output?.safety?.let { SafetyAlertBanner(it.message) }
            state.output?.preflight?.let { if (!state.output!!.scoringArmed) PreflightPill(it) }
            (cameraError ?: state.error)?.let { ErrorPill(it) }
            state.message?.let { HudPill(it, Icons.Rounded.CheckCircle, Brand.Mint) }
        }

        BottomPanel(
            state = state,
            onExplain = { showExplain = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
        )
    }

    if (showExplain) {
        ExplainScoreSheet(state.output, state.profile) { showExplain = false }
    }
    if (showReplayPicker) {
        ReplayPickerDialog(
            clips = remember { vm.listClips() },
            onDemo = { showReplayPicker = false; vm.startDemoReplay() },
            onClip = { showReplayPicker = false; vm.startReplay(it) },
            onDismiss = { showReplayPicker = false }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Top HUD
// ---------------------------------------------------------------------------------------------

@Composable
private fun HudIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    bare: Boolean = false,
    tint: Color = Color.White
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (bare) Color.Transparent else HudGlass,
        border = if (bare) null else androidx.compose.foundation.BorderStroke(1.dp, HudEdge),
        modifier = Modifier.size(if (bare) 40.dp else 44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun LiveChip(replaying: Boolean, fps: Double) {
    val pulse = rememberInfiniteTransition(label = "live")
    val alpha by pulse.animateFloat(1f, 0.25f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot")
    Row(
        Modifier
            .clip(CircleShape)
            .background(HudGlass)
            .border(1.dp, HudEdge, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { this.alpha = alpha }
                .clip(CircleShape)
                .background(if (replaying) Brand.Cyan else ScoreColors.Red)
        )
        Text(if (replaying) "REPLAY" else "LIVE", style = OverlineStyle, color = Color.White)
        if (fps > 0) {
            Text(String.format(Locale.US, "%.0f FPS", fps), style = OverlineStyle, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun HudCard(
    modifier: Modifier = Modifier,
    color: Color = HudGlass,
    edge: Color = HudEdge,
    content: @Composable () -> Unit
) {
    Surface(color = color, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, edge), modifier = modifier) {
        content()
    }
}

@Composable
private fun HudPill(text: String, icon: ImageVector, tint: Color) {
    HudCard {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReplayPickerDialog(clips: List<File>, onDemo: () -> Unit, onClip: (File) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Replay, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Replay a pose clip") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Clips are replayed through the same scoring pipeline as the live camera.",
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onDemo) { Text(DemoClips.WARRIOR_CORRECTION_NAME) }
                if (clips.isEmpty()) {
                    Text("No recorded clips yet — use ⏺ to record 20 s.", style = MaterialTheme.typography.bodySmall)
                }
                clips.take(8).forEach { f -> TextButton(onClick = { onClip(f) }) { Text(f.nameWithoutExtension) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ReplayBanner(name: String, onStop: () -> Unit) {
    HudCard(color = Brand.Teal700.copy(alpha = 0.9f)) {
        Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Replay, contentDescription = null, tint = Brand.Mint, modifier = Modifier.size(18.dp))
            Text("Replaying: $name", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onStop) { Text("Stop", color = Brand.Mint, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun SafetyAlertBanner(message: String) {
    HudCard(color = ScoreColors.Red.copy(alpha = 0.94f), edge = Color.White.copy(alpha = 0.25f)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = "Safety alert", tint = Color.White, modifier = Modifier.size(24.dp))
            Text(message, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SafetyBanner(onDismiss: () -> Unit) {
    HudCard(color = Color(0xF2FFF6E0), edge = ScoreColors.Amber.copy(alpha = 0.4f)) {
        Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
            Text(
                "Practise on a non-slip surface. Stop immediately if you feel sharp pain, dizziness or joint strain.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4A3300),
                modifier = Modifier.weight(1f).padding(vertical = 10.dp)
            )
            Surface(onClick = onDismiss, color = Color.Transparent, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Color(0xFF4A3300))
                }
            }
        }
    }
}

private fun checkIcon(id: PreflightCheckId): ImageVector = when (id) {
    PreflightCheckId.FULL_BODY -> Icons.Rounded.CameraAlt
    PreflightCheckId.DISTANCE -> Icons.Rounded.Straighten
    PreflightCheckId.PHONE -> Icons.Rounded.PhoneAndroid
    PreflightCheckId.LIGHTING -> Icons.Rounded.LightMode
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreflightPill(preflight: PreflightResult) {
    val passed = preflight.checks.count { it.ok }
    HudCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PRE-FLIGHT CHECKS", style = OverlineStyle, color = Brand.Mint, modifier = Modifier.weight(1f))
                Text("$passed / ${preflight.checks.size}", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                preflight.checks.forEach { c ->
                    val bg by animateColorAsState(if (c.ok) ScoreColors.Green.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.10f), label = "check")
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(bg)
                            .border(1.dp, if (c.ok) Color.Transparent else ScoreColors.Red.copy(alpha = 0.7f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            if (c.ok) Icons.Rounded.Check else checkIcon(c.id),
                            contentDescription = null,
                            tint = if (c.ok) Color.White else ScoreColors.Red,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(c.id.label, color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            preflight.primaryMessage?.let { Text(it, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ErrorPill(message: String) {
    HudCard(color = ScoreColors.Red.copy(alpha = 0.9f)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(message, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CameraPermissionPrompt(modifier: Modifier, onRequest: () -> Unit) {
    Column(
        modifier
            .padding(28.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(HudGlass)
            .border(1.dp, HudEdge, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(Icons.Rounded.CameraAlt, Brand.Mint, size = 60.dp)
        Text("Camera access", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(
            "The camera is used only to estimate your pose on this phone. Frames are never saved or sent anywhere.",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        GradientButton(
            "Allow camera", onRequest, Modifier.fillMaxWidth(),
            icon = Icons.Rounded.CameraAlt,
            brush = Brush.horizontalGradient(listOf(Brand.Mint, Brand.Cyan)),
            contentColor = Brand.Navy900
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Bottom HUD
// ---------------------------------------------------------------------------------------------

@Composable
internal fun BottomPanel(state: PracticeUiState, onExplain: () -> Unit, modifier: Modifier) {
    val out = state.output
    val explain: @Composable () -> Unit = { ExplainChip(onExplain) }
    Surface(
        color = HudGlass,
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HudEdge),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val label = out?.lockedLabel
            when {
                !state.modelReady && state.error == null -> StatusRow(
                    { CircularProgressIndicator(Modifier.size(22.dp), color = Brand.Mint, strokeWidth = 2.5.dp) },
                    "Loading pose model…", "On-device AI warming up", explain
                )
                out == null -> StatusRow(
                    { IconBadge(Icons.Rounded.CameraAlt, Brand.Mint, size = 40.dp) },
                    "Set up your space", "Point the phone at your practice space, about 2–3 m away.", explain
                )
                !out.scoringArmed -> StatusRow(
                    { PoseGlyph(Asana.TADASANA, Modifier.size(44.dp), glow = false) },
                    "Stand in Tadasana", "Stand in Tadasana facing the phone. Scoring starts when every check is green.", explain
                )
                label == null -> {
                    val cand = out.lock?.candidate
                    if (cand != null && out.lock.candidateProgress > 0.3) {
                        StatusRow(
                            {
                                RingGauge(out.lock.candidateProgress.toFloat(), Brand.Mint, Modifier.size(44.dp), stroke = 4.dp, trackColor = Color.White.copy(alpha = 0.12f)) {
                                    PoseGlyph(cand.asana, Modifier.size(30.dp), glow = false)
                                }
                            },
                            "Detecting ${cand.asana.sanskritName}…", "Hold steady while the pose locks in", explain
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Move into a pose", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                            explain()
                        }
                        PoseChooser()
                        Text(
                            "Move into a pose: Vrikshasana, Virabhadrasana II, Trikonasana, or Utkatasana (turn 45°/side-on for Chair).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                label.asana == Asana.TADASANA -> {
                    val cal = out.calibration
                    StatusRow(
                        {
                            RingGauge(cal.progress.toFloat(), Brand.Mint, Modifier.size(44.dp), stroke = 4.dp, trackColor = Color.White.copy(alpha = 0.12f)) {
                                PoseGlyph(Asana.TADASANA, Modifier.size(30.dp), glow = false)
                            }
                        },
                        "Tadasana · neutral",
                        if (cal.isCalibrated) "Body proportions calibrated ✓" else "Hold still for 5 s to calibrate…",
                        explain
                    )
                }
                else -> ScoredPoseInfo(out, explain)
            }
            // The coach usually speaks the cue already on screen; show its caption only when it adds something.
            val shown = if (label != null && label.asana != Asana.TADASANA) scoredMessage(out) else null
            state.caption?.takeIf { it.text != shown }?.let { CoachCaption(it) }
        }
    }
}

/** Compact Σ button (with an "EXPLAIN" caption) that opens the live score breakdown. */
@Composable
private fun ExplainChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, HudEdge),
        modifier = Modifier
            .size(width = 54.dp, height = 50.dp)
            .semantics { contentDescription = "Explain Score" }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Functions, contentDescription = null, tint = Brand.Mint, modifier = Modifier.size(20.dp))
            Text("EXPLAIN", style = OverlineStyle.copy(fontSize = 8.sp, letterSpacing = 0.06.em), color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun StatusRow(leading: @Composable () -> Unit, title: String, text: String, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { leading() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.72f))
        }
        trailing()
    }
}

@Composable
private fun PoseChooser() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Asana.entries.filter { it.isScored }.forEach { a ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PoseGlyph(a, Modifier.size(40.dp), glow = false)
                Text(a.englishName.removeSuffix(" Pose"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
            }
        }
    }
}

@Composable
private fun CoachCaption(u: CoachUtterance) {
    val safety = u.kind == CoachUtterance.Kind.SAFETY
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Voice coach",
            tint = if (safety) ScoreColors.Red else Brand.Mint, modifier = Modifier.size(16.dp)
        )
        Text(
            "Coach: " + u.text,
            color = if (safety) ScoreColors.Red else Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Compact scored-pose HUD: score ring + pose identity, the one cue that matters now, and a single
 * row of hold/stability stats — kept short so the lower body stays visible.
 */
@Composable
private fun ScoredPoseInfo(out: PipelineOutput, trailing: @Composable () -> Unit) {
    val label = out.lockedLabel ?: return
    val score = out.score
    val color = scoreColor(score)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RingGauge(
            ((score ?: 0.0) / 100.0).toFloat(), color, Modifier.size(60.dp), stroke = 6.dp,
            trackColor = Color.White.copy(alpha = 0.12f),
            brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.6f), color))
        ) {
            Text(score?.let { "${it.toInt()}%" } ?: "–", style = NumericStyle.copy(fontSize = 17.sp), color = Color.White)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label.asana.sanskritName, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2)
            Text(
                label.asana.englishName + if (label.asana.hasSides) " · ${label.side.label}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
            if (label.asana.cameraAngle == CameraAngle.SIDE_OR_45) {
                Text(label.asana.cameraAngle.instruction, style = MaterialTheme.typography.labelSmall, color = Brand.Mint)
            }
        }
        trailing()
    }

    scoredMessage(out)?.let { msg ->
        when {
            out.safety != null -> CueCallout(msg, Icons.Rounded.WarningAmber, ScoreColors.Red, bold = true)
            out.alignment?.primaryCorrectiveCue != null -> CueCallout(msg, Icons.Rounded.Tune, ScoreColors.Amber)
            else -> CueCallout(msg, Icons.Rounded.CheckCircle, ScoreColors.Green)
        }
    }

    val h = out.hold
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HudStat(
            "HOLD",
            String.format(Locale.US, "%.1fs", h.graceHoldSec) + (label.asana.maxHoldSec?.let { "/$it" } ?: ""),
            Modifier.weight(1f)
        )
        HudStat("STRICT", String.format(Locale.US, "%.1fs", h.strictHoldSec), Modifier.weight(1f))
        HudStat("BEST", String.format(Locale.US, "%.1fs", h.bestGraceSec), Modifier.weight(1f))
        out.sway?.let { HudStat("STABLE", "${it.stability.toInt()}", Modifier.weight(1f)) }
        out.intraSymmetry?.let { HudStat("L/R", "${it.toInt()}%", Modifier.weight(1f)) }
    }
}

/** The one message the scored HUD shows: safety beats a corrective cue, which beats praise. */
private fun scoredMessage(out: PipelineOutput): String? = when {
    out.safety != null -> out.safety.message
    out.alignment?.primaryCorrectiveCue != null -> out.alignment.primaryCorrectiveCue
    out.score != null -> "Beautiful — hold it there"
    else -> null
}

@Composable
private fun CueCallout(text: String, icon: ImageVector, color: Color, bold: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HudStat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = OverlineStyle.copy(fontSize = 8.5.sp, letterSpacing = 0.06.em), color = Color.White.copy(alpha = 0.6f), maxLines = 1)
        Text(value, style = NumericStyle.copy(fontSize = 14.sp), color = Color.White, maxLines = 1)
    }
}
