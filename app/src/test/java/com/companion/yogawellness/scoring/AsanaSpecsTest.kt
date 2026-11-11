package com.companion.yogawellness.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsanaSpecsTest {

    @Test
    fun rosterHasFiveAsanasWithFourScored() {
        assertEquals(5, Asana.entries.size)
        assertEquals(4, AsanaSpecs.scoredAsanas.size)
        assertTrue(AsanaSpecs.specsFor(Asana.TADASANA).isEmpty())
    }

    @Test
    fun weightsSumToOneForEveryScoredAsana() {
        for (asana in AsanaSpecs.scoredAsanas) {
            assertEquals(asana.name, 1.0, AsanaSpecs.specsFor(asana).sumOf { it.weight }, 1e-9)
        }
    }

    @Test
    fun jointNamesAreUniqueAndEachAsanaHasACriticalJoint() {
        for (asana in AsanaSpecs.scoredAsanas) {
            val specs = AsanaSpecs.specsFor(asana)
            assertEquals(asana.name, specs.size, specs.map { it.name }.toSet().size)
            assertTrue(asana.name, specs.any { it.isCritical })
            specs.forEach {
                assertTrue(it.toleranceDeg > 0 && it.maxDeviationDeg > 0)
                assertTrue(it.cueWhenTooLarge.isNotBlank() && it.cueWhenTooSmall.isNotBlank())
            }
        }
    }

    @Test
    fun cameraAnglesAndZWeightsMatchSection4() {
        assertEquals(CameraAngle.SIDE_OR_45, Asana.UTKATASANA.cameraAngle)
        assertEquals(0.15f, Asana.UTKATASANA.zWeight)
        assertEquals(30, Asana.UTKATASANA.maxHoldSec)
        listOf(Asana.TADASANA, Asana.VRIKSHASANA, Asana.VIRABHADRASANA_II, Asana.TRIKONASANA).forEach {
            assertEquals(CameraAngle.FRONT, it.cameraAngle)
            assertEquals(0.25f, it.zWeight)
        }
    }

    @Test
    fun warriorFrontKneeMatchesProofTableParameters() {
        val knee = AsanaSpecs.VIRABHADRASANA_II.first { it.name == "Front Knee" }
        assertEquals(95.0, knee.targetDeg, 0.0)
        assertEquals(8.0, knee.toleranceDeg, 0.0)
        assertEquals(30.0, knee.maxDeviationDeg, 0.0)
        assertEquals(0.30, knee.weight, 0.0)
        assertTrue(knee.isCritical)
    }
}
