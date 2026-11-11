package com.companion.yogawellness.pose

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class PhoneTiltStatus(
    val isUprightOk: Boolean,
    val pitchDeg: Double,
    val rollDeg: Double,
    val warningMessage: String?
)

object GravityAligner {

    const val MAX_TILT_DEG = 15.0

    /**
     * Works on 100% of Android devices using Sensor.TYPE_ACCELEROMETER (ax, ay, az).
     * Checks if phone is propped reasonably upright (within +-15 deg pitch/roll).
     */
    fun checkPhoneTilt(ax: Float, ay: Float, az: Float): PhoneTiltStatus {
        val norm = sqrt((ax * ax + ay * ay + az * az).toDouble())
        if (norm < 1.0) return PhoneTiltStatus(true, 0.0, 0.0, null)

        // In portrait Android, +Y points up out of the top of the phone, +Z out of screen
        val pitchDeg = Math.toDegrees(atan2(az.toDouble(), sqrt((ax * ax + ay * ay).toDouble())))
        val rollDeg = Math.toDegrees(atan2(ax.toDouble(), ay.toDouble()))

        val warn = when {
            abs(pitchDeg) > MAX_TILT_DEG -> "Tilt your phone more upright (currently tilted ${pitchDeg.toInt()}°)"
            abs(rollDeg) > MAX_TILT_DEG -> "Level your phone horizontally"
            else -> null
        }
        return PhoneTiltStatus(warn == null, pitchDeg, rollDeg, warn)
    }
}
