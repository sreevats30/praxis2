package com.companion.yogawellness.scoring

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class JointEvaluation(
    val spec: JointSpec,
    val measuredDeg: Double,
    val excessDeg: Double,
    val jointScore: Double,
    val weightedDeficit: Double,
    val directionalCue: String?
)

data class AlignmentScoreResult(
    val finalScore: Double?,          // null if < 60% of body weight is visible
    val weightedScore: Double,
    val criticalCapValue: Double,
    val isCappedByCritical: Boolean,
    val evaluations: List<JointEvaluation>,
    val primaryCorrectiveCue: String?
)

/**
 * Verified recalibrated alignment scorer (§7.1 formula, §8.2 code).
 *
 *  Δ_i       = max(0, |θ_i − μ_i| − m_τ·τ_i)
 *  s_i       = 100 · max(0, 1 − (Δ_i / d_max,i)^1.2)
 *  S_weighted = Σ_{i∈U} w_i s_i / Σ_{i∈U} w_i          (U = visible joints)
 *  S_align   = min(S_weighted, 15 + 0.85 · min_{i∈U_crit} s_i)
 */
object AlignmentScorer {

    const val PENALTY_EXPONENT = 1.2
    const val CAP_OFFSET = 15.0
    const val CAP_SLOPE = 0.85
    const val MIN_VISIBLE_WEIGHT_FRACTION = 0.60
    const val CUE_JOINT_SCORE_THRESHOLD = 82.0
    const val NOT_VISIBLE_CUE = "Step back so your full body is visible"

    fun evaluate(
        smoothedAngles: Map<String, Double>,
        jointVisibilityOk: Map<String, Boolean>,
        specs: List<JointSpec>,
        toleranceMultiplier: Double = 1.0
    ): AlignmentScoreResult {
        val evals = mutableListOf<JointEvaluation>()
        var weightedSum = 0.0
        var activeWeightSum = 0.0
        var minCriticalScore = 100.0
        val totalSpecWeight = specs.sumOf { it.weight }

        for (spec in specs) {
            val theta = smoothedAngles[spec.name] ?: Double.NaN
            if (theta.isNaN() || jointVisibilityOk[spec.name] != true) continue

            val signedDiff = theta - spec.targetDeg
            val effectiveTol = spec.toleranceDeg * toleranceMultiplier
            val excess = max(0.0, abs(signedDiff) - effectiveTol)
            val normalizedPenalty = (excess / spec.maxDeviationDeg).coerceIn(0.0, 1.0).pow(PENALTY_EXPONENT)
            val score = 100.0 * (1.0 - normalizedPenalty)

            val cue = if (excess > 0.0) {
                if (signedDiff > 0.0) spec.cueWhenTooLarge else spec.cueWhenTooSmall
            } else null

            val deficit = spec.weight * (100.0 - score)
            evals += JointEvaluation(spec, theta, excess, score, deficit, cue)

            weightedSum += spec.weight * score
            activeWeightSum += spec.weight
            if (spec.isCritical) minCriticalScore = min(minCriticalScore, score)
        }

        if (totalSpecWeight <= 0.0 || activeWeightSum / totalSpecWeight < MIN_VISIBLE_WEIGHT_FRACTION) {
            return AlignmentScoreResult(null, 0.0, 100.0, false, evals, NOT_VISIBLE_CUE)
        }

        val rawWeighted = weightedSum / activeWeightSum
        // Tightened Critical-Joint Cap: 125-deg knee in Warrior II (s_knee=31.1) caps total at 41.4%
        val criticalCap = CAP_OFFSET + CAP_SLOPE * minCriticalScore
        val finalScore = min(rawWeighted, criticalCap).coerceIn(0.0, 100.0)

        val worstJoint = evals.maxByOrNull { it.weightedDeficit }
        val topCue = worstJoint?.takeIf { it.jointScore < CUE_JOINT_SCORE_THRESHOLD }?.directionalCue

        return AlignmentScoreResult(
            finalScore = finalScore,
            weightedScore = rawWeighted,
            criticalCapValue = criticalCap,
            isCappedByCritical = criticalCap < rawWeighted - 0.5,
            evaluations = evals.sortedByDescending { it.weightedDeficit },
            primaryCorrectiveCue = topCue
        )
    }
}
