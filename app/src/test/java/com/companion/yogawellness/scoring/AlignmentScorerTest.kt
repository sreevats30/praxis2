package com.companion.yogawellness.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the §7.1 "Proof Table" for Virabhadrasana II: every joint perfect except the Front Knee
 * (μ = 95°, τ = ±8°, d_max = 30°, w = 0.30, critical).
 */
class AlignmentScorerTest {

    private val specs = AsanaSpecs.VIRABHADRASANA_II
    private val allVisible = specs.associate { it.name to true }

    private fun perfectAnglesWithFrontKnee(kneeDeg: Double): Map<String, Double> =
        specs.associate { it.name to it.targetDeg } + ("Front Knee" to kneeDeg)

    private data class Row(
        val knee: Double,
        val excess: Double,
        val kneeScore: Double,
        val weighted: Double,
        val cap: Double,
        val final: Double,
        val holding: Boolean
    )

    // Values copied from the §7.1 proof table (rounded to 0.1 in the blueprint).
    private val proofTable = listOf(
        Row(130.0, 27.0, 11.9, 73.6, 25.1, 25.1, holding = false),
        Row(125.0, 22.0, 31.1, 79.3, 41.4, 41.4, holding = false),
        Row(115.0, 12.0, 66.7, 90.0, 71.7, 71.7, holding = false),
        Row(108.0, 5.0, 88.4, 96.5, 90.1, 90.1, holding = true),
        Row(96.0, 0.0, 100.0, 100.0, 100.0, 100.0, holding = true)
    )

    @Test
    fun warriorIIProofTableMatchesBlueprint() {
        for (row in proofTable) {
            val result = AlignmentScorer.evaluate(perfectAnglesWithFrontKnee(row.knee), allVisible, specs)
            val knee = result.evaluations.first { it.spec.name == "Front Knee" }
            val label = "knee=${row.knee}"

            assertEquals("$label excess", row.excess, knee.excessDeg, 0.05)
            assertEquals("$label s_knee", row.kneeScore, knee.jointScore, 0.06)
            assertEquals("$label S_weighted", row.weighted, result.weightedScore, 0.06)
            assertEquals("$label cap", row.cap, result.criticalCapValue, 0.06)
            assertEquals("$label S_align", row.final, result.finalScore!!, 0.06)
            assertEquals("$label hold", row.holding, result.finalScore!! >= AsanaSpecs.HOLD_THRESHOLD)
        }
    }

    @Test
    fun stageDemoWrongPoseSpeaksKneeCueAndIsCapped() {
        val result = AlignmentScorer.evaluate(perfectAnglesWithFrontKnee(125.0), allVisible, specs)
        assertEquals(41, Math.round(result.finalScore!!).toInt())
        assertTrue(result.isCappedByCritical)
        assertEquals("Bend your front knee deeper toward 90 degrees", result.primaryCorrectiveCue)
        assertEquals("Front Knee", result.evaluations.first().spec.name)
    }

    @Test
    fun kneeTooDeepGivesEaseUpCue() {
        val result = AlignmentScorer.evaluate(perfectAnglesWithFrontKnee(65.0), allVisible, specs)
        assertEquals("Ease up slightly so your knee stays above your ankle", result.primaryCorrectiveCue)
    }

    @Test
    fun deadbandGivesFullScoreAndNoCue() {
        val result = AlignmentScorer.evaluate(perfectAnglesWithFrontKnee(102.9), allVisible, specs)
        assertEquals(100.0, result.finalScore!!, 1e-9)
        assertNull(result.primaryCorrectiveCue)
        assertFalse(result.isCappedByCritical)
    }

    @Test
    fun beginnerToleranceWidensDeadband() {
        // 108° is 13° off; Beginner tolerance = 8 * 1.35 = 10.8° -> excess 2.2°
        val standard = AlignmentScorer.evaluate(perfectAnglesWithFrontKnee(108.0), allVisible, specs, 1.0)
        val beginner = AlignmentScorer.evaluate(
            perfectAnglesWithFrontKnee(108.0), allVisible, specs, ToleranceProfile.BEGINNER.multiplier
        )
        val advanced = AlignmentScorer.evaluate(
            perfectAnglesWithFrontKnee(108.0), allVisible, specs, ToleranceProfile.ADVANCED.multiplier
        )
        assertTrue(beginner.finalScore!! > standard.finalScore!!)
        assertTrue(advanced.finalScore!! < standard.finalScore!!)
        assertEquals(2.2, beginner.evaluations.first { it.spec.name == "Front Knee" }.excessDeg, 1e-9)
    }

    @Test
    fun hiddenJointsAreRenormalizedOut() {
        // Hide both arms (0.30 of weight): 70% visible -> still scored, renormalized
        val visibility = allVisible + mapOf("Front Arm Level" to false, "Back Arm Level" to false)
        val result = AlignmentScorer.evaluate(perfectAnglesWithFrontKnee(95.0), visibility, specs)
        assertEquals(100.0, result.finalScore!!, 1e-9)
        assertEquals(3, result.evaluations.size)
    }

    @Test
    fun lessThanSixtyPercentVisibleReturnsNull() {
        // Hide front knee (0.30) + torso (0.20) = 50% visible
        val visibility = allVisible + mapOf("Front Knee" to false, "Torso Verticality" to false)
        val result = AlignmentScorer.evaluate(perfectAnglesWithFrontKnee(95.0), visibility, specs)
        assertNull(result.finalScore)
        assertEquals(AlignmentScorer.NOT_VISIBLE_CUE, result.primaryCorrectiveCue)
    }

    @Test
    fun scoreIsZeroBeyondMaxDeviation() {
        val result = AlignmentScorer.evaluate(perfectAnglesWithFrontKnee(170.0), allVisible, specs)
        val knee = result.evaluations.first { it.spec.name == "Front Knee" }
        assertEquals(0.0, knee.jointScore, 1e-9)
        assertEquals(15.0, result.finalScore!!, 1e-9) // cap floor = 15 + 0.85 * 0
    }

    @Test
    fun emptySpecsDoNotCrash() {
        val result = AlignmentScorer.evaluate(emptyMap(), emptyMap(), emptyList())
        assertNull(result.finalScore)
    }
}
