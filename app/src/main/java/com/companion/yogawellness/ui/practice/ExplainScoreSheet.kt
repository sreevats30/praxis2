package com.companion.yogawellness.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.classifier.ClassificationResult
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.scoring.AlignmentScoreResult
import com.companion.yogawellness.scoring.AlignmentScorer
import com.companion.yogawellness.scoring.JointEvaluation
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.components.Callout
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.components.Pill
import com.companion.yogawellness.ui.components.ProgressTrack
import com.companion.yogawellness.ui.components.RingGauge
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.ScoreColors
import java.util.Locale

private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)
private fun f2(v: Double) = String.format(Locale.US, "%.2f", v)

/** "∑ Explain Score" live bottom sheet (§3): every term of the §7.1 formula, joint by joint. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainScoreSheet(output: PipelineOutput?, profile: ToleranceProfile, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        ExplainScoreContent(output, profile)
    }
}

@Composable
fun ExplainScoreContent(output: PipelineOutput?, profile: ToleranceProfile) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Rounded.Functions, MaterialTheme.colorScheme.primary, size = 42.dp, filled = true)
            Column {
                Overline("Transparent scoring", color = AppTheme.colors.inkMuted)
                Text("∑ Explain Score", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        val label = output?.lockedLabel
        val alignment = output?.alignment
        if (label == null || alignment == null || alignment.finalScore == null) {
            Callout(
                when {
                    output == null || !output.scoringArmed -> "Scoring starts once the pre-flight checklist is green."
                    label == null -> "Move into one of the poses. The breakdown appears once a pose is locked."
                    !label.asana.isScored -> "Tadasana is the neutral calibration pose — it is not scored."
                    else -> alignment?.primaryCorrectiveCue ?: "Not enough of your body is visible to score."
                },
                Icons.Rounded.Info,
                AppTheme.colors.info
            )
            ClassifierSection(output?.classification)
            Spacer(Modifier.height(24.dp))
            return
        }

        ScoreHeader(label.toString(), alignment, profile)
        FormulaCard(profile)
        Overline("Joint by joint", color = AppTheme.colors.inkMuted, modifier = Modifier.padding(top = 4.dp))
        alignment.evaluations.forEach { JointRow(it, profile) }
        SummaryCard(alignment)

        PanelCard(spacing = 6.dp, elevation = 0.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), borderColor = null) {
            output.sway?.let {
                Text(
                    "Stability ${it.stability.toInt()} / 100 — detrended hip sway SD ${f1(it.swaySdPx)} px " +
                        "= ${f2(it.normalizedSway * 100)}% of torso length.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Hold timer: strict ${f1(output.hold.strictHoldSec)} s · grace-adjusted ${f1(output.hold.graceHoldSec)} s " +
                    "(runs while score ≥ 75%).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ClassifierSection(output.classification)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ScoreHeader(title: String, a: AlignmentScoreResult, profile: ToleranceProfile) {
    val final = a.finalScore ?: 0.0
    val color = scoreColor(final)
    PanelCard(spacing = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RingGauge((final / 100).toFloat(), color, Modifier.size(92.dp), stroke = 9.dp) {
                Text("${final.toInt()}%", style = NumericStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize), color = MaterialTheme.colorScheme.onSurface)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Pill("${profile.label} tolerance · m = ${profile.multiplier}", color = MaterialTheme.colorScheme.primary)
                if (a.isCappedByCritical) Pill("Capped by a critical joint", color = ScoreColors.Red)
            }
        }
    }
}

@Composable
private fun FormulaCard(profile: ToleranceProfile) {
    Surface(color = Brand.Navy900, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Overline("Model", color = Brand.Mint)
            val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color(0xFFE0FBFF))
            Text("Δᵢ = max(0, |θᵢ − μᵢ| − m·τᵢ)   m = ${profile.multiplier} (${profile.label})", style = mono)
            Text("sᵢ = 100 · max(0, 1 − (Δᵢ / d_maxᵢ)^1.2)", style = mono)
            Text("S_w = Σ wᵢsᵢ / Σ wᵢ   (visible joints only)", style = mono)
            Text("S = min(S_w, 15 + 0.85 · min critical sᵢ)", style = mono)
        }
    }
}

@Composable
private fun JointRow(e: JointEvaluation, profile: ToleranceProfile) {
    val tol = e.spec.toleranceDeg * profile.multiplier
    val color = scoreColor(e.jointScore)
    PanelCard(spacing = 8.dp, elevation = 4.dp, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(e.spec.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (e.spec.isCritical) Pill("critical", color = MaterialTheme.colorScheme.secondary)
            Text("${f1(e.jointScore)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
        ProgressTrack((e.jointScore / 100.0).toFloat(), color, height = 6.dp)
        Text(
            "θ ${f1(e.measuredDeg)}° · μ ${e.spec.targetDeg.toInt()}° ± ${f1(tol)}° · Δ ${f1(e.excessDeg)}° · " +
                "d_max ${e.spec.maxDeviationDeg.toInt()}° · w ${f2(e.spec.weight)} · deficit ${f1(e.weightedDeficit)}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        e.directionalCue?.let { Text("→ $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color) }
    }
}

@Composable
private fun SummaryCard(a: AlignmentScoreResult) {
    PanelCard(spacing = 4.dp, elevation = 4.dp) {
        Overline("Result", color = AppTheme.colors.inkMuted)
        Text("Weighted mean S_w = ${f1(a.weightedScore)}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Critical cap = 15 + 0.85 × ${f1((a.criticalCapValue - AlignmentScorer.CAP_OFFSET) / AlignmentScorer.CAP_SLOPE)} = ${f1(a.criticalCapValue)}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Final score S = ${f1(a.finalScore ?: 0.0)}%" + if (a.isCappedByCritical) "  (capped by a critical joint)" else "",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scoreColor(a.finalScore)
        )
    }
}

@Composable
private fun ClassifierSection(c: ClassificationResult?) {
    if (c == null || c.features == null) return
    PanelCard(spacing = 6.dp, elevation = 0.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), borderColor = null) {
        Text("Why this pose? (rules first, then prototype distance)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        c.ruleTrace.forEach { rc ->
            Text(
                "${if (rc.passed) "✓" else "✗"} ${rc.asana.sanskritName}: ${rc.detail}",
                style = MaterialTheme.typography.bodySmall,
                color = if (rc.passed) AppTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (c.prototypeDistances.isNotEmpty()) {
            Text(
                "Prototype distance: " + c.prototypeDistances.entries.sortedBy { it.value }
                    .joinToString(" · ") { "${it.key.sanskritName} ${f2(it.value)}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
