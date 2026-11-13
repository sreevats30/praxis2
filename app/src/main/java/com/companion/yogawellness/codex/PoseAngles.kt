package com.companion.yogawellness.codex

/**
 * Degrees of freedom of the rig, all in degrees, all 0 = neutral Tadasana (§5.3).
 *
 *  - root*: orientation of the whole body. rootPitch + tips the head toward the viewer/forward
 *    (90 = prone), rootRoll + tips the body toward the figure's LEFT.
 *  - trunk*: pelvis→neck relative to the root. Pitch + = forward fold, roll + = side bend to the
 *    figure's left, twist = axial rotation.
 *  - neckPitch: + = chin toward chest, − = head back.
 *  - shoulderFlex: arm starts pointing down; 90 = forward, 180 = overhead (values past 180 keep
 *    rotating behind the head). shoulderAbd: then outward in the frontal plane (90 = sideways).
 *    elbowFlex: 0 = straight; the forearm bends in the arm's flexion plane.
 *  - hipFlex / hipAbd: same convention for the thigh. kneeFlex: 0 = straight.
 *    hipRot: rotates the plane in which the knee bends about the thigh axis
 *    (0 = shin swings backward; ±90 = shin swings in the frontal plane, e.g. Warrior II).
 *  - ankleFlex: 0 = foot perpendicular to the shin; + = pointing the toes.
 *
 * Sign conventions are pinned by ForwardKinematicsTest, not by this comment.
 */
data class PoseAngles(
    val rootPitch: Float = 0f,
    val rootRoll: Float = 0f,
    val trunkPitch: Float = 0f,
    val trunkRoll: Float = 0f,
    val trunkTwist: Float = 0f,
    val neckPitch: Float = 0f,
    val lShoulderFlex: Float = 0f,
    val lShoulderAbd: Float = 0f,
    val lElbowFlex: Float = 0f,
    val rShoulderFlex: Float = 0f,
    val rShoulderAbd: Float = 0f,
    val rElbowFlex: Float = 0f,
    val lHipFlex: Float = 0f,
    val lHipAbd: Float = 0f,
    val lHipRot: Float = 0f,
    val lKneeFlex: Float = 0f,
    val rHipFlex: Float = 0f,
    val rHipAbd: Float = 0f,
    val rHipRot: Float = 0f,
    val rKneeFlex: Float = 0f,
    val lAnkleFlex: Float = 0f,
    val rAnkleFlex: Float = 0f
) {
    companion object {
        /** Neutral standing (Tadasana). */
        val STANDING = PoseAngles()
        /** Seated on the floor, legs forward, spine upright. */
        val SEATED = PoseAngles(lHipFlex = 90f, rHipFlex = 90f)
        /** Hands and knees (tabletop), as used for floor-pose starts. */
        val TABLETOP = PoseAngles(
            rootPitch = 82.6f, lShoulderFlex = 90f, rShoulderFlex = 90f, lElbowFlex = 5f, rElbowFlex = 5f,
            lHipFlex = 87.6f, rHipFlex = 87.6f, lKneeFlex = 92f, rKneeFlex = 92f, lAnkleFlex = 90f, rAnkleFlex = 90f
        )
        /** Lying face down, arms bent beside the chest. */
        val PRONE = PoseAngles(rootPitch = 90f, lShoulderFlex = 160f, rShoulderFlex = 160f, lElbowFlex = 150f, rElbowFlex = 150f,
            lAnkleFlex = 80f, rAnkleFlex = 80f)
        /** Lying on the back. */
        val SUPINE = PoseAngles(rootPitch = -90f)

        /** Linear interpolation of every angle; bone lengths are not involved. */
        fun lerp(a: PoseAngles, b: PoseAngles, t: Float): PoseAngles {
            fun m(x: Float, y: Float) = x + (y - x) * t
            return PoseAngles(
                m(a.rootPitch, b.rootPitch), m(a.rootRoll, b.rootRoll),
                m(a.trunkPitch, b.trunkPitch), m(a.trunkRoll, b.trunkRoll), m(a.trunkTwist, b.trunkTwist),
                m(a.neckPitch, b.neckPitch),
                m(a.lShoulderFlex, b.lShoulderFlex), m(a.lShoulderAbd, b.lShoulderAbd), m(a.lElbowFlex, b.lElbowFlex),
                m(a.rShoulderFlex, b.rShoulderFlex), m(a.rShoulderAbd, b.rShoulderAbd), m(a.rElbowFlex, b.rElbowFlex),
                m(a.lHipFlex, b.lHipFlex), m(a.lHipAbd, b.lHipAbd), m(a.lHipRot, b.lHipRot), m(a.lKneeFlex, b.lKneeFlex),
                m(a.rHipFlex, b.rHipFlex), m(a.rHipAbd, b.rHipAbd), m(a.rHipRot, b.rHipRot), m(a.rKneeFlex, b.rKneeFlex),
                m(a.lAnkleFlex, b.lAnkleFlex), m(a.rAnkleFlex, b.rAnkleFlex)
            )
        }
    }
}
