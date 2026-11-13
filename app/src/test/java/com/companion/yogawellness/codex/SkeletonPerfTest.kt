package com.companion.yogawellness.codex

import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards against accidental allocation-heavy rewrites of the solver (§5.7). */
class SkeletonPerfTest {
    @Test fun thousandSolvesAreFast() {
        val item = AsanaCatalog32.byId("extended_side_angle")!!
        fun run(n: Int): Long {
            val t0 = System.nanoTime()
            var sink = 0.0
            for (i in 0 until n) {
                val p = PoseTimeline.anglesAt(item.start, item.peak, i * 0.016f, 1f, item.breath, false)
                sink += ForwardKinematics.solve(p, 45f)[SkeletonJoint.HEAD].y
            }
            check(sink.isFinite())
            return (System.nanoTime() - t0) / 1_000_000
        }
        run(2000) // JIT warm-up
        val ms = run(1000)
        assertTrue("1000 solves took $ms ms", ms < 200)
    }
}
