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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.classifier.ClassificationResult
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.scoring.AlignmentScoreResult
import com.companion.yogawellness.scoring.AlignmentScorer
import com.companion.yogawellness.scoring.JointEvaluation
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Meter
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.Ring
import com.companion.yogawellness.ui.components.SectionLabel
import com.companion.yogawellness.ui.components.Tag
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import java.util.Locale

private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)
private fun f2(v: Double) = String.format(Locale.US, "%.2f", v)

/** "How this score works" live bottom sheet (§3): every term of the §7.1 formula, joint by joint. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainScoreSheet(output: PipelineOutput?, profile: ToleranceProfile, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppTheme.colors.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        ExplainScoreContent(output, profile)
    }
}

@Composable
fun ExplainScoreContent(output: PipelineOutput?, profile: ToleranceProfile) {
    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text("How this score works", style = MaterialTheme.typography.headlineSmall, color = c.text)
            Text("Every term of the live alignment score, joint by joint", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
        }
        val label = output?.lockedLabel
        val alignment = output?.alignment
        if (label == null || alignment == null || alignment.finalScore == null) {
            Notice(
                when {
                    output == null || !output.scoringArmed -> "Scoring starts once the pre-flight checklist is green."
                    label == null -> "Move into one of the poses. The breakdown appears once a pose is locked."
                    !label.asana.isScored -> "Tadasana is the neutral calibration pose — it is not scored."
                    else -> alignment?.primaryCorrectiveCue ?: "Not enough of your body is visible to score."
                },
                Icons.Outlined.Info
            )
            ClassifierSection(output?.classification)
            Spacer(Modifier.height(24.dp))
            return
        }

        ScoreHeader(label.toString(), alignment, profile)
        FormulaCard(profile)
        SectionLabel("Joints")
        Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
            alignment.evaluations.forEachIndexed { i, e ->
                if (i > 0) Hairline()
                JointRow(e, profile)
            }
        }
        SummaryCard(alignment)
        Panel(spacing = 6.dp) {
            output.sway?.let {
                Text(
                    "Stability ${it.stability.toInt()} / 100 — detrended hip sway SD ${f1(it.swaySdPx)} px " +
                        "= ${f2(it.normalizedSway * 100)}% of torso length.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary
                )
            }
            Text(
                "Hold timer: strict ${f1(output.hold.strictHoldSec)} s · grace-adjusted ${f1(output.hold.graceHoldSec)} s " +
                    "(runs while score ≥ 75%).",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary
            )
        }
        ClassifierSection(output.classification)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ScoreHeader(title: String, a: AlignmentScoreResult, profile: ToleranceProfile) {
    val c = AppTheme.colors
    val final = a.finalScore ?: 0.0
    val color = scoreColor(final)
    Panel(spacing = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Ring((final / 100).toFloat(), color, Modifier.size(72.dp), stroke = 6.dp) {
                Text("${final.toInt()}%", style = NumericStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = c.text)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
                Text("${profile.label} tolerance · m = ${profile.multiplier}", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                if (a.isCappedByCritical) Tag("Capped by a critical joint", tone = Tone.NEGATIVE)
            }
        }
    }
}

@Composable
private fun FormulaCard(profile: ToleranceProfile) {
    val c = AppTheme.colors
    Panel(color = c.sunken, border = null, spacing = 4.dp) {
        Text("Formula", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = c.text)
        // Formulas never wrap mid-term; on narrow screens the block scrolls sideways instead.
        Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Δᵢ = max(0, |θᵢ − μᵢ| − m·τᵢ)", style = mono, softWrap = false)
            Text("sᵢ = 100 · max(0, 1 − (Δᵢ/d_maxᵢ)^1.2)", style = mono, softWrap = false)
            Text("S_w = Σ wᵢsᵢ / Σ wᵢ", style = mono, softWrap = false)
            Text("S = min(S_w, 15 + 0.85·min critical sᵢ)", style = mono, softWrap = false)
        }
        Text(
            "m = ${profile.multiplier} (${profile.label} tolerance) · S_w counts visible joints only",
            style = MaterialTheme.typography.bodySmall,
            color = c.textTertiary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun JointRow(e: JointEvaluation, profile: ToleranceProfile) {
    val c = AppTheme.colors
    val tol = e.spec.toleranceDeg * profile.multiplier
    val color = scoreColor(e.jointScore)
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(e.spec.name, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
            if (e.spec.isCritical) Tag("Critical")
            Text("${f1(e.jointScore)}%", style = NumericStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize), color = c.text)
        }
        Meter((e.jointScore / 100.0).toFloat(), color)
        Text(
            "θ ${f1(e.measuredDeg)}° · μ ${e.spec.targetDeg.toInt()}° ± ${f1(tol)}° · Δ ${f1(e.excessDeg)}° · " +
                "d_max ${e.spec.maxDeviationDeg.toInt()}° · w ${f2(e.spec.weight)} · deficit ${f1(e.weightedDeficit)}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = c.textSecondary
        )
        e.directionalCue?.let { Text("→ $it", style = MaterialTheme.typography.bodySmall, color = c.text) }
    }
}

@Composable
private fun SummaryCard(a: AlignmentScoreResult) {
    val c = AppTheme.colors
    Panel(spacing = 4.dp) {
        Text("Result", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        Text("Weighted mean S_w = ${f1(a.weightedScore)}%", style = MaterialTheme.typography.bodyMedium, color = c.text)
        Text(
            "Critical cap = 15 + 0.85 × ${f1((a.criticalCapValue - AlignmentScorer.CAP_OFFSET) / AlignmentScorer.CAP_SLOPE)} = ${f1(a.criticalCapValue)}%",
            style = MaterialTheme.typography.bodyMedium,
            color = c.text
        )
        Text(
            "Final score S = ${f1(a.finalScore ?: 0.0)}%" + if (a.isCappedByCritical) "  (capped by a critical joint)" else "",
            style = MaterialTheme.typography.titleSmall,
            color = c.text
        )
    }
}

@Composable
private fun ClassifierSection(c: ClassificationResult?) {
    if (c == null || c.features == null) return
    val colors = AppTheme.colors
    Panel(spacing = 6.dp) {
        Text("Why this pose? (rules first, then prototype distance)", style = MaterialTheme.typography.titleSmall, color = colors.text)
        c.ruleTrace.forEach { rc ->
            Text(
                "${if (rc.passed) "✓" else "✗"} ${rc.asana.sanskritName}: ${rc.detail}",
                style = MaterialTheme.typography.bodySmall,
                color = if (rc.passed) colors.positive else colors.textSecondary
            )
        }
        if (c.prototypeDistances.isNotEmpty()) {
            Text(
                "Prototype distance: " + c.prototypeDistances.entries.sortedBy { it.value }
                    .joinToString(" · ") { "${it.key.sanskritName} ${f2(it.value)}" },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}
