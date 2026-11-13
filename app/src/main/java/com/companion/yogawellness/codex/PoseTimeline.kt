package com.companion.yogawellness.codex

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor

enum class Phase { ENTER, HOLD, EXIT }

/** [blend] runs 0 (start pose) → 1 (peak pose). */
data class TimelineState(val phase: Phase, val blend: Float)

/** Inhale / hold / exhale lengths in seconds (data, formatted for display by [format]). */
data class BreathPattern(val inhaleSec: Int, val holdSec: Int, val exhaleSec: Int) {
    val cycleSec: Int get() = inhaleSec + holdSec + exhaleSec
    fun format(): String =
        if (holdSec > 0) "Inhale $inhaleSec s · hold $holdSec s · exhale $exhaleSec s" else "Inhale $inhaleSec s · exhale $exhaleSec s"
}

enum class BreathStep { INHALE, HOLD, EXHALE }

/** Breath position: which step, and a level from -1 (empty) to +1 (full). */
data class BreathState(val step: BreathStep, val level: Float)

object PoseTimeline {
    const val LOOP_SEC = 8f
    const val BREATH_DEG = 1.5f

    /**
     * 8 s loop at speed 1.0: 0–2 s ENTER (cosine ease 0→1), 2–6 s HOLD (1), 6–8 s EXIT (mirror).
     * At speed 0.5 the same shape plays over 16 s.
     */
    fun timelineAt(tSec: Float, speed: Float = 1f): TimelineState {
        val t = ((tSec * speed) % LOOP_SEC + LOOP_SEC) % LOOP_SEC
        return when {
            t < 2f -> TimelineState(Phase.ENTER, ease(t / 2f))
            t < 6f -> TimelineState(Phase.HOLD, 1f)
            else -> TimelineState(Phase.EXIT, ease((LOOP_SEC - t) / 2f))
        }
    }

    private fun ease(p: Float): Float = (0.5 - 0.5 * cos(PI * p)).toFloat()

    /** Where in the breath cycle [tSec] falls. */
    fun breathAt(tSec: Float, pattern: BreathPattern): BreathState = BreathState(breathStep(tSec, pattern), breathLevel(tSec, pattern))

    /** Current breath step (allocation-free, for per-frame use). */
    fun breathStep(tSec: Float, pattern: BreathPattern): BreathStep {
        val t = cycleTime(tSec, pattern)
        return when {
            t < pattern.inhaleSec -> BreathStep.INHALE
            t < pattern.inhaleSec + pattern.holdSec -> BreathStep.HOLD
            else -> BreathStep.EXHALE
        }
    }

    /** Lung level from -1 (empty) to +1 (full); allocation-free, for per-frame use. */
    fun breathLevel(tSec: Float, pattern: BreathPattern): Float {
        val t = cycleTime(tSec, pattern)
        return when {
            t < pattern.inhaleSec -> -cos(PI * t / pattern.inhaleSec).toFloat()
            t < pattern.inhaleSec + pattern.holdSec -> 1f
            else -> {
                val e = (t - pattern.inhaleSec - pattern.holdSec) / pattern.exhaleSec.coerceAtLeast(1)
                cos(PI * e).toFloat()
            }
        }
    }

    private fun cycleTime(tSec: Float, pattern: BreathPattern): Float {
        val cycle = pattern.cycleSec.toFloat().coerceAtLeast(1f)
        return tSec - floor(tSec / cycle) * cycle
    }

    /** Breathing micro-motion in degrees (±[BREATH_DEG]); only during HOLD. */
    fun breathOffset(tSec: Float, pattern: BreathPattern, phase: Phase): Float =
        if (phase != Phase.HOLD) 0f else BREATH_DEG * breathLevel(tSec, pattern)

    /** The angles shown at [tSec]: start→peak blend plus breathing in the hold. */
    fun anglesAt(start: PoseAngles, peak: PoseAngles, tSec: Float, speed: Float, breath: BreathPattern, pausedAtPeak: Boolean): PoseAngles {
        val state = if (pausedAtPeak) TimelineState(Phase.HOLD, 1f) else timelineAt(tSec, speed)
        return anglesFor(start, peak, state, breathOffset(tSec, breath, state.phase))
    }

    /** Blends start→peak by [state] and adds [breathDeg] of breathing to the arms and chest. */
    fun anglesFor(start: PoseAngles, peak: PoseAngles, state: TimelineState, breathDeg: Float): PoseAngles {
        val base = PoseAngles.lerp(start, peak, state.blend)
        if (breathDeg == 0f) return base
        return base.copy(
            lShoulderFlex = base.lShoulderFlex + breathDeg,
            rShoulderFlex = base.rShoulderFlex + breathDeg,
            trunkPitch = base.trunkPitch - breathDeg // inhale lifts the chest slightly
        )
    }
}
