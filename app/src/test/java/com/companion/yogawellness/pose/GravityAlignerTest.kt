package com.companion.yogawellness.pose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class GravityAlignerTest {

    private val g = 9.81f

    @Test
    fun uprightPortraitPhoneIsOk() {
        val s = GravityAligner.checkPhoneTilt(0f, g, 0f)
        assertTrue(s.isUprightOk)
        assertEquals(0.0, s.pitchDeg, 1e-6)
        assertEquals(0.0, s.rollDeg, 1e-6)
        assertNull(s.warningMessage)
    }

    @Test
    fun tenDegreesBackwardIsStillOk() {
        val t = Math.toRadians(10.0)
        val s = GravityAligner.checkPhoneTilt(0f, (g * cos(t)).toFloat(), (g * sin(t)).toFloat())
        assertTrue(s.isUprightOk)
        assertEquals(10.0, s.pitchDeg, 1e-3)
    }

    @Test
    fun twentyFiveDegreesBackwardWarnsToProphoneUpright() {
        val t = Math.toRadians(25.0)
        val s = GravityAligner.checkPhoneTilt(0f, (g * cos(t)).toFloat(), (g * sin(t)).toFloat())
        assertFalse(s.isUprightOk)
        assertTrue(s.warningMessage!!.startsWith("Tilt your phone more upright"))
    }

    @Test
    fun sidewaysRollWarnsToLevel() {
        val t = Math.toRadians(30.0)
        val s = GravityAligner.checkPhoneTilt((g * sin(t)).toFloat(), (g * cos(t)).toFloat(), 0f)
        assertFalse(s.isUprightOk)
        assertEquals("Level your phone horizontally", s.warningMessage)
        assertEquals(30.0, s.rollDeg, 1e-3)
    }

    @Test
    fun freeFallOrNoDataIsIgnored() {
        assertTrue(GravityAligner.checkPhoneTilt(0f, 0f, 0f).isUprightOk)
    }
}
