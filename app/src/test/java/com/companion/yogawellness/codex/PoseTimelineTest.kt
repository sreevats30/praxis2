package com.companion.yogawellness.codex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PoseTimelineTest {
    private fun check(t: Float, phase: Phase, blend: Float, speed: Float = 1f) {
        val s = PoseTimeline.timelineAt(t, speed)
        assertEquals("t=$t phase", phase, s.phase)
        assertEquals("t=$t blend", blend, s.blend, 1e-4f)
    }

    @Test fun eightSecondLoopAtNormalSpeed() {
        check(0f, Phase.ENTER, 0f)
        check(1f, Phase.ENTER, 0.5f)
        check(2f, Phase.HOLD, 1f)
        check(4f, Phase.HOLD, 1f)
        check(6f, Phase.EXIT, 1f)
        check(7f, Phase.EXIT, 0.5f)
        check(8f, Phase.ENTER, 0f) // wraps
        check(9f, Phase.ENTER, 0.5f)
    }

    @Test fun halfSpeedStretchesTheLoopTo16s() {
        check(0f, Phase.ENTER, 0f, 0.5f)
        check(2f, Phase.ENTER, 0.5f, 0.5f)
        check(4f, Phase.HOLD, 1f, 0.5f)
        check(12f, Phase.EXIT, 1f, 0.5f)
        check(14f, Phase.EXIT, 0.5f, 0.5f)
        check(16f, Phase.ENTER, 0f, 0.5f)
    }

    @Test fun breathingOffsetIsBoundedAndOnlyInHold() {
        val pattern = BreathPattern(4, 2, 6)
        var maxSeen = 0f
        var t = 0f
        while (t < 40f) {
            for (phase in Phase.entries) {
                val b = PoseTimeline.breathOffset(t, pattern, phase)
                assertTrue("|$b| ≤ 1.5", abs(b) <= PoseTimeline.BREATH_DEG + 1e-6f)
                if (phase != Phase.HOLD) assertEquals(0f, b, 0f) else maxSeen = maxOf(maxSeen, abs(b))
            }
            t += 0.05f
        }
        assertEquals(1.5f, maxSeen, 0.01f)
    }

    @Test fun breathStepsFollowThePattern() {
        val p = BreathPattern(4, 2, 6)
        assertEquals(12, p.cycleSec)
        assertEquals(BreathStep.INHALE, PoseTimeline.breathAt(1f, p).step)
        assertEquals(BreathStep.HOLD, PoseTimeline.breathAt(5f, p).step)
        assertEquals(BreathStep.EXHALE, PoseTimeline.breathAt(7f, p).step)
        assertEquals(BreathStep.INHALE, PoseTimeline.breathAt(13f, p).step)
        assertEquals("Inhale 4 s · hold 2 s · exhale 6 s", p.format())
        assertEquals("Inhale 4 s · exhale 4 s", BreathPattern(4, 0, 4).format())
    }

    @Test fun pausedAtPeakFreezesTheBlendAtOne() {
        val item = AsanaCatalog32.byId("warrior_2")!!
        val a = PoseTimeline.anglesAt(item.start, item.peak, 0f, 1f, item.breath, pausedAtPeak = true)
        assertEquals(item.peak.lKneeFlex, a.lKneeFlex, 1e-4f)
        val atStart = PoseTimeline.anglesAt(item.start, item.peak, 0f, 1f, item.breath, pausedAtPeak = false)
        assertEquals(item.start.lKneeFlex, atStart.lKneeFlex, 1e-4f)
    }
}
