package com.companion.yogawellness.scoring

import com.companion.yogawellness.pose.OneEuroFilter
import kotlin.math.max
import kotlin.math.sqrt

data class SwayResult(
    /** Detrended RMS sway of the hip midpoint, in pixels. */
    val swaySdPx: Double,
    /** Sway divided by the reference torso length (dimensionless). */
    val normalizedSway: Double,
    /** 0–100, higher = steadier. */
    val stability: Double
)

/**
 * Stability score (§3 Stage 3): standard deviation of the *linearly detrended* hip-midpoint
 * trajectory over a sliding window, normalized by the calibrated (or running median) torso length.
 *
 * Detrending removes slow intentional drift (e.g. settling into the pose) so only wobble counts.
 * The hip midpoint is lightly pre-filtered (One-Euro, 2 Hz) to strip landmark jitter, which is far
 * faster than human postural sway (< 1 Hz).
 *
 * Mapping: stability = 100 · clamp(1 − (r − [DEADBAND]) / [RANGE], 0, 1), r = normalized sway.
 * With a ~50 cm torso, r = 0.005 ≈ 2.5 mm (rock steady → 100) and r = 0.045 ≈ 2.2 cm (→ 0).
 * Both constants are meant to be tuned on device with the record/replay harness.
 */
class SwayStabilityTracker(
    private val windowSec: Double = 3.0,
    private val minSamples: Int = 20
) {
    private data class Sample(val t: Double, val x: Double, val y: Double)

    private val samples = ArrayDeque<Sample>()
    private val fx = OneEuroFilter(minCutoff = 2.0, beta = 0.0)
    private val fy = OneEuroFilter(minCutoff = 2.0, beta = 0.0)

    fun reset() {
        samples.clear()
        fx.reset()
        fy.reset()
    }

    fun update(tSec: Double, hipMidX: Double, hipMidY: Double, torsoRefPx: Double): SwayResult? {
        samples.addLast(Sample(tSec, fx.filter(hipMidX, tSec), fy.filter(hipMidY, tSec)))
        while (samples.isNotEmpty() && tSec - samples.first().t > windowSec) samples.removeFirst()
        if (samples.size < minSamples || torsoRefPx <= 1.0) return null

        val sd = sqrt(detrendedVariance(samples.map { it.t }, samples.map { it.x }) +
            detrendedVariance(samples.map { it.t }, samples.map { it.y }))
        val r = sd / torsoRefPx
        return SwayResult(sd, r, stabilityFromSway(r))
    }

    companion object {
        const val DEADBAND = 0.005
        const val RANGE = 0.04

        fun stabilityFromSway(normalizedSway: Double): Double =
            100.0 * (1.0 - max(0.0, normalizedSway - DEADBAND) / RANGE).coerceIn(0.0, 1.0)

        /** Variance of the residuals after a least-squares line fit v ≈ a + b·t. */
        fun detrendedVariance(t: List<Double>, v: List<Double>): Double {
            val n = t.size
            if (n < 3) return 0.0
            val tm = t.average()
            val vm = v.average()
            var stt = 0.0
            var stv = 0.0
            for (i in 0 until n) {
                stt += (t[i] - tm) * (t[i] - tm)
                stv += (t[i] - tm) * (v[i] - vm)
            }
            val b = if (stt > 1e-12) stv / stt else 0.0
            var ss = 0.0
            for (i in 0 until n) {
                val r = v[i] - (vm + b * (t[i] - tm))
                ss += r * r
            }
            return ss / n
        }
    }
}
