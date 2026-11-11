package com.companion.yogawellness.classifier

import com.companion.yogawellness.pose.AngleFilterBank
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.JointAngleExtractor
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.PoseGeometry
import com.companion.yogawellness.pose.PoseLandmarkIndex as L
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** An asana together with the detected side variant. */
data class AsanaLabel(val asana: Asana, val side: PoseSide) {
    override fun toString(): String = if (asana.hasSides) "${asana.sanskritName} · ${side.label}" else asana.sanskritName
}

/** Scale-invariant features used by the classifier (§6.1). Angles in degrees, distances in torso lengths. */
data class ClassifierFeatures(
    val torsoPx: Double,
    val kneeLeft: Double,
    val kneeRight: Double,
    val shoulderLeft: Double,
    val shoulderRight: Double,
    val trunkTilt: Double,
    val stanceWidth: Double,
    val ankleVerticalSep: Double,
    /** Raw pixel positions needed by the side detectors. */
    val ankleLeftY: Double,
    val ankleRightY: Double,
    val ankleLeftX: Double,
    val ankleRightX: Double,
    val kneeLeftX: Double,
    val kneeRightX: Double,
    val wristLeftY: Double,
    val wristRightY: Double
)

data class RuleCheck(val asana: Asana, val passed: Boolean, val detail: String)

enum class ClassificationMethod { RULE, PROTOTYPE, NONE }

data class ClassificationResult(
    val label: AsanaLabel?,
    val method: ClassificationMethod,
    val features: ClassifierFeatures?,
    /** Normalized RMS distance to each asana prototype (lower = closer). */
    val prototypeDistances: Map<Asana, Double>,
    val ruleTrace: List<RuleCheck>
)

/**
 * Stage 2 (§6): deterministic geometric rules first, prototype-centroid distance second.
 *
 * Rules are evaluated in the §6.1 priority order; the first match wins. If no rule matches, the
 * nearest prototype is accepted only when it is very close ([PROTOTYPE_FALLBACK_MAX_DISTANCE]),
 * so the prototype never overrides an explicit rule. Distances are always reported so the
 * "∑ Explain" sheet can show them.
 */
class RuleAndCentroidClassifier {

    private val angleFilters = AngleFilterBank()

    fun reset() = angleFilters.reset()

    fun classify(frame: PoseFrame): ClassificationResult {
        val features = computeFeatures(frame) ?: return ClassificationResult(null, ClassificationMethod.NONE, null, emptyMap(), emptyList())
        val trace = ArrayList<RuleCheck>(5)
        val ruleLabel = applyRules(features, trace)
        val distances = prototypeDistances(features)

        if (ruleLabel != null) {
            return ClassificationResult(ruleLabel, ClassificationMethod.RULE, features, distances.mapValues { it.value.first }, trace)
        }
        val nearest = distances.minByOrNull { it.value.first }
        val fallback = nearest?.takeIf { it.value.first <= PROTOTYPE_FALLBACK_MAX_DISTANCE }
            ?.let { AsanaLabel(it.key, it.value.second) }
        return ClassificationResult(
            fallback,
            if (fallback != null) ClassificationMethod.PROTOTYPE else ClassificationMethod.NONE,
            features,
            distances.mapValues { it.value.first },
            trace
        )
    }

    fun computeFeatures(frame: PoseFrame): ClassifierFeatures? {
        if (!frame.hasPose) return null
        val g = PoseGeometry(frame, CLASSIFIER_Z_WEIGHT)
        val torso = g.torsoLengthPx
        if (torso < 1.0) return null

        fun ang(a: Int, b: Int, c: Int) = JointAngleExtractor.angle3(g.point(a), g.point(b), g.point(c))
        val raw = mapOf(
            "kneeL" to ang(L.LEFT_HIP, L.LEFT_KNEE, L.LEFT_ANKLE),
            "kneeR" to ang(L.RIGHT_HIP, L.RIGHT_KNEE, L.RIGHT_ANKLE),
            "shL" to ang(L.LEFT_HIP, L.LEFT_SHOULDER, L.LEFT_ELBOW),
            "shR" to ang(L.RIGHT_HIP, L.RIGHT_SHOULDER, L.RIGHT_ELBOW),
            "trunk" to JointAngleExtractor.segmentVsVertical(g.midHip, g.midShoulder)
        )
        if (raw.values.any { it.isNaN() }) return null
        val s = angleFilters.filter(raw, frame.timestampSec)

        val aL = g.point(L.LEFT_ANKLE)
        val aR = g.point(L.RIGHT_ANKLE)
        return ClassifierFeatures(
            torsoPx = torso,
            kneeLeft = s.getValue("kneeL"),
            kneeRight = s.getValue("kneeR"),
            shoulderLeft = s.getValue("shL"),
            shoulderRight = s.getValue("shR"),
            trunkTilt = s.getValue("trunk"),
            stanceWidth = abs(aL.x - aR.x) / torso,
            ankleVerticalSep = abs(aL.y - aR.y) / torso,
            ankleLeftY = aL.y,
            ankleRightY = aR.y,
            ankleLeftX = aL.x,
            ankleRightX = aR.x,
            kneeLeftX = g.point(L.LEFT_KNEE).x,
            kneeRightX = g.point(L.RIGHT_KNEE).x,
            wristLeftY = g.point(L.LEFT_WRIST).y,
            wristRightY = g.point(L.RIGHT_WRIST).y
        )
    }

    // ------------------------------------------------------------------------------------------
    // §6.1 Deterministic decision table
    // ------------------------------------------------------------------------------------------

    fun applyRules(f: ClassifierFeatures, trace: MutableList<RuleCheck>? = null): AsanaLabel? {
        fun record(asana: Asana, passed: Boolean, detail: String): Boolean {
            trace?.add(RuleCheck(asana, passed, detail))
            return passed
        }

        // 1. Vrikshasana: one foot lifted, near the standing knee, standing knee straight
        run {
            val leftLifted = f.ankleLeftY < f.ankleRightY
            val liftedAnkleX = if (leftLifted) f.ankleLeftX else f.ankleRightX
            val standingKneeX = if (leftLifted) f.kneeRightX else f.kneeLeftX
            val standingKnee = if (leftLifted) f.kneeRight else f.kneeLeft
            val near = abs(liftedAnkleX - standingKneeX) / f.torsoPx
            val ok = f.ankleVerticalSep > 0.22 && near < 0.35 && standingKnee > 155.0
            val detail = "ankle Δy=${f.ankleVerticalSep.fmt()}T (>0.22), lifted ankle↔standing knee Δx=${near.fmt()}T (<0.35), standing knee=${standingKnee.deg()} (>155°)"
            if (record(Asana.VRIKSHASANA, ok, detail)) {
                return AsanaLabel(Asana.VRIKSHASANA, if (leftLifted) PoseSide.LEFT_LIFTED else PoseSide.RIGHT_LIFTED)
            }
        }

        // 2. Utkatasana: both feet down, narrow stance, both knees flexed
        run {
            val ok = f.ankleVerticalSep < 0.15 && f.stanceWidth < 0.85 &&
                f.kneeLeft in 75.0..148.0 && f.kneeRight in 75.0..148.0
            val detail = "ankle Δy=${f.ankleVerticalSep.fmt()}T (<0.15), stance=${f.stanceWidth.fmt()}T (<0.85), knees L/R=${f.kneeLeft.deg()}/${f.kneeRight.deg()} (75–148°)"
            if (record(Asana.UTKATASANA, ok, detail)) return AsanaLabel(Asana.UTKATASANA, PoseSide.SYMMETRIC)
        }

        // 3. Virabhadrasana II: wide stance, upright trunk, one knee bent, one straight, arms abducted
        run {
            val front = min(f.kneeLeft, f.kneeRight)
            val back = max(f.kneeLeft, f.kneeRight)
            val ok = f.stanceWidth > 1.10 && f.trunkTilt < 25.0 && front in 70.0..142.0 && back > 152.0 &&
                f.shoulderLeft in 60.0..120.0 && f.shoulderRight in 60.0..120.0
            val detail = "stance=${f.stanceWidth.fmt()}T (>1.10), trunk=${f.trunkTilt.deg()} (<25°), front knee=${front.deg()} (70–142°), back knee=${back.deg()} (>152°), shoulders=${f.shoulderLeft.deg()}/${f.shoulderRight.deg()} (60–120°)"
            if (record(Asana.VIRABHADRASANA_II, ok, detail)) {
                return AsanaLabel(
                    Asana.VIRABHADRASANA_II,
                    if (f.kneeLeft <= f.kneeRight) PoseSide.FRONT_LEFT else PoseSide.FRONT_RIGHT
                )
            }
        }

        // 4. Trikonasana: wide stance, both knees straight, lateral trunk tilt
        run {
            val ok = f.stanceWidth > 1.00 && f.kneeLeft > 152.0 && f.kneeRight > 152.0 && f.trunkTilt in 35.0..95.0
            val detail = "stance=${f.stanceWidth.fmt()}T (>1.00), knees=${f.kneeLeft.deg()}/${f.kneeRight.deg()} (>152°), trunk tilt=${f.trunkTilt.deg()} (35–95°)"
            if (record(Asana.TRIKONASANA, ok, detail)) {
                // Lower wrist (larger y) is the leading side
                return AsanaLabel(
                    Asana.TRIKONASANA,
                    if (f.wristLeftY >= f.wristRightY) PoseSide.LEADING_LEFT else PoseSide.LEADING_RIGHT
                )
            }
        }

        // 5. Tadasana: upright, straight knees, narrow stance, arms by sides
        run {
            val ok = f.trunkTilt < 15.0 && f.kneeLeft > 162.0 && f.kneeRight > 162.0 && f.stanceWidth < 0.75 &&
                f.shoulderLeft < 35.0 && f.shoulderRight < 35.0
            val detail = "trunk=${f.trunkTilt.deg()} (<15°), knees=${f.kneeLeft.deg()}/${f.kneeRight.deg()} (>162°), stance=${f.stanceWidth.fmt()}T (<0.75), shoulders=${f.shoulderLeft.deg()}/${f.shoulderRight.deg()} (<35°)"
            if (record(Asana.TADASANA, ok, detail)) return AsanaLabel(Asana.TADASANA, PoseSide.NEUTRAL_CALIBRATION)
        }
        return null
    }

    // ------------------------------------------------------------------------------------------
    // Prototype centroids: typical feature values (mean, spread) for each asana.
    // Distance = RMS of (feature − mean) / spread; side-specific asanas try both sides.
    // ------------------------------------------------------------------------------------------

    private data class Proto(val mean: Double, val spread: Double)

    /** Returns asana -> (distance, best side). */
    fun prototypeDistances(f: ClassifierFeatures): Map<Asana, Pair<Double, PoseSide>> {
        fun rms(terms: List<Pair<Double, Proto>>): Double =
            sqrt(terms.sumOf { (v, p) -> ((v - p.mean) / p.spread).let { it * it } } / terms.size)

        val result = LinkedHashMap<Asana, Pair<Double, PoseSide>>()
        val shoulderAvg = (f.shoulderLeft + f.shoulderRight) / 2
        val kneeAvg = (f.kneeLeft + f.kneeRight) / 2

        // Tree: try each foot as the lifted one
        result[Asana.VRIKSHASANA] = listOf(BodySide.LEFT, BodySide.RIGHT).map { lifted ->
            val standing = if (lifted == BodySide.LEFT) f.kneeRight else f.kneeLeft
            val liftedKnee = if (lifted == BodySide.LEFT) f.kneeLeft else f.kneeRight
            val d = rms(
                listOf(
                    standing to Proto(178.0, 12.0),
                    liftedKnee to Proto(70.0, 30.0),
                    f.trunkTilt to Proto(0.0, 10.0),
                    f.ankleVerticalSep to Proto(0.8, 0.4),
                    f.stanceWidth to Proto(0.2, 0.3)
                )
            )
            d to (if (lifted == BodySide.LEFT) PoseSide.LEFT_LIFTED else PoseSide.RIGHT_LIFTED)
        }.minBy { it.first }

        result[Asana.UTKATASANA] = rms(
            listOf(
                kneeAvg to Proto(112.0, 20.0),
                f.trunkTilt to Proto(28.0, 15.0),
                f.stanceWidth to Proto(0.3, 0.4),
                f.ankleVerticalSep to Proto(0.0, 0.15)
            )
        ) to PoseSide.SYMMETRIC

        result[Asana.VIRABHADRASANA_II] = listOf(BodySide.LEFT, BodySide.RIGHT).map { frontSide ->
            val frontKnee = if (frontSide == BodySide.LEFT) f.kneeLeft else f.kneeRight
            val backKnee = if (frontSide == BodySide.LEFT) f.kneeRight else f.kneeLeft
            val d = rms(
                listOf(
                    frontKnee to Proto(95.0, 20.0),
                    backKnee to Proto(175.0, 12.0),
                    shoulderAvg to Proto(90.0, 20.0),
                    f.trunkTilt to Proto(0.0, 12.0),
                    f.stanceWidth to Proto(1.8, 0.6),
                    f.ankleVerticalSep to Proto(0.0, 0.2)
                )
            )
            d to (if (frontSide == BodySide.LEFT) PoseSide.FRONT_LEFT else PoseSide.FRONT_RIGHT)
        }.minBy { it.first }

        result[Asana.TRIKONASANA] = rms(
            listOf(
                f.kneeLeft to Proto(175.0, 12.0),
                f.kneeRight to Proto(176.0, 12.0),
                f.trunkTilt to Proto(68.0, 20.0),
                f.stanceWidth to Proto(1.6, 0.6),
                f.ankleVerticalSep to Proto(0.0, 0.2)
            )
        ) to (if (f.wristLeftY >= f.wristRightY) PoseSide.LEADING_LEFT else PoseSide.LEADING_RIGHT)

        result[Asana.TADASANA] = rms(
            listOf(
                kneeAvg to Proto(178.0, 10.0),
                f.trunkTilt to Proto(0.0, 8.0),
                f.stanceWidth to Proto(0.3, 0.3),
                shoulderAvg to Proto(10.0, 20.0),
                f.ankleVerticalSep to Proto(0.0, 0.1)
            )
        ) to PoseSide.NEUTRAL_CALIBRATION

        return result
    }

    companion object {
        /** Classifier angles use front-on depth damping regardless of the asana. */
        const val CLASSIFIER_Z_WEIGHT = 0.25
        const val PROTOTYPE_FALLBACK_MAX_DISTANCE = 0.6

        private fun Double.fmt() = String.format(java.util.Locale.US, "%.2f", this)
        private fun Double.deg() = String.format(java.util.Locale.US, "%.0f°", this)
    }
}

data class LockState(
    val locked: AsanaLabel?,
    /** Fraction (0..1) of the lock requirement met by the most frequent recent label. */
    val candidate: AsanaLabel?,
    val candidateProgress: Double,
    val changed: Boolean
)

/**
 * §6.2 Temporal hysteresis lock: lock when the same label appears in >= 14 of the last 18 frames;
 * once locked, stay locked until the pose is rejected for 25 consecutive frames.
 */
class TemporalLock(
    private val window: Int = 18,
    private val lockCount: Int = 14,
    private val unlockFrames: Int = 25
) {
    private val history = ArrayDeque<AsanaLabel?>()
    private var rejectRun = 0
    var locked: AsanaLabel? = null
        private set

    fun reset() {
        history.clear()
        rejectRun = 0
        locked = null
    }

    fun update(label: AsanaLabel?): LockState {
        history.addLast(label)
        if (history.size > window) history.removeFirst()
        var changed = false

        locked?.let { current ->
            rejectRun = if (label == current) 0 else rejectRun + 1
            if (rejectRun >= unlockFrames) {
                locked = null
                rejectRun = 0
                changed = true
            }
        }

        val best = history.filterNotNull().groupingBy { it }.eachCount().maxByOrNull { it.value }
        if (locked == null && best != null && best.value >= lockCount) {
            locked = best.key
            rejectRun = 0
            changed = true
        }
        return LockState(
            locked = locked,
            candidate = best?.key,
            candidateProgress = ((best?.value ?: 0).toDouble() / lockCount).coerceAtMost(1.0),
            changed = changed
        )
    }
}
