package com.companion.yogawellness.pose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class OneEuroFilterTest {

    @Test
    fun firstSampleIsPassedThrough() {
        val f = OneEuroFilter.forDegrees()
        assertEquals(123.0, f.filter(123.0, 0.0), 0.0)
    }

    @Test
    fun constantSignalStaysConstant() {
        val f = OneEuroFilter.forDegrees()
        var out = 0.0
        for (i in 0 until 100) out = f.filter(90.0, i / 25.0)
        assertEquals(90.0, out, 1e-9)
    }

    @Test
    fun reducesJitterOnStaticPose() {
        val rnd = Random(42)
        val f = OneEuroFilter.forDegrees()
        val raw = mutableListOf<Double>()
        val smooth = mutableListOf<Double>()
        for (i in 0 until 250) {
            val x = 95.0 + rnd.nextDouble(-4.0, 4.0)
            raw += x
            smooth += f.filter(x, i / 25.0)
        }
        fun sd(v: List<Double>): Double {
            val tail = v.drop(25)
            val m = tail.average()
            return sqrt(tail.sumOf { (it - m) * (it - m) } / tail.size)
        }
        assertTrue("smoothed SD ${sd(smooth)} should be < half raw SD ${sd(raw)}", sd(smooth) < 0.5 * sd(raw))
    }

    @Test
    fun followsLargeStepWithinHalfASecond() {
        val f = OneEuroFilter.forDegrees()
        for (i in 0 until 25) f.filter(125.0, i / 25.0)
        var out = 125.0
        for (i in 25 until 38) out = f.filter(95.0, i / 25.0) // ~0.5 s after the step
        assertTrue("filter should be within 3° of the new value, got $out", abs(out - 95.0) < 3.0)
    }

    @Test
    fun nanAndDuplicateTimestampsAreSafe() {
        val f = OneEuroFilter.forDegrees()
        f.filter(10.0, 0.0)
        assertTrue(f.filter(Double.NaN, 0.04).isNaN())
        assertEquals(10.0, f.filter(50.0, 0.0), 0.0) // non-increasing time -> previous estimate
    }

    @Test
    fun filterBankKeepsJointsIndependent() {
        val bank = AngleFilterBank()
        bank.filter(mapOf("a" to 10.0, "b" to 100.0), 0.0)
        val out = bank.filter(mapOf("a" to 10.0, "b" to 100.0), 0.04)
        assertEquals(10.0, out["a"]!!, 1e-9)
        assertEquals(100.0, out["b"]!!, 1e-9)
    }
}
