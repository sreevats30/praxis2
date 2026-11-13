package com.companion.yogawellness.codex

import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.codex.CalloutJoint as J

internal val HIP_POSES = listOf(
    AsanaCodexItem(
        id = "triangle_pose", number = 24, sanskritName = "Trikonasana", englishName = "Triangle Pose",
        category = AsanaCategory.HIPS, everydayBenefit = "Lengthens the hamstrings, inner thighs and waist",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        liveAsana = Asana.TRIKONASANA,
        start = PoseAngles.STANDING,
        peak = PoseAngles(trunkRoll = 68.0f, lShoulderAbd = 68.0f, rShoulderAbd = 104.4f, lHipAbd = 35.0f, lKneeFlex = 5.0f,
            rHipAbd = 35.0f, rKneeFlex = 4.0f),
        // Labels and targets equal AsanaSpecs.TRIKONASANA (LiveSpecConsistencyTest).
        callouts = listOf(
            JointCallout("Front Knee", J.L_KNEE, 175, 7), JointCallout("Back Knee", J.R_KNEE, 176, 7),
            JointCallout("Trunk Lateral Tilt", J.TRUNK_TILT, 68, 10), JointCallout("Vertical Arm Line", J.ARM_LINE, 174, 8)
        ),
        steps = listOf(
            "Stand with the feet wide, the front foot pointing forward and the back foot turned slightly in.",
            "Reach forward over the front leg, then tip the torso sideways.",
            "Rest the lower hand on the shin, a block or the floor.",
            "Stack the top arm straight up above the bottom arm.",
            "Rise up and switch sides."
        ),
        commonMistakes = listOf(
            "Locking or hyperextending the front knee.", "Rolling the chest toward the floor.", "Shortening the bottom side of the waist."
        ),
        safety = "Knees: keep a micro-bend in the front knee (the live coach flags hyperextension past 182°).",
        modifications = listOf("Hand on a block or high on the shin.", "Gaze down instead of up.")
    ),
    AsanaCodexItem(
        id = "extended_side_angle", number = 25, sanskritName = "Utthita Parsvakonasana", englishName = "Extended Side Angle",
        category = AsanaCategory.HIPS, everydayBenefit = "Opens the groin and lengthens the whole side body",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(trunkRoll = 59.4f, lShoulderAbd = 60.0f, lElbowFlex = 60.0f, rShoulderAbd = 178.6f, lHipAbd = 85.0f,
            lHipRot = 90.0f, lKneeFlex = 85.0f, rHipAbd = 58.0f),
        callouts = listOf(
            JointCallout("Front knee", J.L_KNEE, 95, 8), JointCallout("Back leg to top arm line", J.R_SIDE_LINE, 180, 10)
        ),
        steps = listOf(
            "From Warrior II, keep the front knee bent over the ankle.",
            "Rest the front forearm on the thigh, or the hand on a block.",
            "Reach the top arm overhead so it makes one line with the back leg.",
            "Keep the chest open and the back foot grounded.",
            "Come up and switch sides."
        ),
        commonMistakes = listOf("Sinking the weight into the front arm.", "Front knee collapsing inward."),
        safety = "Knees: keep the front knee over the ankle and the back leg active.",
        modifications = listOf("Forearm on the thigh.", "Top hand on the hip.")
    ),
    AsanaCodexItem(
        id = "yogic_squat", number = 26, sanskritName = "Malasana", englishName = "Garland Pose (Deep Squat)",
        category = AsanaCategory.HIPS, everydayBenefit = "Opens the ankles, knees and hips",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 6), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(trunkPitch = 10.0f, lShoulderFlex = 40.0f, lShoulderAbd = 10.0f, lElbowFlex = 110.0f, rShoulderFlex = 40.0f,
            rShoulderAbd = 10.0f, rElbowFlex = 110.0f, lHipFlex = 123.6f, lHipAbd = -38.1f, lHipRot = -27.7f, lKneeFlex = 130.0f,
            rHipFlex = 123.6f, rHipAbd = -38.1f, rHipRot = -27.7f, rKneeFlex = 130.0f, lAnkleFlex = -40.0f, rAnkleFlex = -40.0f),
        callouts = listOf(
            JointCallout("Deep knee bend", J.L_KNEE, 50, 8), JointCallout("Hip fold", J.L_HIP, 55, 8),
            JointCallout("Spine upright", J.TRUNK_TILT, 10, 6)
        ),
        steps = listOf(
            "Stand with the feet a little wider than the hips, toes turned out.",
            "Bend the knees and lower the hips toward the floor.",
            "Bring the palms together and press the elbows gently into the inner knees.",
            "Lengthen the spine upward.",
            "Use the hands to rise up slowly."
        ),
        commonMistakes = listOf("Heels lifting high off the floor.", "Knees collapsing inward."),
        safety = "Knees: sit on a block and roll a blanket under the heels if the knees or ankles strain.",
        modifications = listOf("Sit on a block.", "Blanket under the heels.")
    ),
    AsanaCodexItem(
        id = "bound_angle", number = 27, sanskritName = "Baddha Konasana", englishName = "Butterfly Pose",
        category = AsanaCategory.HIPS, everydayBenefit = "Gently opens the inner thighs and hips",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 45, breath = BreathPattern(4, 0, 6), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = PoseAngles.SEATED,
        peak = PoseAngles(lShoulderFlex = 30.0f, lElbowFlex = 20.0f, rShoulderFlex = 30.0f, rElbowFlex = 20.0f, lHipFlex = 35.0f,
            lHipAbd = 102.4f, lHipRot = -176.8f, lKneeFlex = 151.3f, rHipFlex = 35.0f, rHipAbd = 102.4f, rHipRot = -176.8f, rKneeFlex = 151.3f),
        callouts = listOf(
            JointCallout("Spine vertical", J.TRUNK_TILT, 0, 5), JointCallout("Knees opening outward", J.L_THIGH_OPENING, 55, 8)
        ),
        steps = listOf(
            "Sit tall and bring the soles of the feet together.",
            "Let the knees fall open to the sides.",
            "Hold the feet or ankles and lengthen the spine.",
            "Breathe and let the thighs soften without pushing them down."
        ),
        commonMistakes = listOf("Rounding the lower back.", "Pressing the knees down with the hands."),
        safety = "Hips and knees: support the knees on cushions if they feel strained, and sit on a folded blanket.",
        modifications = listOf("Cushions under the knees.", "Feet further from the body.")
    ),
    AsanaCodexItem(
        id = "wide_forward_fold", number = 28, sanskritName = "Prasarita Padottanasana", englishName = "Wide-Legged Forward Fold",
        category = AsanaCategory.HIPS, everydayBenefit = "Lengthens the inner thighs and hamstrings",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 6), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(trunkPitch = 131.5f, neckPitch = 5.0f, lShoulderFlex = -30.0f, lShoulderAbd = 20.0f, lElbowFlex = 100.0f,
            rShoulderFlex = -30.0f, rShoulderAbd = 20.0f, rElbowFlex = 100.0f, lHipAbd = 30.0f, lKneeFlex = 5.0f, rHipAbd = 30.0f, rKneeFlex = 5.0f),
        callouts = listOf(
            JointCallout("Legs (knees)", J.L_KNEE, 175, 5), JointCallout("Hip hinge", J.L_HIP, 55, 8),
            JointCallout("Spine long", J.NECK, 175, 6)
        ),
        steps = listOf(
            "Stand with the feet wide and parallel.",
            "Place the hands on the hips and lengthen the spine.",
            "Hinge forward from the hips and place the hands on the floor or blocks.",
            "Let the crown of the head reach toward the floor.",
            "Return to standing slowly with a flat back."
        ),
        commonMistakes = listOf("Rounding the back to reach the floor.", "Rolling onto the outer edges of the feet."),
        safety = "Lower back and head below heart: bend the knees, use blocks, and come up slowly if you feel dizzy.",
        modifications = listOf("Hands on blocks.", "Bend the knees.")
    )
)
