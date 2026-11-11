package com.companion.yogawellness.stats

import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

data class CorrelationResult(
    val rho: Double?,
    val pValue: Double?,
    val n: Int,
    /** False when n < the minimum sample (14 days) — no p-value is reported then. */
    val sufficient: Boolean,
    val shuffles: Int
)

/**
 * Statistical honesty (§9.4):
 *  - tie-corrected Spearman's ρ = Pearson correlation of average-ranked vectors;
 *  - two-sided Monte Carlo permutation p-value with 5,000 shuffles,
 *    p = (1 + #{|ρ_perm| ≥ |ρ_obs|}) / (shuffles + 1);
 *  - plain-language, non-causal insight text with explicit caveats.
 */
object RankStatsEngine {

    const val DEFAULT_SHUFFLES = 5_000
    const val MIN_DAYS = 14
    const val SIGNIFICANCE = 0.05

    const val CAVEAT_CAUSATION =
        "Correlation reflects a pattern in your personal logs and does not prove causation—other factors like sleep and workload also vary."
    const val CAVEAT_AUTOCORRELATION =
        "Note: Daily self-reports are naturally autocorrelated across consecutive days, and a few weeks of logs is a small sample."

    /** Average ranks (1-based); tied values share the mean of the ranks they span. */
    fun averageRanks(values: DoubleArray): DoubleArray {
        val n = values.size
        val order = (0 until n).sortedBy { values[it] }
        val ranks = DoubleArray(n)
        var i = 0
        while (i < n) {
            var j = i
            while (j + 1 < n && values[order[j + 1]] == values[order[i]]) j++
            val avg = (i + j) / 2.0 + 1.0
            for (k in i..j) ranks[order[k]] = avg
            i = j + 1
        }
        return ranks
    }

    fun pearson(x: DoubleArray, y: DoubleArray): Double? {
        require(x.size == y.size) { "x and y must have the same length" }
        val n = x.size
        if (n < 3) return null
        val mx = x.average()
        val my = y.average()
        var sxy = 0.0
        var sxx = 0.0
        var syy = 0.0
        for (i in 0 until n) {
            val dx = x[i] - mx
            val dy = y[i] - my
            sxy += dx * dy
            sxx += dx * dx
            syy += dy * dy
        }
        if (sxx <= 1e-12 || syy <= 1e-12) return null
        return sxy / sqrt(sxx * syy)
    }

    /** Tie-corrected Spearman's ρ, or null if undefined (n < 3 or a constant series). */
    fun spearman(x: DoubleArray, y: DoubleArray): Double? = pearson(averageRanks(x), averageRanks(y))

    /** Two-sided Monte Carlo permutation p-value for Spearman's ρ (deterministic for a given seed). */
    fun permutationPValue(
        x: DoubleArray,
        y: DoubleArray,
        shuffles: Int = DEFAULT_SHUFFLES,
        seed: Long = 20_240_101L
    ): Double? {
        val rx = averageRanks(x)
        val ry = averageRanks(y)
        val observed = pearson(rx, ry) ?: return null
        val rnd = Random(seed)
        val perm = ry.copyOf()
        var extreme = 0
        repeat(shuffles) {
            // Fisher–Yates shuffle of y's ranks breaks any pairing with x
            for (i in perm.size - 1 downTo 1) {
                val j = rnd.nextInt(i + 1)
                val tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp
            }
            val r = pearson(rx, perm) ?: 0.0
            if (abs(r) >= abs(observed) - 1e-12) extreme++
        }
        return (1.0 + extreme) / (shuffles + 1.0)
    }

    fun analyze(
        x: DoubleArray,
        y: DoubleArray,
        minN: Int = MIN_DAYS,
        shuffles: Int = DEFAULT_SHUFFLES,
        seed: Long = 20_240_101L
    ): CorrelationResult {
        val n = x.size
        val rho = spearman(x, y)
        if (n < minN || rho == null) return CorrelationResult(rho, null, n, false, shuffles)
        return CorrelationResult(rho, permutationPValue(x, y, shuffles, seed), n, true, shuffles)
    }

    /**
     * Plain-language insight. Never speculates about motives or causes; always carries both caveats.
     * @param xLabel e.g. "practice minutes"
     * @param yLabel e.g. "self-rated stress"
     */
    fun insight(result: CorrelationResult, xLabel: String, yLabel: String): String {
        val stats = describe(result)
        val headline = when {
            !result.sufficient || result.rho == null ->
                "Not enough days yet to look for a pattern between $xLabel and $yLabel ($stats). Keep logging for at least ${MIN_DAYS} days."
            result.pValue!! < SIGNIFICANCE && result.rho < 0 ->
                "Days with more $xLabel tended to coincide with lower $yLabel in this window ($stats)."
            result.pValue < SIGNIFICANCE && result.rho > 0 ->
                "Days with more $xLabel tended to coincide with higher $yLabel in this window ($stats)."
            result.rho < 0 ->
                "A slight downward tendency between $xLabel and $yLabel was seen, but it is not distinguishable from chance ($stats)."
            else ->
                "No downward trend between $xLabel and $yLabel was observed in this window ($stats)."
        }
        return "$headline $CAVEAT_CAUSATION $CAVEAT_AUTOCORRELATION"
    }

    fun describe(r: CorrelationResult): String {
        val rho = r.rho?.let { String.format(Locale.US, "%+.2f", it) } ?: "n/a"
        val p = r.pValue?.let { if (it < 0.001) "< 0.001" else "= " + String.format(Locale.US, "%.3f", it) }
        return if (p != null) "ρ = $rho, p $p, n = ${r.n} days" else "ρ = $rho, n = ${r.n} days"
    }
}
