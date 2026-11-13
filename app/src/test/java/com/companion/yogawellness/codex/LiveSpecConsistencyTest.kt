package com.companion.yogawellness.codex

import com.companion.yogawellness.scoring.AsanaSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** The Learn tab must never show a live pose with numbers that disagree with the scorer. */
class LiveSpecConsistencyTest {
    @Test fun liveCalloutsMatchScorerTargetsWithinOneDegree() {
        var matched = 0
        for (item in AsanaCatalog32.liveItems) {
            val specs = AsanaSpecs.specsFor(item.liveAsana!!).associateBy { it.name }
            for (c in item.callouts) {
                val spec = specs[c.label] ?: continue
                matched++
                assertTrue(
                    "${item.id} '${c.label}': catalog ${c.targetDeg}° vs scorer ${spec.targetDeg}°",
                    abs(c.targetDeg - spec.targetDeg) <= 1.0
                )
            }
        }
        // Warrior II 5 + Chair 5 + Tree 5 + Triangle 4 (its Neck Alignment has no callout).
        assertEquals(19, matched)
    }
}
