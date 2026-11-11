package com.companion.yogawellness.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.classifier.ClassificationResult
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.scoring.AlignmentScoreResult
import com.companion.yogawellness.scoring.AlignmentScorer
import com.companion.yogawellness.scoring.JointEvaluation
import com.companion.yogawellness.scoring.ToleranceProfile
import java.util.Locale

private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)
private fun f2(v: Double) = String.format(Locale.US, "%.2f", v)

/** "∑ Explain Score" live bottom sheet (§3): every term of the §7.1 formula, joint by joint. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainScoreSheet(output: PipelineOutput?, profile: ToleranceProfile, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("∑ Explain Score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val label = output?.lockedLabel
        val alignment = output?.alignment
        if (label == null || alignment == null || alignment.finalScore == null) {
            Text(
                when {
                    output == null || !output.scoringArmed -> "Scoring starts once the pre-flight checklist is green."
                    label == null -> "Move into one of the poses. The breakdown appears once a pose is locked."
                    !label.asana.isScored -> "Tadasana is the neutral calibration pose — it is not scored."
                    else -> alignment?.primaryCorrectiveCue ?: "Not enough of your body is visible to score."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            ClassifierSection(output?.classification)
            Spacer(Modifier.height(24.dp))
            return
        }

        Text(label.toString(), style = MaterialTheme.typography.titleMedium)
        FormulaCard(profile)
        alignment.evaluations.forEach { JointRow(it, profile) }
        SummaryCard(alignment)

        output.sway?.let {
            Text(
                "Stability ${it.stability.toInt()} / 100 — detrended hip sway SD ${f1(it.swaySdPx)} px " +
                    "= ${f2(it.normalizedSway * 100)}% of torso length.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            "Hold timer: strict ${f1(output.hold.strictHoldSec)} s · grace-adjusted ${f1(output.hold.graceHoldSec)} s " +
                "(runs while score ≥ 75%).",
            style = MaterialTheme.typography.bodySmall
        )
        ClassifierSection(output.classification)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FormulaCard(profile: ToleranceProfile) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(e.spec.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (e.spec.isCritical) {
                Text(
                    "critical",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text("${f1(e.jointScore)}%", fontWeight = FontWeight.Bold, color = scoreColor(e.jointScore))
        }
        LinearProgressIndicator(
            progress = { (e.jointScore / 100.0).toFloat() },
            color = scoreColor(e.jointScore),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "θ ${f1(e.measuredDeg)}° · μ ${e.spec.targetDeg.toInt()}° ± ${f1(tol)}° · Δ ${f1(e.excessDeg)}° · " +
                "d_max ${e.spec.maxDeviationDeg.toInt()}° · w ${f2(e.spec.weight)} · deficit ${f1(e.weightedDeficit)}",
            style = MaterialTheme.typography.bodySmall
        )
        e.directionalCue?.let { Text("→ $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun SummaryCard(a: AlignmentScoreResult) {
    HorizontalDivider()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Weighted mean S_w = ${f1(a.weightedScore)}%")
        Text("Critical cap = 15 + 0.85 × ${f1((a.criticalCapValue - AlignmentScorer.CAP_OFFSET) / AlignmentScorer.CAP_SLOPE)} = ${f1(a.criticalCapValue)}%")
        Text(
            "Final score S = ${f1(a.finalScore ?: 0.0)}%" + if (a.isCappedByCritical) "  (capped by a critical joint)" else "",
            fontWeight = FontWeight.Bold,
            color = scoreColor(a.finalScore)
        )
    }
    HorizontalDivider()
}

@Composable
private fun ClassifierSection(c: ClassificationResult?) {
    if (c == null || c.features == null) return
    Text("Why this pose? (rules first, then prototype distance)", style = MaterialTheme.typography.titleSmall)
    c.ruleTrace.forEach { rc ->
        Text(
            "${if (rc.passed) "✓" else "✗"} ${rc.asana.sanskritName}: ${rc.detail}",
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (c.prototypeDistances.isNotEmpty()) {
        Text(
            "Prototype distance: " + c.prototypeDistances.entries.sortedBy { it.value }
                .joinToString(" · ") { "${it.key.sanskritName} ${f2(it.value)}" },
            style = MaterialTheme.typography.bodySmall
        )
    }
}
