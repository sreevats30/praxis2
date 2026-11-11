package com.companion.yogawellness.classifier

import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporalLockTest {

    private val warrior = AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT)
    private val tree = AsanaLabel(Asana.VRIKSHASANA, PoseSide.LEFT_LIFTED)

    @Test
    fun locksOnFourteenthMatchingFrame() {
        val lock = TemporalLock()
        repeat(13) { assertNull(lock.update(warrior).locked) }
        val s = lock.update(warrior)
        assertEquals(warrior, s.locked)
        assertTrue(s.changed)
        assertFalse(lock.update(warrior).changed)
    }

    @Test
    fun fourteenOfEighteenWithNoiseStillLocks() {
        val lock = TemporalLock()
        val seq = listOf(warrior, null, warrior, warrior, tree, warrior, warrior, warrior, null, warrior,
            warrior, warrior, warrior, warrior, warrior, warrior, null, warrior)
        var last: LockState? = null
        seq.forEach { last = lock.update(it) }
        assertEquals(14, seq.count { it == warrior }) // exactly the lock threshold
        assertEquals(warrior, last!!.locked)
    }

    @Test
    fun flickerNeverLocks() {
        val lock = TemporalLock()
        repeat(100) { i -> assertNull(lock.update(if (i % 2 == 0) warrior else tree).locked) }
    }

    @Test
    fun shortRejectionKeepsLockButTwentyFiveUnlocks() {
        val lock = TemporalLock()
        repeat(14) { lock.update(warrior) }
        repeat(24) { assertEquals(warrior, lock.update(null).locked) }
        assertEquals(warrior, lock.update(warrior).locked) // adjusting the knee never drops the lock
        repeat(24) { lock.update(null) }
        val s = lock.update(null)
        assertNull(s.locked)
        assertTrue(s.changed)
    }

    @Test
    fun switchesToNewPoseAfterUnlock() {
        val lock = TemporalLock()
        repeat(14) { lock.update(warrior) }
        var s: LockState? = null
        repeat(25) { s = lock.update(tree) }
        assertEquals(tree, s!!.locked)
    }
}
