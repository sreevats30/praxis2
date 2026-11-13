package com.companion.yogawellness.pose

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.classifier.ClassificationResult
import com.companion.yogawellness.classifier.LockState
import com.companion.yogawellness.classifier.RuleAndCentroidClassifier
import com.companion.yogawellness.classifier.TemporalLock
import com.companion.yogawellness.feedback.SafetyAlert
import com.companion.yogawellness.feedback.SafetyGuards
import com.companion.yogawellness.scoring.AlignmentScoreResult
import com.companion.yogawellness.scoring.AlignmentScorer
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.AsanaSpecs
import com.companion.yogawellness.scoring.HoldStateMachine
import com.companion.yogawellness.scoring.HoldStatus
import com.companion.yogawellness.scoring.SessionTracker
import com.companion.yogawellness.scoring.SwayResult
import com.companion.yogawellness.scoring.SwayStabilityTracker
import com.companion.yogawellness.scoring.SymmetryCalculator
import com.companion.yogawellness.scoring.ToleranceProfile

data class CalibrationState(
    val calibratedTorsoPx: Double?,
    /** 0..1 progress of the 5 s Tadasana calibration (1 = done). */
    val progress: Double
) {
    val isCalibrated: Boolean get() = calibratedTorsoPx != null
}

sealed interface PipelineEvent {
    data class LockAcquired(val label: AsanaLabel) : PipelineEvent
    data class LockReleased(val label: AsanaLabel) : PipelineEvent
    data class Praise(val label: AsanaLabel) : PipelineEvent
    data class HoldCapReached(val label: AsanaLabel, val seconds: Int) : PipelineEvent
    data class CalibrationComplete(val torsoPx: Double) : PipelineEvent
    data object ScoringArmed : PipelineEvent
}

/** Everything the UI, coach and recorder need about one processed frame. */
data class PipelineOutput(
    val frame: PoseFrame,
    val preflight: PreflightResult,
    val scoringArmed: Boolean,
    val classification: ClassificationResult?,
    val lock: LockState?,
    val alignment: AlignmentScoreResult?,
    val smoothedAngles: Map<String, Double>,
    val jointVisibility: Map<String, Boolean>,
    val sway: SwayResult?,
    val hold: HoldStatus,
    val intraSymmetry: Double?,
    val calibration: CalibrationState,
    val events: List<PipelineEvent>,
    /** Physical safety rule currently violated (§4), checked before any alignment cue. */
    val safety: SafetyAlert? = null
) {
    val lockedLabel: AsanaLabel? get() = lock?.locked
    val score: Double? get() = alignment?.finalScore
}

/**
 * 5-second Tadasana body-proportion calibration (§4): median torso length while Tadasana is locked.
 */
class TorsoCalibrator(private val durationSec: Double = 5.0) {
    private val samples = ArrayList<Double>()
    private var startT = Double.NaN
    var calibratedTorsoPx: Double? = null
        private set

    fun reset() {
        samples.clear()
        startT = Double.NaN
        calibratedTorsoPx = null
    }

    /** Returns true on the frame where calibration completes. */
    fun update(tSec: Double, inTadasana: Boolean, torsoPx: Double): Boolean {
        if (calibratedTorsoPx != null) return false
        if (!inTadasana) {
            samples.clear()
            startT = Double.NaN
            return false
        }
        if (startT.isNaN()) startT = tSec
        samples += torsoPx
        if (tSec - startT >= durationSec && samples.size >= 10) {
            calibratedTorsoPx = samples.sorted()[samples.size / 2]
            return true
        }
        return false
    }

    fun state(tSec: Double): CalibrationState = CalibrationState(
        calibratedTorsoPx,
        when {
            calibratedTorsoPx != null -> 1.0
            startT.isNaN() -> 0.0
            else -> ((tSec - startT) / durationSec).coerceIn(0.0, 0.99)
        }
    )
}

/**
 * The full per-frame pipeline (§3 stages 1–3), pure Kotlin so it runs identically on live camera
 * frames, replayed `.jsonl` clips and JVM unit tests.
 */
class PosePipeline(var toleranceProfile: ToleranceProfile = ToleranceProfile.STANDARD) {

    private val classifier = RuleAndCentroidClassifier()
    private val lock = TemporalLock()
    private val scoringFilters = AngleFilterBank()
    private val sway = SwayStabilityTracker()
    private var hold = HoldStateMachine()
    private val calibrator = TorsoCalibrator()
    private val recentTorso = ArrayDeque<Double>()
    val session = SessionTracker()

    private var armed = false
    private var greenRun = 0
    private var phoneBadRun = 0

    fun reset() {
        classifier.reset()
        lock.reset()
        scoringFilters.reset()
        sway.reset()
        hold = HoldStateMachine()
        calibrator.reset()
        recentTorso.clear()
        session.reset()
        armed = false
        greenRun = 0
        phoneBadRun = 0
    }

    fun process(frame: PoseFrame, sensors: SensorSnapshot? = null): PipelineOutput {
        val t = frame.timestampSec
        val events = ArrayList<PipelineEvent>()

        // ---- Pre-flight arming --------------------------------------------------------------
        val preflight = PreflightGuard.evaluate(frame, sensors)
        greenRun = if (preflight.allGreen) greenRun + 1 else 0
        phoneBadRun = if (!preflight.check(PreflightCheckId.PHONE).ok) phoneBadRun + 1 else 0
        if (!armed && greenRun >= ARM_FRAMES) {
            armed = true
            events += PipelineEvent.ScoringArmed
        } else if (armed && phoneBadRun >= DISARM_FRAMES) {
            // The phone was picked up or knocked over: re-run the checklist.
            armed = false
            releaseLock(events)
        }

        if (!armed) {
            return PipelineOutput(
                frame, preflight, false, null, null, null, emptyMap(), emptyMap(), null,
                HoldStatus.IDLE, null, calibrator.state(t), events
            )
        }

        // ---- Stage 2: classify + temporal lock ----------------------------------------------
        val classification = classifier.classify(frame)
        val previous = lock.locked
        val lockState = lock.update(classification.label)
        if (lockState.changed) {
            previous?.let { events += PipelineEvent.LockReleased(it) }
            lockState.locked?.let { events += PipelineEvent.LockAcquired(it) }
            onLockChanged(lockState.locked)
        }
        val label = lockState.locked

        // ---- Torso reference (calibrated, else running median) -------------------------------
        var torsoPx = Double.NaN
        if (frame.hasPose) {
            torsoPx = PoseGeometry(frame, 0.0).torsoLengthPx
            recentTorso.addLast(torsoPx)
            if (recentTorso.size > 90) recentTorso.removeFirst()
            if (calibrator.update(t, label?.asana == Asana.TADASANA, torsoPx)) {
                events += PipelineEvent.CalibrationComplete(calibrator.calibratedTorsoPx!!)
            }
        }
        val torsoRef = calibrator.calibratedTorsoPx ?: recentTorso.sorted().getOrNull(recentTorso.size / 2) ?: torsoPx

        // ---- Stage 3: scoring ----------------------------------------------------------------
        var alignment: AlignmentScoreResult? = null
        var smoothed: Map<String, Double> = emptyMap()
        var visibility: Map<String, Boolean> = emptyMap()
        var swayResult: SwayResult? = null
        var intraSym: Double? = null
        var safety: SafetyAlert? = null

        if (label != null && label.asana.isScored && frame.hasPose) {
            val specs = AsanaSpecs.specsFor(label.asana)
            val extraction = JointAngleExtractor.extract(frame, specs, label.side.frontSide, label.asana.zWeight.toDouble())
            val visibleRaw = extraction.angles.filterKeys { extraction.visibilityOk[it] == true }
            smoothed = scoringFilters.filter(visibleRaw, t)
            visibility = extraction.visibilityOk
            alignment = AlignmentScorer.evaluate(smoothed, visibility, specs, toleranceProfile.multiplier)
            intraSym = SymmetryCalculator.intraPose(label.asana, smoothed, visibility)
            safety = SafetyGuards.evaluate(label, frame, smoothed)

            val g = PoseGeometry(frame, 0.0)
            val hip = g.midHip
            swayResult = sway.update(t, hip.x, hip.y, torsoRef)
        }

        // A violated safety rule pauses the hold timer (and praise), however good the alignment is.
        val holdScore = if (safety != null) 0.0 else alignment?.finalScore
        val holdStatus = if (label != null && label.asana.isScored) hold.update(t, holdScore) else HoldStatus.IDLE
        if (label != null) {
            if (holdStatus.praiseTriggered) events += PipelineEvent.Praise(label)
            if (holdStatus.capReached) events += PipelineEvent.HoldCapReached(label, label.asana.maxHoldSec ?: 0)
        }
        session.update(t, label, alignment?.finalScore, swayResult?.stability, holdStatus, alignment)

        return PipelineOutput(
            frame, preflight, true, classification, lockState, alignment, smoothed, visibility,
            swayResult, holdStatus, intraSym, calibrator.state(t), events, safety
        )
    }

    private fun onLockChanged(newLabel: AsanaLabel?) {
        scoringFilters.reset()
        sway.reset()
        hold = HoldStateMachine(maxHoldSec = newLabel?.asana?.maxHoldSec)
    }

    private fun releaseLock(events: MutableList<PipelineEvent>) {
        lock.locked?.let { events += PipelineEvent.LockReleased(it) }
        lock.reset()
        classifier.reset()
        onLockChanged(null)
    }

    companion object {
        /** Consecutive all-green frames before scoring starts (~0.4 s at 25 FPS). */
        const val ARM_FRAMES = 10
        /** Consecutive "phone moved/tilted" frames before scoring is paused (~1 s). */
        const val DISARM_FRAMES = 25
    }
}
