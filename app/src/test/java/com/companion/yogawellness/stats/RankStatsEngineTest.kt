package com.companion.yogawellness.stats

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RankStatsEngineTest {

    private fun d(vararg v: Number) = v.map { it.toDouble() }.toDoubleArray()

    @Test
    fun averageRanksWithTies() {
        assertArrayEquals(d(1, 2.5, 2.5, 4), RankStatsEngine.averageRanks(d(10, 20, 20, 30)), 1e-12)
        assertArrayEquals(d(2, 2, 2), RankStatsEngine.averageRanks(d(5, 5, 5)), 1e-12)
    }

    @Test
    fun matchesScipySpearmanReferenceValues() {
        // scipy.stats.spearmanr([1,2,3,4,5],[5,6,7,8,7]).correlation
        assertEquals(0.8207826816681233, RankStatsEngine.spearman(d(1, 2, 3, 4, 5), d(5, 6, 7, 8, 7))!!, 1e-12)
        // heavy ties: scipy.stats.spearmanr([3,1,4,1,5,9,2,6,5,3,5],[2,7,1,8,2,8,1,8,2,8,4])
        assertEquals(
            0.1384567651467695,
            RankStatsEngine.spearman(d(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5), d(2, 7, 1, 8, 2, 8, 1, 8, 2, 8, 4))!!,
            1e-12
        )
    }

    @Test
    fun monotonicIsPlusMinusOne() {
        assertEquals(1.0, RankStatsEngine.spearman(d(1, 2, 3, 4), d(1, 4, 9, 16))!!, 1e-12)
        assertEquals(-1.0, RankStatsEngine.spearman(d(1, 2, 3, 4), d(9, 7, 3, 1))!!, 1e-12)
    }

    @Test
    fun undefinedCases() {
        assertNull(RankStatsEngine.spearman(d(1, 2), d(2, 1)))
        assertNull(RankStatsEngine.spearman(d(1, 2, 3), d(4, 4, 4)))
    }

    @Test
    fun permutationPValueApproximatesExactTest() {
        // Exact two-sided permutation p over all 120 orderings = 16/120 = 0.1333 (computed with scipy)
        val p = RankStatsEngine.permutationPValue(d(1, 2, 3, 4, 5), d(5, 6, 7, 8, 7))!!
        assertEquals(0.1333, p, 0.015)
    }

    @Test
    fun strongAssociationIsSignificantAndNoiseIsNot() {
        val rnd = Random(3)
        val x = DoubleArray(28) { it.toDouble() }
        val y = DoubleArray(28) { -0.3 * it + rnd.nextDouble(-2.0, 2.0) }
        val strong = RankStatsEngine.analyze(x, y)
        assertTrue(strong.rho!! < -0.7)
        assertTrue(strong.pValue!! < 0.001)
        assertEquals(1.0 / 5001.0, strong.pValue!!, 1e-12) // no shuffle beats it

        val noise = RankStatsEngine.analyze(DoubleArray(28) { rnd.nextDouble() }, DoubleArray(28) { rnd.nextDouble() })
        assertTrue("p=${noise.pValue}", noise.pValue!! > 0.05)
    }

    @Test
    fun deterministicForAGivenSeed() {
        val x = d(1, 5, 2, 8, 3, 9, 4, 7, 6, 10, 11, 12, 14, 13)
        val y = d(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8, 9, 7)
        assertEquals(RankStatsEngine.permutationPValue(x, y), RankStatsEngine.permutationPValue(x, y))
    }

    @Test
    fun belowFourteenDaysIsInsufficient() {
        val r = RankStatsEngine.analyze(DoubleArray(10) { it.toDouble() }, DoubleArray(10) { it.toDouble() })
        assertFalse(r.sufficient)
        assertNull(r.pValue)
        assertTrue(RankStatsEngine.insight(r, "practice minutes", "self-rated stress").startsWith("Not enough days yet"))
    }

    @Test
    fun insightTextIsNonCausalAndCarriesCaveats() {
        val positive = CorrelationResult(0.18, 0.34, 28, true, 5000)
        val text = RankStatsEngine.insight(positive, "practice minutes", "self-rated stress")
        assertTrue(text.startsWith("No downward trend between practice minutes and self-rated stress was observed in this window (ρ = +0.18, p = 0.340, n = 28 days)."))
        assertTrue(text.contains(RankStatsEngine.CAVEAT_CAUSATION))
        assertTrue(text.contains(RankStatsEngine.CAVEAT_AUTOCORRELATION))

        val negative = RankStatsEngine.insight(CorrelationResult(-0.52, 0.003, 42, true, 5000), "practice minutes", "self-rated stress")
        assertTrue(negative.startsWith("Days with more practice minutes tended to coincide with lower self-rated stress"))
        for (t in listOf(text, negative)) {
            val lower = t.lowercase()
            listOf("because", "causes", "caused", "leads to", "reduces your", "improves your").forEach {
                assertFalse("'$it' in: $t", lower.contains(it))
            }
        }
    }
}
