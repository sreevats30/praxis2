package com.companion.yogawellness.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.classifier.ClassificationResult
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.scoring.AlignmentScoreResult
import com.companion.yogawellness.scoring.AlignmentScorer
import com.companion.yogawellness.scoring.JointEvaluation
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Meter
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.Ring
import com.companion.yogawellness.ui.components.Section
import com.companion.yogawellness.ui.components.StatusLabel
import com.companion.yogawellness.ui.components.Tag
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.Radius
import com.companion.yogawellness.ui.theme.Space
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
        containerColor = AppTheme.colors.raised,
        shape = RoundedCornerShape(topStart = Radius.L, topEnd = Radius.L)
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
            .padding(bottom = Space.XL)
    ) {
        Column(Modifier.padding(horizontal = Space.Gutter, vertical = Space.S), verticalArrangement = Arrangement.spacedBy(Space.XS)) {
            Text("How this score works", style = MaterialTheme.typography.headlineMedium, color = c.text)
            Text("Every term of the live alignment score, joint by joint.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
        val label = output?.lockedLabel
        val alignment = output?.alignment
        if (label == null || alignment == null || alignment.finalScore == null) {
            Section(divider = false) {
                Notice(
                    when {
                        output == null || !output.scoringArmed -> "Scoring starts once the pre-flight checklist is green."
                        label == null -> "Move into one of the poses. The breakdown appears once a pose is locked."
                        !label.asana.isScored -> "Tadasana is the neutral calibration pose — it is not scored."
                        else -> alignment?.primaryCorrectiveCue ?: "Not enough of your body is visible to score."
                    }
                )
            }
            ClassifierSection(output?.classification)
            return
        }

        Section(divider = false) { ScoreHeader(label.toString(), alignment, profile) }
        Section(title = "Formula") { Formula(profile) }
        Section(title = "Joints", spacing = 0.dp) {
            alignment.evaluations.forEachIndexed { i, e ->
                if (i > 0) Hairline()
                JointRow(e, profile)
            }
        }
        Section(title = "Result", spacing = Space.XS) {
            Text("Weighted mean S_w = ${f1(alignment.weightedScore)}%", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            Text(
                "Critical cap = 15 + 0.85 × ${f1((alignment.criticalCapValue - AlignmentScorer.CAP_OFFSET) / AlignmentScorer.CAP_SLOPE)} = ${f1(alignment.criticalCapValue)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary
            )
            Text(
                "Final score S = ${f1(alignment.finalScore)}%" + if (alignment.isCappedByCritical) "  (capped by a critical joint)" else "",
                style = MaterialTheme.typography.titleSmall,
                color = c.text,
                modifier = Modifier.padding(top = Space.XS)
            )
        }
        Section(title = "Hold and stability", spacing = Space.XS) {
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
    }
}

@Composable
private fun ScoreHeader(title: String, a: AlignmentScoreResult, profile: ToleranceProfile) {
    val c = AppTheme.colors
    val final = a.finalScore ?: 0.0
    val color = scoreColor(final)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.L)) {
        Ring((final / 100).toFloat(), color, Modifier.size(72.dp), stroke = 5.dp) {
            Text("${final.toInt()}%", style = NumericStyle.copy(fontSize = 19.sp), color = c.text)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.XS)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text("${profile.label} tolerance · m = ${profile.multiplier}", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            if (a.isCappedByCritical) StatusLabel("Capped by a critical joint", Tone.NEGATIVE)
        }
    }
}

@Composable
private fun Formula(profile: ToleranceProfile) {
    val c = AppTheme.colors
    val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = c.text)
    // Formulas never wrap mid-term; on narrow screens the block scrolls sideways instead.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.M))
            .background(c.control)
            .horizontalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Δᵢ = max(0, |θᵢ − μᵢ| − m·τᵢ)", style = mono, softWrap = false)
        Text("sᵢ = 100 · max(0, 1 − (Δᵢ/d_maxᵢ)^1.2)", style = mono, softWrap = false)
        Text("S_w = Σ wᵢsᵢ / Σ wᵢ", style = mono, softWrap = false)
        Text("S = min(S_w, 15 + 0.85·min critical sᵢ)", style = mono, softWrap = false)
    }
    Text(
        "m = ${profile.multiplier} (${profile.label} tolerance) · S_w counts visible joints only",
        style = MaterialTheme.typography.bodySmall,
        color = c.textTertiary
    )
}

@Composable
private fun JointRow(e: JointEvaluation, profile: ToleranceProfile) {
    val c = AppTheme.colors
    val tol = e.spec.toleranceDeg * profile.multiplier
    val color = scoreColor(e.jointScore)
    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(Space.S)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.S)) {
            Text(e.spec.name, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
            if (e.spec.isCritical) Tag("Critical")
            Text("${f1(e.jointScore)}%", style = NumericStyle.copy(fontSize = 14.sp), color = c.text)
        }
        Meter((e.jointScore / 100.0).toFloat(), color, height = 3.dp)
        Text(
            "θ ${f1(e.measuredDeg)}° · μ ${e.spec.targetDeg.toInt()}° ± ${f1(tol)}° · Δ ${f1(e.excessDeg)}° · " +
                "d_max ${e.spec.maxDeviationDeg.toInt()}° · w ${f2(e.spec.weight)} · deficit ${f1(e.weightedDeficit)}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            color = c.textTertiary
        )
        e.directionalCue?.let { Text("→ $it", style = MaterialTheme.typography.bodySmall, color = c.text) }
    }
}

@Composable
private fun ClassifierSection(c: ClassificationResult?) {
    if (c == null || c.features == null) return
    val colors = AppTheme.colors
    Section(title = "Why this pose?", spacing = Space.XS) {
        Text("Rules first, then distance to each pose prototype.", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
        c.ruleTrace.forEach { rc ->
            Text(
                "${if (rc.passed) "✓" else "✗"} ${rc.asana.sanskritName}: ${rc.detail}",
                style = MaterialTheme.typography.bodySmall,
                color = if (rc.passed) colors.accent else colors.textSecondary
            )
        }
        if (c.prototypeDistances.isNotEmpty()) {
            Text(
                "Prototype distance: " + c.prototypeDistances.entries.sortedBy { it.value }
                    .joinToString(" · ") { "${it.key.sanskritName} ${f2(it.value)}" },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = Space.XS)
            )
        }
    }
}
