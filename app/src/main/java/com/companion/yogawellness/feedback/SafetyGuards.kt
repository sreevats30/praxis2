package com.companion.yogawellness.feedback

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.pose.BodyPart
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.JointAngleExtractor
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.PoseGeometry
import com.companion.yogawellness.scoring.Asana
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sign

data class SafetyAlert(val id: String, val asana: Asana, val message: String)

/**
 * Built-in physical safety rules (§4). These are checked before any alignment cue (§3 Stage 4).
 */
object SafetyGuards {

    /** Tree: λ_tree = vertical position of the lifted ankle along the standing thigh (1.0 = knee). */
    const val TREE_LAMBDA_MIN = 0.88
    const val TREE_LAMBDA_MAX = 1.12
    /** The lifted foot only "presses" on the knee if it is horizontally close to the standing leg. */
    const val TREE_NEAR_TORSO_FRACTION = 0.35

    /** Warrior II: front knee may not pass the front ankle by more than 15% of lower-leg length. */
    const val WARRIOR_OVERSHOOT_FRACTION = 0.15

    /** Triangle: front knee must not exceed 182° (hyperextension). */
    const val TRIANGLE_HYPEREXTENSION_DEG = 182.0

    const val TREE_KNEE_MESSAGE = "Move your lifted foot above or below your knee — never press on the knee joint"
    const val WARRIOR_KNEE_MESSAGE = "Your front knee is past your ankle. Ease up or widen your stance to protect the knee"
    const val TRIANGLE_KNEE_MESSAGE = "Soften your front knee — don't lock it backwards"

    private const val MIN_VIS = JointAngleExtractor.JOINT_VISIBILITY_MIN

    fun evaluate(label: AsanaLabel?, frame: PoseFrame, smoothedAngles: Map<String, Double>): SafetyAlert? {
        if (label == null || !frame.hasPose) return null
        return when (label.asana) {
            Asana.VRIKSHASANA -> {
                val lifted = label.side.workingSide ?: return null
                val lambda = treeLambda(frame, lifted) ?: return null
                if (lambda in TREE_LAMBDA_MIN..TREE_LAMBDA_MAX) SafetyAlert("tree_knee", label.asana, TREE_KNEE_MESSAGE) else null
            }

            Asana.VIRABHADRASANA_II -> {
                val overshoot = warriorKneeOvershoot(frame, label.side.frontSide) ?: return null
                if (overshoot > WARRIOR_OVERSHOOT_FRACTION) SafetyAlert("warrior_knee", label.asana, WARRIOR_KNEE_MESSAGE) else null
            }

            Asana.TRIKONASANA -> {
                val knee = smoothedAngles["Front Knee"] ?: return null
                if (knee > TRIANGLE_HYPEREXTENSION_DEG) SafetyAlert("triangle_knee", label.asana, TRIANGLE_KNEE_MESSAGE) else null
            }

            else -> null
        }
    }

    /**
     * λ_tree = (y_lifted_ankle − y_standing_hip) / (y_standing_knee − y_standing_hip).
     * Returns null if the landmarks are not visible or the lifted foot is not near the standing leg.
     */
    fun treeLambda(frame: PoseFrame, liftedSide: BodySide): Double? {
        val standing = liftedSide.opposite
        val iAnkle = BodyPart.ANKLE.index(liftedSide)
        val iHip = BodyPart.HIP.index(standing)
        val iKnee = BodyPart.KNEE.index(standing)
        if (listOf(iAnkle, iHip, iKnee).any { frame[it].visibility < MIN_VIS }) return null
        val g = PoseGeometry(frame, 0.0)
        val ankle = g.point(iAnkle)
        val hip = g.point(iHip)
        val knee = g.point(iKnee)
        val thigh = knee.y - hip.y
        if (thigh < 1.0) return null
        if (abs(ankle.x - knee.x) / g.torsoLengthPx > TREE_NEAR_TORSO_FRACTION) return null
        return (ankle.y - hip.y) / thigh
    }

    /**
     * How far the front knee passes the front ankle, in the direction the front foot points
     * (away from the body midline), as a fraction of lower-leg length. Negative = behind the ankle.
     */
    fun warriorKneeOvershoot(frame: PoseFrame, frontSide: BodySide): Double? {
        val iKnee = BodyPart.KNEE.index(frontSide)
        val iAnkle = BodyPart.ANKLE.index(frontSide)
        if (frame[iKnee].visibility < MIN_VIS || frame[iAnkle].visibility < MIN_VIS) return null
        val g = PoseGeometry(frame, 0.0)
        val knee = g.point(iKnee)
        val ankle = g.point(iAnkle)
        val outward = sign(ankle.x - g.midHip.x)
        if (outward == 0.0) return null
        val lowerLeg = hypot(knee.x - ankle.x, knee.y - ankle.y)
        if (lowerLeg < 1.0) return null
        return (knee.x - ankle.x) * outward / lowerLeg
    }
}
