package com.companion.yogawellness.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PreflightResult
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.CameraAngle
import com.companion.yogawellness.ui.theme.ScoreColors
import java.util.Locale

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

    LifecycleStartEffect(vm) {
        vm.onStart()
        onStopOrDispose { vm.onStop() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCamera) {
            val analyzer = remember(vm) { ImageAnalysis.Analyzer { proxy -> vm.landmarker.detectLiveStream(proxy) } }
            CameraPreview(
                useFrontCamera = state.useFrontCamera,
                analyzer = analyzer,
                onCameraUnavailable = { cameraError = it },
                modifier = Modifier.fillMaxSize()
            )
            PoseOverlay(state.output, mirror = state.useFrontCamera, modifier = Modifier.fillMaxSize())
        } else {
            CameraPermissionPrompt(Modifier.align(Alignment.Center)) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OverlayIconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text(
                    if (state.fps > 0) String.format(Locale.US, "%.0f FPS", state.fps) else "",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                ScoreBadge(state.output)
                OverlayIconButton(vm::toggleCamera) { Icon(Icons.Default.Cameraswitch, contentDescription = "Switch camera") }
            }
            if (showSafety) SafetyBanner { showSafety = false }
            state.output?.preflight?.let { if (!state.output!!.scoringArmed) PreflightPill(it) }
            (cameraError ?: state.error)?.let { ErrorPill(it) }
        }

        BottomPanel(
            state = state,
            onExplain = { showExplain = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(10.dp)
        )
    }

    if (showExplain) {
        ExplainScoreSheet(state.output, state.profile) { showExplain = false }
    }
}

@Composable
private fun OverlayIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White)
    ) { content() }
}

@Composable
private fun ScoreBadge(output: PipelineOutput?) {
    val score = output?.score
    if (score == null || output.lockedLabel == null) return
    Box(
        Modifier
            .padding(end = 8.dp)
            .size(64.dp)
            .background(scoreColor(score), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("${score.toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun SafetyBanner(onDismiss: () -> Unit) {
    Surface(color = Color(0xE6FFF4D6), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Practise on a non-slip surface. Stop immediately if you feel sharp pain, dizziness or joint strain.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4A3B00),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF4A3B00)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreflightPill(preflight: PreflightResult) {
    Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                preflight.checks.forEach { c ->
                    Text(
                        (if (c.ok) "✓ " else "• ") + c.id.label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .background(if (c.ok) ScoreColors.Green else ScoreColors.Red, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            preflight.primaryMessage?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ErrorPill(message: String) {
    Surface(color = ScoreColors.Red.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp)) {
        Text(message, color = Color.White, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CameraPermissionPrompt(modifier: Modifier, onRequest: () -> Unit) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "The camera is used only to estimate your pose on this phone. Frames are never saved or sent anywhere.",
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRequest) { Text("Allow camera") }
    }
}

@Composable
internal fun BottomPanel(state: PracticeUiState, onExplain: () -> Unit, modifier: Modifier) {
    val out = state.output
    Surface(color = Color.Black.copy(alpha = 0.62f), shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val label = out?.lockedLabel
            when {
                !state.modelReady && state.error == null -> PanelText("Loading pose model…")
                out == null -> PanelText("Point the phone at your practice space, about 2–3 m away.")
                !out.scoringArmed -> PanelText("Stand in Tadasana facing the phone. Scoring starts when every check is green.")
                label == null -> {
                    val cand = out.lock?.candidate
                    if (cand != null && out.lock.candidateProgress > 0.3) {
                        PanelText("Detecting ${cand.asana.sanskritName}…", bold = true)
                        LinearProgressIndicator(progress = { out.lock.candidateProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    } else {
                        PanelText("Move into a pose: Vrikshasana, Virabhadrasana II, Trikonasana, or Utkatasana (turn 45°/side-on for Chair).")
                    }
                }
                label.asana == Asana.TADASANA -> {
                    PanelText("Tadasana · neutral", bold = true)
                    if (out.calibration.isCalibrated) PanelText("Body proportions calibrated ✓")
                    else {
                        PanelText("Hold still for 5 s to calibrate…")
                        LinearProgressIndicator(progress = { out.calibration.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    }
                }
                else -> ScoredPoseInfo(out)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onExplain) { Text("∑ Explain Score") }
            }
        }
    }
}

@Composable
private fun ScoredPoseInfo(out: PipelineOutput) {
    val label = out.lockedLabel ?: return
    PanelText(label.asana.displayName + if (label.asana.hasSides) " · ${label.side.label}" else "", bold = true)
    if (label.asana.cameraAngle == CameraAngle.SIDE_OR_45) {
        PanelText(label.asana.cameraAngle.instruction, small = true)
    }
    val cue = out.alignment?.primaryCorrectiveCue
    if (cue != null) {
        Text(cue, color = ScoreColors.Amber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    } else if (out.score != null) {
        Text("Beautiful — hold it there", color = ScoreColors.Green, style = MaterialTheme.typography.titleMedium)
    }
    val h = out.hold
    PanelText(
        String.format(Locale.US, "Hold %.1f s (strict %.1f s) · best %.1f s", h.graceHoldSec, h.strictHoldSec, h.bestGraceSec)
            + (label.asana.maxHoldSec?.let { " / $it s max" } ?: "")
    )
    val extras = listOfNotNull(
        out.sway?.let { "Stability ${it.stability.toInt()}" },
        out.intraSymmetry?.let { "L/R symmetry ${it.toInt()}%" }
    )
    if (extras.isNotEmpty()) PanelText(extras.joinToString(" · "), small = true)
}

@Composable
private fun PanelText(text: String, bold: Boolean = false, small: Boolean = false) {
    Text(
        text,
        color = Color.White,
        style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}
