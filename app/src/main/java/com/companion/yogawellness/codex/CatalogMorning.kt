package com.companion.yogawellness.codex

import com.companion.yogawellness.codex.CalloutJoint as J

internal val MORNING_POSES = listOf(
    AsanaCodexItem(
        id = "mountain", number = 7, sanskritName = "Tadasana", englishName = "Mountain Pose",
        category = AsanaCategory.MORNING, everydayBenefit = "A steady, grounded baseline for posture",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        // Tadasana is the calibration pose in Practice; it is never scored, so it is not a live item.
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(lShoulderAbd = 15.0f, rShoulderAbd = 15.0f, lKneeFlex = 2.0f, rKneeFlex = 2.0f),
        callouts = listOf(
            JointCallout("Knees", J.L_KNEE, 178, 5), JointCallout("Hips", J.L_HIP, 180, 5),
            JointCallout("Spine vertical", J.TRUNK_TILT, 0, 5), JointCallout("Arms from sides", J.L_SHOULDER, 15, 5)
        ),
        steps = listOf(
            "Stand with the feet hip-width apart and the weight spread through both feet.",
            "Straighten the knees without locking them.",
            "Stack the hips over the ankles and the shoulders over the hips.",
            "Let the arms hang a little away from your sides and breathe evenly."
        ),
        commonMistakes = listOf("Locking the knees back.", "Tipping the pelvis forward and arching the lower back."),
        safety = "Balance: if you feel light-headed, widen your stance and keep your eyes open.",
        modifications = listOf("Stand with your back near a wall for feedback.")
    ),
    AsanaCodexItem(
        id = "upward_salute", number = 8, sanskritName = "Urdhva Hastasana", englishName = "Upward Salute",
        category = AsanaCategory.MORNING, everydayBenefit = "A full-body morning wake-up that lets the ribs expand",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 20, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(trunkPitch = -10.0f, lShoulderFlex = 175.0f, lElbowFlex = 5.0f, rShoulderFlex = 175.0f, rElbowFlex = 5.0f),
        callouts = listOf(
            JointCallout("Arms overhead", J.L_SHOULDER, 175, 5), JointCallout("Elbows", J.L_ELBOW, 175, 5),
            JointCallout("Upper-back lift", J.TRUNK_TILT, 10, 5)
        ),
        steps = listOf(
            "Start in Mountain Pose.",
            "Inhale and sweep both arms out and up overhead.",
            "Reach through the fingertips with soft elbows and relaxed shoulders.",
            "Lift the chest gently, then exhale and lower the arms."
        ),
        commonMistakes = listOf("Shrugging the shoulders up to the ears.", "Leaning back from the lower back instead of lifting the chest."),
        safety = "Shoulders and lower back: keep the backbend small, and lower the arms to shoulder height if the shoulders pinch.",
        modifications = listOf("Keep the hands wider than shoulder-width apart.")
    ),
    AsanaCodexItem(
        id = "standing_forward_fold", number = 9, sanskritName = "Uttanasana", englishName = "Standing Forward Fold",
        category = AsanaCategory.MORNING, everydayBenefit = "Lengthens the back and wakes up the hamstrings",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 6), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(trunkPitch = 125.0f, lShoulderFlex = 125.0f, rShoulderFlex = 125.0f, lHipFlex = 10.0f, lKneeFlex = 10.0f,
            rHipFlex = 10.0f, rKneeFlex = 10.0f),
        callouts = listOf(
            JointCallout("Hip hinge", J.L_HIP, 45, 8), JointCallout("Knees (micro-bend)", J.L_KNEE, 170, 5),
            JointCallout("Spine hanging", J.NECK, 180, 5)
        ),
        steps = listOf(
            "From Mountain Pose, place the hands on the hips.",
            "Exhale and hinge forward from the hips with a long spine.",
            "Bend the knees as much as you need and let the head hang.",
            "Rest the hands on the shins, blocks or the floor.",
            "To come up, bend the knees and roll up slowly."
        ),
        commonMistakes = listOf("Rounding from the lower back with locked knees.", "Coming up quickly, head first."),
        safety = "Lower back and head below heart: keep the knees bent, come up slowly, and skip the pose if you feel dizzy.",
        modifications = listOf("Rest the hands on blocks or a chair seat.")
    ),
    AsanaCodexItem(
        id = "halfway_lift", number = 10, sanskritName = "Ardha Uttanasana", englishName = "Halfway Lift",
        category = AsanaCategory.MORNING, everydayBenefit = "Practises a flat back and an active core",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 15, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(trunkPitch = 90.0f, lShoulderFlex = 70.0f, rShoulderFlex = 70.0f, lKneeFlex = 5.0f, rKneeFlex = 5.0f),
        callouts = listOf(
            JointCallout("Hip angle", J.L_HIP, 90, 8), JointCallout("Trunk horizontal", J.TRUNK_TILT, 90, 8),
            JointCallout("Knees", J.L_KNEE, 175, 5)
        ),
        steps = listOf(
            "From a forward fold, place the hands on the shins.",
            "Inhale and lengthen the spine forward until the back is flat.",
            "Draw the shoulders away from the ears and look a little ahead.",
            "Exhale and fold back down, or rise to standing."
        ),
        commonMistakes = listOf("Rounding the upper back.", "Craning the neck to look forward."),
        safety = "Lower back: bend the knees and keep the hands on the thighs if the back strains.",
        modifications = listOf("Place the hands on a wall or chair back at hip height.")
    ),
    AsanaCodexItem(
        id = "low_lunge", number = 11, sanskritName = "Ashwa Sanchalanasana", englishName = "Crescent Low Lunge",
        category = AsanaCategory.MORNING, everydayBenefit = "Opens the front of the hips that tighten from sitting",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(lShoulderFlex = 170.0f, rShoulderFlex = 170.0f, lHipFlex = 96.6f, lKneeFlex = 90.0f, rHipFlex = -35.0f,
            rKneeFlex = 57.7f, rAnkleFlex = 80.0f),
        callouts = listOf(
            JointCallout("Front knee", J.L_KNEE, 90, 8), JointCallout("Back hip opening", J.R_HIP, 145, 8),
            JointCallout("Arms overhead", J.L_SHOULDER, 170, 8)
        ),
        steps = listOf(
            "Step one foot forward and lower the back knee to the floor.",
            "Stack the front knee over the front ankle.",
            "Shift the hips gently forward until the front of the back hip opens.",
            "Inhale and sweep the arms overhead.",
            "Switch sides."
        ),
        commonMistakes = listOf("Letting the front knee move past the toes.", "Arching the lower back instead of lengthening."),
        safety = "Knees: pad the back knee with a blanket and keep the front knee over the ankle.",
        modifications = listOf("Keep the hands on the front thigh or on blocks.")
    ),
    AsanaCodexItem(
        id = "downward_dog", number = 12, sanskritName = "Adho Mukha Svanasana", englishName = "Downward-Facing Dog",
        category = AsanaCategory.MORNING, everydayBenefit = "Stretches the whole back of the body and steadies the shoulders",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 6), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.TABLETOP,
        peak = PoseAngles(rootPitch = 135.8f, lShoulderFlex = 175.0f, rShoulderFlex = 175.0f, lHipFlex = 105.0f, lKneeFlex = 10.0f,
            rHipFlex = 105.0f, rKneeFlex = 10.0f),
        callouts = listOf(
            JointCallout("Hip apex", J.L_HIP, 75, 8), JointCallout("Shoulder line", J.L_SHOULDER, 175, 8),
            JointCallout("Knees", J.L_KNEE, 170, 8)
        ),
        steps = listOf(
            "Start on hands and knees with the hands slightly ahead of the shoulders.",
            "Tuck the toes and lift the hips up and back.",
            "Press the floor away and lengthen the spine.",
            "Bend the knees as needed while the heels reach toward the floor.",
            "Lower the knees to rest."
        ),
        commonMistakes = listOf("Rounding the back to force straight legs.", "Sinking the chest and straining the shoulders."),
        safety = "Wrists and head below heart: spread the fingers wide, rest on the forearms if the wrists complain, and come down if you feel dizzy.",
        modifications = listOf("Bend the knees generously.", "Place the hands on a wall or chair seat.")
    )
)
