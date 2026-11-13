package com.companion.yogawellness.scoring

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.pose.BodySide
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Intra-pose symmetry (§3 Stage 3): compare matching left/right joints within one frame. */
object SymmetryCalculator {

    /** Joint pairs compared for intra-pose symmetry, per asana. */
    val INTRA_PAIRS: Map<Asana, Pair<String, String>> = mapOf(
        Asana.UTKATASANA to ("Left Knee Flexion" to "Right Knee Flexion"),
        Asana.VIRABHADRASANA_II to ("Front Arm Level" to "Back Arm Level")
    )

    /** A 30° left/right difference maps to 0% symmetry. */
    const val FULL_ASYMMETRY_DEG = 30.0

    fun intraPose(asana: Asana, angles: Map<String, Double>, visible: Map<String, Boolean>): Double? {
        val (a, b) = INTRA_PAIRS[asana] ?: return null
        if (visible[a] != true || visible[b] != true) return null
        val va = angles[a] ?: return null
        val vb = angles[b] ?: return null
        return 100.0 * max(0.0, 1.0 - abs(va - vb) / FULL_ASYMMETRY_DEG)
    }

    /** Inter-pose symmetry: shorter/longer best hold of the two sides, as a percentage. */
    fun holdSymmetry(leftSec: Double, rightSec: Double): Double? {
        val hi = max(leftSec, rightSec)
        if (hi <= 0.0) return null
        return 100.0 * min(leftSec, rightSec) / hi
    }
}

/** Aggregated stats for one (asana, side) during a practice session. */
data class PoseSegmentStats(
    val label: AsanaLabel,
    val lockedSec: Double,
    val scoredFrames: Int,
    val meanScore: Double?,
    val bestStrictHoldSec: Double,
    val bestGraceHoldSec: Double,
    val meanStability: Double?,
    /** Seconds with alignment >= [AsanaSpecs.HOLD_THRESHOLD]. */
    val qualitySec: Double = 0.0,
    val peakScore: Double? = null,
    /** Joint (spec name) that was the lowest-scoring one most often, ignoring frames where every joint was perfect. */
    val worstJoint: String? = null,
    /** Most frequent primary corrective cue over the segment. */
    val topCue: String? = null
)

data class SideComparison(
    val asana: Asana,
    val left: PoseSegmentStats,
    val right: PoseSegmentStats,
    val holdSymmetryPct: Double?,
    val scoreDifference: Double?
)

/**
 * Collects per-(asana, side) statistics over a session for the summary and inter-pose symmetry.
 * Thread-safe: the pipeline thread updates it while the UI may snapshot it to save the session.
 */
class SessionTracker {
    private class Acc {
        var lockedSec = 0.0
        var scoredFrames = 0
        var scoreSum = 0.0
        var stabilityFrames = 0
        var stabilitySum = 0.0
        var bestStrict = 0.0
        var bestGrace = 0.0
        var qualitySec = 0.0
        var peak: Double? = null
        val worstJointCounts = HashMap<String, Int>()
        val cueCounts = HashMap<String, Int>()
    }

    private fun HashMap<String, Int>.argmax(): String? = maxByOrNull { it.value }?.key

    private val acc = LinkedHashMap<AsanaLabel, Acc>()
    private var tPrev = Double.NaN

    @Synchronized
    fun reset() {
        acc.clear()
        tPrev = Double.NaN
    }

    /** @param alignment optional full alignment result, used only for worst-joint / cue statistics. */
    @Synchronized
    fun update(
        tSec: Double,
        label: AsanaLabel?,
        score: Double?,
        stability: Double?,
        hold: HoldStatus,
        alignment: AlignmentScoreResult? = null
    ) {
        val dt = if (tPrev.isNaN()) 0.0 else (tSec - tPrev).coerceIn(0.0, 0.5)
        tPrev = tSec
        if (label == null || !label.asana.isScored) return
        val a = acc.getOrPut(label) { Acc() }
        a.lockedSec += dt
        if (score != null) {
            a.scoredFrames++
            a.scoreSum += score
            if (score >= AsanaSpecs.HOLD_THRESHOLD) a.qualitySec += dt
            a.peak = max(a.peak ?: score, score)
        }
        if (alignment != null) {
            val worst = alignment.evaluations.minByOrNull { it.jointScore }
            if (worst != null && worst.jointScore < 100.0) {
                a.worstJointCounts.merge(worst.spec.name, 1, Int::plus)
            }
            alignment.primaryCorrectiveCue?.let { a.cueCounts.merge(it, 1, Int::plus) }
        }
        if (stability != null) {
            a.stabilityFrames++
            a.stabilitySum += stability
        }
        a.bestStrict = max(a.bestStrict, hold.bestStrictSec)
        a.bestGrace = max(a.bestGrace, hold.bestGraceSec)
    }

    @Synchronized
    fun segments(): List<PoseSegmentStats> = acc.map { (label, a) ->
        PoseSegmentStats(
            label = label,
            lockedSec = a.lockedSec,
            scoredFrames = a.scoredFrames,
            meanScore = if (a.scoredFrames > 0) a.scoreSum / a.scoredFrames else null,
            bestStrictHoldSec = a.bestStrict,
            bestGraceHoldSec = a.bestGrace,
            meanStability = if (a.stabilityFrames > 0) a.stabilitySum / a.stabilityFrames else null,
            qualitySec = a.qualitySec,
            peakScore = a.peak,
            worstJoint = a.worstJointCounts.argmax(),
            topCue = a.cueCounts.argmax()
        )
    }

    /** Left-vs-right comparison for every sided asana practised on both sides. */
    fun sideComparisons(): List<SideComparison> = segments()
        .filter { it.label.asana.hasSides && it.label.side.workingSide != null }
        .groupBy { it.label.asana }
        .mapNotNull { (asana, segs) ->
            val left = segs.filter { it.label.side.workingSide == BodySide.LEFT }.maxByOrNull { it.lockedSec }
            val right = segs.filter { it.label.side.workingSide == BodySide.RIGHT }.maxByOrNull { it.lockedSec }
            if (left == null || right == null) return@mapNotNull null
            SideComparison(
                asana, left, right,
                SymmetryCalculator.holdSymmetry(left.bestGraceHoldSec, right.bestGraceHoldSec),
                if (left.meanScore != null && right.meanScore != null) left.meanScore - right.meanScore else null
            )
        }
}
