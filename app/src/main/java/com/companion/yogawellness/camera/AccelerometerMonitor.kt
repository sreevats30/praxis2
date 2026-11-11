package com.companion.yogawellness.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.companion.yogawellness.pose.GravityAligner
import com.companion.yogawellness.pose.MotionSteadinessTracker
import com.companion.yogawellness.pose.SensorSnapshot

/**
 * Sensor.TYPE_ACCELEROMETER only (works on budget phones without a gyroscope, §2):
 * feeds the pre-flight tilt and steadiness checks.
 */
class AccelerometerMonitor(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val motion = MotionSteadinessTracker()

    @Volatile
    private var last: FloatArray? = null
    private var running = false

    val isAvailable: Boolean get() = sensor != null

    fun start() {
        if (running || sensor == null) return
        running = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!running) return
        sensorManager?.unregisterListener(this)
        running = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val v = event.values
        last = floatArrayOf(v[0], v[1], v[2])
        motion.add(event.timestamp, v[0], v[1], v[2])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Null fields mean "no data", which the pre-flight guard treats as non-blocking. */
    fun snapshot(): SensorSnapshot {
        val v = last ?: return SensorSnapshot(null, null)
        return SensorSnapshot(GravityAligner.checkPhoneTilt(v[0], v[1], v[2]), motion.sd())
    }
}
