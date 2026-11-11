package com.companion.yogawellness.scoring

import kotlin.math.max

enum class HoldPhase { WAITING, ALIGNING, HOLDING, GRACE, COMPLETE }

data class HoldStatus(
    val phase: HoldPhase,
    /** Continuous time at >= threshold; any dip resets it. */
    val strictHoldSec: Double,
    /** Time at >= threshold where dips shorter than the grace period do not reset. */
    val graceHoldSec: Double,
    val bestStrictSec: Double,
    val bestGraceSec: Double,
    /** True on the single frame where >= 88% has been held for 4 s. */
    val praiseTriggered: Boolean,
    /** True on the single frame where the endurance cap (e.g. Utkatasana 30 s) is reached. */
    val capReached: Boolean
) {
    companion object {
        val IDLE = HoldStatus(HoldPhase.WAITING, 0.0, 0.0, 0.0, 0.0, false, false)
    }
}

/**
 * Hold timer (§3 Stage 3). Reports BOTH a strict continuous hold and a grace-adjusted hold:
 * a brief wobble below [holdThreshold] shorter than [graceSec] pauses the grace timer instead of
 * resetting it. Also emits the praise trigger (>= 88% for 4 s) and the endurance cap.
 */
class HoldStateMachine(
    private val holdThreshold: Double = AsanaSpecs.HOLD_THRESHOLD,
    private val graceSec: Double = 1.5,
    private val praiseThreshold: Double = AsanaSpecs.PRAISE_THRESHOLD,
    private val praiseHoldSec: Double = AsanaSpecs.PRAISE_HOLD_SEC,
    private val maxHoldSec: Int? = null
) {
    private var tPrev = Double.NaN
    private var prevGood = false
    private var strict = 0.0
    private var grace = 0.0
    private var bestStrict = 0.0
    private var bestGrace = 0.0
    private var belowSince = Double.NaN
    private var praiseSince = Double.NaN
    private var praisedThisRun = false
    private var complete = false

    fun reset() {
        tPrev = Double.NaN
        prevGood = false
        strict = 0.0
        grace = 0.0
        bestStrict = 0.0
        bestGrace = 0.0
        belowSince = Double.NaN
        praiseSince = Double.NaN
        praisedThisRun = false
        complete = false
    }

    fun update(tSec: Double, score: Double?): HoldStatus {
        val dt = if (tPrev.isNaN()) 0.0 else (tSec - tPrev).coerceIn(0.0, 0.5)
        tPrev = tSec

        if (complete) {
            return HoldStatus(HoldPhase.COMPLETE, strict, grace, bestStrict, bestGrace, false, false)
        }

        val good = score != null && score >= holdThreshold
        var phase: HoldPhase
        if (good) {
            if (prevGood) {
                strict += dt
                grace += dt
            }
            belowSince = Double.NaN
            phase = HoldPhase.HOLDING
        } else {
            strict = 0.0
            if (grace > 0.0) {
                if (belowSince.isNaN()) belowSince = tSec
                if (tSec - belowSince > graceSec) {
                    grace = 0.0
                    phase = HoldPhase.ALIGNING
                } else {
                    phase = HoldPhase.GRACE
                }
            } else {
                phase = if (score == null) HoldPhase.WAITING else HoldPhase.ALIGNING
            }
        }
        prevGood = good
        bestStrict = max(bestStrict, strict)
        bestGrace = max(bestGrace, grace)

        // Praise: >= 88% continuously for 4 s, once per run above the praise threshold
        var praise = false
        if (score != null && score >= praiseThreshold) {
            if (praiseSince.isNaN()) praiseSince = tSec
            if (!praisedThisRun && tSec - praiseSince >= praiseHoldSec) {
                praise = true
                praisedThisRun = true
            }
        } else {
            praiseSince = Double.NaN
            praisedThisRun = false
        }

        // Endurance cap
        var cap = false
        if (maxHoldSec != null && grace >= maxHoldSec) {
            complete = true
            cap = true
            phase = HoldPhase.COMPLETE
        }
        return HoldStatus(phase, strict, grace, bestStrict, bestGrace, praise, cap)
    }
}
