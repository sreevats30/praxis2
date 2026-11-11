package com.companion.yogawellness.pose

import kotlin.math.PI
import kotlin.math.abs

/**
 * One-Euro filter (Casiez, Roussel & Vogel, CHI 2012).
 *
 * Adaptive low-pass: at low speed the cutoff stays near [minCutoff] (kills jitter), and at high
 * speed it rises by [beta] * |speed| (kills lag). Used on the 12 core joint angles (§2).
 *
 * @param minCutoff minimum cutoff frequency in Hz
 * @param beta speed coefficient (Hz per unit/second)
 * @param dCutoff cutoff frequency for the derivative estimate in Hz
 */
class OneEuroFilter(
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.0,
    private val dCutoff: Double = 1.0
) {
    private var xPrev = Double.NaN
    private var dxPrev = 0.0
    private var tPrev = Double.NaN

    init {
        require(minCutoff > 0.0) { "minCutoff must be > 0" }
        require(dCutoff > 0.0) { "dCutoff must be > 0" }
        require(beta >= 0.0) { "beta must be >= 0" }
    }

    /**
     * @param x new raw sample; NaN samples are ignored and returned as-is (state is kept)
     * @param timestampSec sample time in seconds (monotonic)
     */
    fun filter(x: Double, timestampSec: Double): Double {
        if (x.isNaN()) return x
        if (xPrev.isNaN() || tPrev.isNaN()) {
            xPrev = x
            dxPrev = 0.0
            tPrev = timestampSec
            return x
        }
        val dt = timestampSec - tPrev
        if (dt <= 0.0) return xPrev // duplicate / out-of-order frame

        val dx = (x - xPrev) / dt
        val aD = smoothingFactor(dt, dCutoff)
        val dxHat = aD * dx + (1.0 - aD) * dxPrev

        val cutoff = minCutoff + beta * abs(dxHat)
        val a = smoothingFactor(dt, cutoff)
        val xHat = a * x + (1.0 - a) * xPrev

        xPrev = xHat
        dxPrev = dxHat
        tPrev = timestampSec
        return xHat
    }

    fun reset() {
        xPrev = Double.NaN
        dxPrev = 0.0
        tPrev = Double.NaN
    }

    private fun smoothingFactor(dt: Double, cutoff: Double): Double {
        val tau = 1.0 / (2.0 * PI * cutoff)
        return 1.0 / (1.0 + tau / dt)
    }

    companion object {
        /** Blueprint §2 tuning for joint angles measured in degrees. */
        const val DEGREES_MIN_CUTOFF = 1.2
        const val DEGREES_BETA = 0.04

        fun forDegrees(
            minCutoff: Double = DEGREES_MIN_CUTOFF,
            beta: Double = DEGREES_BETA,
            dCutoff: Double = 1.0
        ): OneEuroFilter = OneEuroFilter(minCutoff, beta, dCutoff)
    }
}

/** One independent [OneEuroFilter] per named joint angle. */
class AngleFilterBank(private val factory: () -> OneEuroFilter = { OneEuroFilter.forDegrees() }) {
    private val filters = HashMap<String, OneEuroFilter>()

    fun filter(raw: Map<String, Double>, timestampSec: Double): Map<String, Double> =
        raw.mapValues { (name, value) -> filters.getOrPut(name, factory).filter(value, timestampSec) }

    fun reset() = filters.clear()
}
