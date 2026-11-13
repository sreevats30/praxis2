package com.companion.yogawellness.codex

import com.companion.yogawellness.codex.CalloutJoint as J

internal val DESK_POSES = listOf(
    AsanaCodexItem(
        id = "cat_cow", number = 1, sanskritName = "Marjaryasana-Bitilasana", englishName = "Cat-Cow Flow",
        category = AsanaCategory.DESK, everydayBenefit = "Loosens a stiff desk spine and neck",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        // A flow, not a hold: `start` is Cat (head tucked, neck 155°) and `peak` is Cow (head lifted, 205°).
        // The rig has one rigid spine segment, so the head-to-spine angle stands in for the spinal wave.
        start = PoseAngles.TABLETOP.copy(neckPitch = 25f),
        peak = PoseAngles(rootPitch = 82.6f, neckPitch = -25.0f, lShoulderFlex = 90.0f, lElbowFlex = 5.0f, rShoulderFlex = 90.0f, rElbowFlex = 5.0f,
            lHipFlex = 87.6f, lKneeFlex = 92.0f, rHipFlex = 87.6f, rKneeFlex = 92.0f, lAnkleFlex = 90.0f, rAnkleFlex = 90.0f),
        callouts = listOf(
            JointCallout("Spine (Cow)", J.NECK, 205, 10), JointCallout("Elbows", J.L_ELBOW, 175, 5),
            JointCallout("Hips over knees", J.L_HIP, 90, 5), JointCallout("Knees", J.L_KNEE, 90, 5)
        ),
        steps = listOf(
            "Come to hands and knees, wrists under shoulders and knees under hips.",
            "Inhale: let the belly soften down and lift your gaze for Cow.",
            "Exhale: round your back, tuck your chin and press the floor away for Cat.",
            "Move slowly with your breath for several rounds."
        ),
        commonMistakes = listOf("Rushing the movement instead of following the breath.", "Locking the elbows or sinking into the shoulders."),
        safety = "Wrists: spread your fingers and press through the whole hand; make fists or use forearms if your wrists complain.",
        modifications = listOf("Pad the knees with a folded blanket.", "Do the same spinal wave seated on a chair.")
    ),
    AsanaCodexItem(
        id = "seated_twist", number = 2, sanskritName = "Bharadvajasana", englishName = "Seated Spinal Twist",
        category = AsanaCategory.DESK, everydayBenefit = "Eases a tight lower back and sides after sitting",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 6), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = PoseAngles(lHipFlex = 133f, rHipFlex = 133f, lKneeFlex = 90f, rKneeFlex = 90f),
        peak = PoseAngles(trunkTwist = 45.0f, lShoulderFlex = 40.0f, lShoulderAbd = -30.0f, lElbowFlex = 30.0f, rShoulderFlex = -30.0f,
            rShoulderAbd = 20.0f, lHipFlex = 133.0f, lKneeFlex = 90.0f, rHipFlex = 133.0f, rKneeFlex = 90.0f),
        callouts = listOf(
            JointCallout("Torso twist", J.TRUNK_TWIST, 45, 8), JointCallout("Spine vertical", J.TRUNK_TILT, 0, 5),
            JointCallout("Knees", J.L_KNEE, 90, 10)
        ),
        steps = listOf(
            "Sit tall with knees bent and feet on the floor.",
            "Inhale and lengthen up through the crown of your head.",
            "Exhale and turn your chest to one side, one hand on the opposite knee, the other behind you.",
            "Keep both sit bones grounded; repeat on the other side."
        ),
        commonMistakes = listOf("Leading the twist with the neck instead of the ribs.", "Slumping the lower back while turning."),
        safety = "Twist only as far as is comfortable; skip or modify deep twists if pregnant.",
        modifications = listOf("Sit on a folded blanket.", "Do it sitting sideways on a chair, holding the backrest.")
    ),
    AsanaCodexItem(
        id = "cow_face_arms", number = 3, sanskritName = "Gomukhasana Arms", englishName = "Cow Face Shoulder Opener",
        category = AsanaCategory.DESK, everydayBenefit = "Opens shoulders rounded by laptop work",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 20, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(lShoulderFlex = -40.0f, lElbowFlex = 120.0f, rShoulderFlex = 175.0f, rElbowFlex = 145.0f, lKneeFlex = 2.0f, rKneeFlex = 2.0f),
        callouts = listOf(
            JointCallout("Top elbow", J.R_ELBOW, 35, 10), JointCallout("Top arm elevation", J.R_SHOULDER, 175, 8),
            JointCallout("Spine vertical", J.TRUNK_TILT, 0, 5)
        ),
        steps = listOf(
            "Stand or sit tall.",
            "Reach one arm up, bend the elbow and let that hand drop behind your head.",
            "Bring the other arm behind your back and walk that hand up toward the first.",
            "Hold a strap or towel if the hands do not meet; switch sides."
        ),
        commonMistakes = listOf("Arching the lower back to fake the reach.", "Pushing the head forward against the top arm."),
        safety = "Shoulders: stop before any pinching at the front of the shoulder; use a strap instead of forcing the hands together.",
        modifications = listOf("Hold a strap or towel between the hands.")
    ),
    AsanaCodexItem(
        id = "eagle_pose", number = 4, sanskritName = "Garudasana", englishName = "Eagle Pose",
        category = AsanaCategory.DESK, everydayBenefit = "Opens the upper back, shoulder blades and outer hips",
        difficulty = Difficulty.INTERMEDIATE, recommendedHoldSec = 20, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(lShoulderFlex = 90.0f, lShoulderAbd = 25.0f, lElbowFlex = 90.0f, rShoulderFlex = 90.0f, rShoulderAbd = 25.0f,
            rElbowFlex = 90.0f, lHipFlex = 45.0f, lKneeFlex = 45.0f, rHipFlex = 58.0f, rHipAbd = -27.4f, rHipRot = -17.1f, rKneeFlex = 76.9f),
        callouts = listOf(
            JointCallout("Standing knee", J.L_KNEE, 135, 10), JointCallout("Left elbow", J.L_ELBOW, 90, 10),
            JointCallout("Right elbow", J.R_ELBOW, 90, 10), JointCallout("Torso upright", J.TRUNK_TILT, 0, 5)
        ),
        steps = listOf(
            "Bend both knees slightly and cross one thigh over the other.",
            "Hook the top foot behind the standing calf if it is available.",
            "Bring the arms forward, bend the elbows and wrap the forearms.",
            "Lift the elbows to shoulder height and sit back a little; switch sides."
        ),
        commonMistakes = listOf("Leaning the torso forward to keep balance.", "Letting the elbows drop toward the chest."),
        safety = "Balance: stand near a wall or chair for support, and rest the top toes on the floor if needed.",
        modifications = listOf("Rest the top foot's toes on the floor like a kickstand.", "Hug the shoulders instead of wrapping the arms.")
    ),
    AsanaCodexItem(
        id = "puppy_pose", number = 5, sanskritName = "Uttana Shishosana", englishName = "Extended Puppy Pose",
        category = AsanaCategory.DESK, everydayBenefit = "Softens tension across the upper back and chest",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 6), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.TABLETOP,
        // Rig note: "Spine dip 145°" is shown as the arm-to-torso angle as the chest sinks toward the floor;
        // "Shoulder flexion 175°" is represented by long, straight arms (elbows 175°).
        peak = PoseAngles(trunkPitch = 135.1f, neckPitch = -21.3f, lShoulderFlex = 215.0f, lElbowFlex = 5.0f, rShoulderFlex = 215.0f,
            rElbowFlex = 5.0f, lKneeFlex = 90.0f, rKneeFlex = 90.0f, lAnkleFlex = 90.0f, rAnkleFlex = 90.0f),
        callouts = listOf(
            JointCallout("Hips over knees", J.L_KNEE, 90, 5), JointCallout("Chest dip (arms to torso)", J.L_SHOULDER, 145, 10),
            JointCallout("Arms long", J.L_ELBOW, 175, 5)
        ),
        steps = listOf(
            "Start on hands and knees with hips over knees.",
            "Walk the hands forward, keeping the hips stacked over the knees.",
            "Let the chest melt toward the floor; rest the forehead down.",
            "Breathe into the upper back, then walk the hands back in."
        ),
        commonMistakes = listOf("Letting the hips drift back toward the heels.", "Collapsing into the lower back instead of the upper back."),
        safety = "Shoulders and wrists: keep a small bend in the elbows if the shoulders pinch; skip if you have a shoulder injury.",
        modifications = listOf("Rest the forehead on a block.", "Place the forearms down instead of the hands.")
    ),
    AsanaCodexItem(
        id = "gentle_cobra", number = 6, sanskritName = "Bhujangasana", englishName = "Gentle Cobra",
        category = AsanaCategory.DESK, everydayBenefit = "Counters a hunched sitting posture",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 15, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.PRONE,
        peak = PoseAngles(rootPitch = 90.0f, trunkPitch = -38.0f, lShoulderFlex = 90.4f, lElbowFlex = 30.0f, rShoulderFlex = 90.4f,
            rElbowFlex = 30.0f, lKneeFlex = 5.0f, rKneeFlex = 5.0f, lAnkleFlex = 80.0f, rAnkleFlex = 80.0f),
        callouts = listOf(
            JointCallout("Spinal extension", J.L_HIP, 142, 8), JointCallout("Elbows soft", J.L_ELBOW, 150, 10),
            JointCallout("Legs long", J.L_KNEE, 175, 5)
        ),
        steps = listOf(
            "Lie on your belly with hands under the shoulders and legs long.",
            "Press the tops of the feet down and draw the elbows in.",
            "Inhale and lift the chest only as high as is easy, keeping the elbows soft.",
            "Exhale and lower down slowly."
        ),
        commonMistakes = listOf("Pushing up with straight, locked arms.", "Craning the neck back to look up."),
        safety = "Neck and lower back: keep the gaze forward and the lift small; skip or modify if pregnant (lying on the belly).",
        modifications = listOf("Stay low in Sphinx on the forearms.")
    )
)
