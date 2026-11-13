package com.companion.yogawellness.codex

import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.codex.CalloutJoint as J

internal val BALANCE_POSES = listOf(
    AsanaCodexItem(
        id = "tree_pose", number = 19, sanskritName = "Vrikshasana", englishName = "Tree Pose",
        category = AsanaCategory.BALANCE, everydayBenefit = "Ankle stability, single-leg balance and calm focus",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        liveAsana = Asana.VRIKSHASANA,
        start = PoseAngles.STANDING,
        // The lifted foot rests on the inner calf: the knee guard forbids the knee, and the rig cannot
        // reach the inner thigh at the scorer's 75° lifted-knee target.
        peak = PoseAngles(lShoulderFlex = 170.0f, lElbowFlex = 12.0f, rShoulderFlex = 170.0f, rElbowFlex = 12.0f, lHipFlex = 23.0f,
            lHipAbd = 33.8f, lHipRot = 73.6f, lKneeFlex = 105.0f, rKneeFlex = 2.0f),
        // Labels and targets equal AsanaSpecs.VRIKSHASANA (LiveSpecConsistencyTest).
        callouts = listOf(
            JointCallout("Standing Knee", J.R_KNEE, 178, 7), JointCallout("Lifted Knee Flexion", J.L_KNEE, 75, 20),
            JointCallout("Pelvic Level", J.PELVIS_LEVEL, 0, 6), JointCallout("Torso Verticality", J.TRUNK_TILT, 0, 5),
            JointCallout("Overhead Elbows", J.L_ELBOW, 168, 12)
        ),
        steps = listOf(
            "Stand tall and shift the weight onto one foot.",
            "Place the other foot on the inner calf or inner thigh, never on the knee.",
            "Press foot and leg into each other and keep the hips level.",
            "Raise the arms overhead with soft elbows.",
            "Switch sides."
        ),
        commonMistakes = listOf(
            "Pressing the foot against the side of the knee.", "Letting the standing hip jut out sideways.", "Holding the breath."
        ),
        safety = "Balance and knees: stand near a wall or chair, and never press the lifted foot against the standing knee joint.",
        modifications = listOf("Keep the toes on the floor with the heel on the ankle.", "Hands together at the chest.")
    ),
    AsanaCodexItem(
        id = "warrior_3", number = 20, sanskritName = "Virabhadrasana III", englishName = "Warrior III",
        category = AsanaCategory.BALANCE, everydayBenefit = "Trains balance along the whole back of the body",
        difficulty = Difficulty.INTERMEDIATE, recommendedHoldSec = 15, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(rootPitch = 90.0f, lShoulderFlex = 180.0f, rShoulderFlex = 180.0f, lHipFlex = 90.0f, lKneeFlex = 5.0f),
        callouts = listOf(
            JointCallout("Standing knee", J.L_KNEE, 175, 7), JointCallout("Arms-trunk-leg line", J.R_LEG_LINE, 180, 10)
        ),
        steps = listOf(
            "Stand on one leg with a soft knee and the hands at the hips.",
            "Hinge forward from the hip while the other leg lifts behind you.",
            "Aim for one long line from the hands to the lifted heel.",
            "Keep the hips level with the lifted toes pointing down.",
            "Return to standing and switch sides."
        ),
        commonMistakes = listOf("Opening the lifted hip toward the ceiling.", "Locking the standing knee."),
        safety = "Balance: keep the fingertips on a chair or blocks for support, and lower the leg if you lose the line.",
        modifications = listOf("Hands on a chair back.", "Lift the leg only partway.")
    ),
    AsanaCodexItem(
        id = "half_moon", number = 21, sanskritName = "Ardha Chandrasana", englishName = "Half Moon Pose",
        category = AsanaCategory.BALANCE, everydayBenefit = "Steadies the sides of the hips and sharpens body awareness",
        difficulty = Difficulty.INTERMEDIATE, recommendedHoldSec = 15, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(rootRoll = 90.0f, lShoulderAbd = 90.0f, rShoulderAbd = 90.0f, lHipAbd = 90.0f, lKneeFlex = 5.0f),
        callouts = listOf(
            JointCallout("Standing knee", J.L_KNEE, 175, 7), JointCallout("Lifted leg from vertical", J.R_LEG_FROM_VERTICAL, 90, 10),
            JointCallout("Arm stack", J.ARM_LINE, 180, 10)
        ),
        steps = listOf(
            "From Triangle, bend the front knee and place the front hand on a block ahead of the foot.",
            "Shift the weight onto the front leg and lift the back leg to hip height.",
            "Straighten the standing leg and stack the top hip over the bottom one.",
            "Reach the top arm toward the ceiling.",
            "Come down with control and switch sides."
        ),
        commonMistakes = listOf("Locking the standing knee.", "Rolling the chest toward the floor."),
        safety = "Balance: use a block or chair under the bottom hand and practise near a wall.",
        modifications = listOf("Back against a wall.", "Top hand on the hip.")
    ),
    AsanaCodexItem(
        id = "dancer_pose", number = 22, sanskritName = "Natarajasana", englishName = "Lord of the Dance",
        category = AsanaCategory.BALANCE, everydayBenefit = "Stretches the front of the thighs and builds poised focus",
        difficulty = Difficulty.INTERMEDIATE, recommendedHoldSec = 20, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.STANDING,
        // The back hand reaches the lifted foot (within ~0.17 T of the ankle).
        peak = PoseAngles(trunkPitch = 30.0f, lShoulderFlex = 110.0f, rShoulderFlex = -45.7f, rShoulderAbd = -8.1f, lKneeFlex = 5.0f,
            rHipFlex = -75.2f, rKneeFlex = 115.0f),
        callouts = listOf(
            JointCallout("Standing knee", J.L_KNEE, 175, 7), JointCallout("Lifted knee", J.R_KNEE, 65, 10),
            JointCallout("Front arm", J.L_SHOULDER, 110, 10), JointCallout("Trunk lean", J.TRUNK_TILT, 30, 8)
        ),
        steps = listOf(
            "Stand on one leg and bend the other knee, holding that foot or ankle behind you.",
            "Reach the free arm forward and up.",
            "Press the foot into the hand and let the chest tip forward a little.",
            "Keep the knees close and the hips level.",
            "Release slowly and switch sides."
        ),
        commonMistakes = listOf("Twisting the lifted knee out to the side.", "Collapsing into the lower back."),
        safety = "Balance and knees: hold a wall or chair, and use a strap around the foot if reaching it strains the knee.",
        modifications = listOf("Hold a strap around the lifted foot.", "Stay upright without tipping forward.")
    ),
    AsanaCodexItem(
        id = "side_plank", number = 23, sanskritName = "Vasisthasana", englishName = "Side Plank",
        category = AsanaCategory.BALANCE, everydayBenefit = "Strengthens the sides of the trunk and the shoulders",
        difficulty = Difficulty.INTERMEDIATE, recommendedHoldSec = 15, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = HIGH_PLANK, // entered from High Plank
        peak = PoseAngles(rootRoll = 66.2f, lShoulderAbd = 90.0f, rShoulderAbd = 90.0f),
        callouts = listOf(
            JointCallout("Side body line", J.BODY_LINE, 180, 8), JointCallout("Supporting arm", J.L_SHOULDER, 90, 8),
            JointCallout("Top arm line", J.ARM_LINE, 180, 8)
        ),
        steps = listOf(
            "From High Plank, shift onto one hand and the outer edge of that foot.",
            "Stack the feet, or stagger them for balance.",
            "Lift the hips so the body is one straight line.",
            "Reach the top arm toward the ceiling.",
            "Come down and switch sides."
        ),
        commonMistakes = listOf("Hips sagging toward the floor.", "Supporting hand placed too far from the shoulder."),
        safety = "Wrists and shoulders: keep the supporting hand under the shoulder, and lower the bottom knee or use the forearm if needed.",
        modifications = listOf("Bottom knee down.", "Forearm side plank.")
    )
)
