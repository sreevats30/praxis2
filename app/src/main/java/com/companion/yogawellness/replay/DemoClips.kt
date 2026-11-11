package com.companion.yogawellness.replay

import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.PoseFrame
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic synthetic clips. The Warrior II correction clip reproduces the stage demo
 * (§4 "Deliberately Wrong → Live Correction") without a camera, e.g. as a fallback on stage.
 */
object DemoClips {
    const val WARRIOR_CORRECTION_NAME = "Demo · Warrior II correction (synthetic)"

    fun warriorCorrection(fps: Int = 25, seed: Int = 7): PoseClip {
        val b = SyntheticPoseBuilder()
        val rnd = Random(seed)
        val dtMs = 1000L / fps
        val frames = ArrayList<ClipFrame>()
        var t = 1_000L
        fun add(f: PoseFrame) {
            frames += ClipFrame(jitter(f, rnd), null)
            t += dtMs
        }
        fun sway(): Double = 1.5 * sin(2 * PI * 0.35 * t / 1000.0)

        repeat(6 * fps) { add(b.tadasana(t, dx = sway())) }                                  // calibrate
        repeat(5 * fps) { add(b.warriorII(t, BodySide.LEFT, 125.0, dx = sway())) }           // wrong
        val steps = 3 * fps
        repeat(steps) { i -> add(b.warriorII(t, BodySide.LEFT, 125.0 - 30.0 * (i + 1) / steps, dx = sway())) } // correcting
        repeat(10 * fps) { add(b.warriorII(t, BodySide.LEFT, 95.0, dx = sway())) }           // hold
        return PoseClip(WARRIOR_CORRECTION_NAME, null, frames)
    }

    /** ±~1 px landmark noise, similar to MediaPipe jitter on a steady subject. */
    private fun jitter(f: PoseFrame, rnd: Random): PoseFrame = f.copy(
        landmarks = f.landmarks.map {
            it.copy(
                x = it.x + rnd.nextDouble(-0.002, 0.002).toFloat(),
                y = it.y + rnd.nextDouble(-0.0015, 0.0015).toFloat()
            )
        }
    )
}
