package com.companion.yogawellness.codex

import com.companion.yogawellness.codex.CalloutJoint as J

internal val EVENING_POSES = listOf(
    AsanaCodexItem(
        id = "childs_pose", number = 29, sanskritName = "Balasana", englishName = "Child's Pose",
        category = AsanaCategory.EVENING, everydayBenefit = "Helps you slow down and lets the back relax",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 45, breath = BreathPattern(4, 0, 6), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.TABLETOP,
        // Toes-tucked variation: with a rigid one-segment spine, the catalog's knee 35° / hip 40° cannot
        // also lay the shins flat, so knees, toes and forehead rest on the floor and the hands hover ~0.07 T.
        peak = PoseAngles(rootPitch = 105.3f, neckPitch = 39.2f, lShoulderFlex = 175.0f, rShoulderFlex = 175.0f, lHipFlex = 140.0f,
            lKneeFlex = 145.0f, rHipFlex = 140.0f, rKneeFlex = 145.0f, lAnkleFlex = 2.1f, rAnkleFlex = 2.1f),
        callouts = listOf(
            JointCallout("Knees folded", J.L_KNEE, 35, 8), JointCallout("Hips toward heels", J.L_HIP, 40, 8),
            JointCallout("Arms extended", J.L_SHOULDER, 175, 8)
        ),
        steps = listOf(
            "Kneel and bring the big toes together.",
            "Sit the hips back toward the heels.",
            "Fold forward and rest the forehead down.",
            "Reach the arms forward or rest them beside you.",
            "Breathe slowly into the back."
        ),
        commonMistakes = listOf("Forcing the hips onto the heels.", "Tensing the shoulders."),
        safety = "Knees: place a cushion between the hips and heels, or under the knees, if they feel strained.",
        modifications = listOf("Knees wide apart.", "Forehead on a cushion.")
    ),
    AsanaCodexItem(
        id = "seated_forward_bend", number = 30, sanskritName = "Paschimottanasana", englishName = "Seated Forward Bend",
        category = AsanaCategory.EVENING, everydayBenefit = "A calming stretch for the whole back of the body",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 45, breath = BreathPattern(4, 0, 6), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.SEATED,
        peak = PoseAngles(trunkPitch = 27.6f, lShoulderFlex = 160.0f, rShoulderFlex = 160.0f, lHipFlex = 92.4f, lKneeFlex = 5.0f,
            rHipFlex = 92.4f, rKneeFlex = 5.0f),
        callouts = listOf(
            JointCallout("Legs (knees)", J.L_KNEE, 175, 5), JointCallout("Hip hinge", J.L_HIP, 60, 8),
            JointCallout("Arms reaching", J.L_SHOULDER, 160, 8)
        ),
        steps = listOf(
            "Sit with the legs straight in front of you.",
            "Inhale, raise the arms and lengthen the spine.",
            "Exhale and hinge forward from the hips.",
            "Rest the hands on the legs or feet, keeping the spine long rather than pulling yourself down."
        ),
        commonMistakes = listOf("Rounding the lower back to reach the feet.", "Locking the knees."),
        safety = "Lower back: bend the knees and sit on a folded blanket if the back strains.",
        modifications = listOf("Strap around the feet.", "Bend the knees.")
    ),
    AsanaCodexItem(
        id = "bridge_pose", number = 31, sanskritName = "Setu Bandha Sarvangasana", englishName = "Bridge Pose",
        category = AsanaCategory.EVENING, everydayBenefit = "Wakes up the glutes and opens the chest",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.SUPINE,
        peak = PoseAngles(rootPitch = -109.0f, neckPitch = 19.0f, lShoulderFlex = -19.0f, rShoulderFlex = -19.0f, lHipFlex = 8.0f,
            lKneeFlex = 90.0f, rHipFlex = 8.0f, rKneeFlex = 90.0f, lAnkleFlex = 27.0f, rAnkleFlex = 27.0f),
        callouts = listOf(
            JointCallout("Knees", J.L_KNEE, 90, 8), JointCallout("Shoulder-hip-knee line", J.L_HIP, 172, 8)
        ),
        steps = listOf(
            "Lie on your back with the knees bent and the feet flat, hip-width apart.",
            "Press the feet down and lift the hips.",
            "Keep the knees pointing forward and the neck relaxed.",
            "Lower down slowly, one part of the spine at a time."
        ),
        commonMistakes = listOf("Turning the head while the hips are lifted.", "Letting the knees drift apart."),
        safety = "Neck: keep the head still and facing up, and never turn it while the hips are lifted.",
        modifications = listOf("Rest the sacrum on a block.")
    ),
    AsanaCodexItem(
        id = "corpse_relaxation", number = 32, sanskritName = "Savasana", englishName = "Mindful Corpse Relaxation",
        category = AsanaCategory.EVENING, everydayBenefit = "A quiet rest to settle body and mind before sleep",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 120, breath = BreathPattern(4, 0, 6),
        // 45° view: front or side alone hides either the body line or the arm/leg spread.
        defaultView = CodexView.DEG45,
        liveAsana = null,
        start = PoseAngles.SUPINE,
        peak = PoseAngles(rootPitch = -90.0f, lShoulderAbd = 30.0f, rShoulderAbd = 30.0f, lHipAbd = 10.0f, rHipAbd = 10.0f),
        callouts = listOf(
            JointCallout("Body line", J.BODY_LINE, 180, 5), JointCallout("Arms from sides", J.L_SHOULDER, 30, 6),
            JointCallout("Legs apart", J.LEG_SPREAD, 20, 6)
        ),
        steps = listOf(
            "Lie on your back with the legs a little apart.",
            "Rest the arms slightly away from the body, palms up.",
            "Close the eyes and let the breath become natural.",
            "Rest for a few minutes, then roll to one side to come up."
        ),
        commonMistakes = listOf("Holding tension in the jaw or shoulders.", "Sitting up quickly at the end."),
        safety = "Lower back: place a cushion under the knees, and lie on your side instead if lying on your back is uncomfortable (for example later in pregnancy).",
        modifications = listOf("Cushion under the knees.", "Blanket for warmth.")
    )
)
