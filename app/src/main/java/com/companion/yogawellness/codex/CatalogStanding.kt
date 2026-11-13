package com.companion.yogawellness.codex

import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.codex.CalloutJoint as J

/** The High Plank shape; also the entry shape for Side Plank. */
internal val HIGH_PLANK = PoseAngles(rootPitch = 73.3f, lShoulderFlex = 75.0f, lElbowFlex = 4.0f, rShoulderFlex = 75.0f, rElbowFlex = 4.0f)

internal val STANDING_POSES = listOf(
    AsanaCodexItem(
        id = "warrior_1", number = 13, sanskritName = "Virabhadrasana I", englishName = "Warrior I",
        category = AsanaCategory.STANDING, everydayBenefit = "Builds leg stamina and opens the chest",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.DEG45,
        liveAsana = null,
        start = PoseAngles.STANDING,
        // Back heel down: the back foot is flat (ankle dorsiflexed) with both feet on the floor.
        peak = PoseAngles(lShoulderFlex = 170.0f, rShoulderFlex = 170.0f, lHipFlex = 85.0f, lKneeFlex = 85.0f, rHipFlex = -55.6f, rKneeFlex = 5.0f,
            rAnkleFlex = -60.6f),
        callouts = listOf(
            JointCallout("Front knee", J.L_KNEE, 95, 8), JointCallout("Back knee", J.R_KNEE, 175, 7),
            JointCallout("Arms overhead", J.L_SHOULDER, 170, 8), JointCallout("Torso upright", J.TRUNK_TILT, 0, 6)
        ),
        steps = listOf(
            "Step one foot back about a leg's length and turn the back foot out slightly.",
            "Bend the front knee over the front ankle.",
            "Square the hips toward the front as far as is comfortable.",
            "Inhale and raise the arms overhead.",
            "Switch sides."
        ),
        commonMistakes = listOf(
            "Front knee drifting inward or past the toes.", "Leaning the torso forward over the front leg.",
            "Collapsing the arch of the back foot."
        ),
        safety = "Knees: keep the front knee tracking over the second toe, and shorten the stance if the knee or hips strain.",
        modifications = listOf("Hands on the hips instead of overhead.")
    ),
    AsanaCodexItem(
        id = "warrior_2", number = 14, sanskritName = "Virabhadrasana II", englishName = "Warrior II",
        category = AsanaCategory.STANDING, everydayBenefit = "Steady hips, strong legs and focus",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.FRONT,
        liveAsana = Asana.VIRABHADRASANA_II,
        start = PoseAngles.STANDING,
        peak = PoseAngles(lShoulderAbd = 90.0f, rShoulderAbd = 90.0f, lHipAbd = 85.0f, lHipRot = 90.0f, lKneeFlex = 85.0f, rHipAbd = 58.5f, rKneeFlex = 5.0f),
        // Labels and targets equal AsanaSpecs.VIRABHADRASANA_II (LiveSpecConsistencyTest).
        callouts = listOf(
            JointCallout("Front Knee", J.L_KNEE, 95, 8), JointCallout("Back Knee", J.R_KNEE, 175, 7),
            JointCallout("Front Arm Level", J.L_SHOULDER, 90, 7), JointCallout("Back Arm Level", J.R_SHOULDER, 90, 7),
            JointCallout("Torso Verticality", J.TRUNK_TILT, 0, 6)
        ),
        steps = listOf(
            "Stand with the feet wide, the front foot pointing forward and the back foot turned slightly in.",
            "Bend the front knee until it is over the front ankle.",
            "Extend the arms level with the shoulders, palms down.",
            "Keep the torso upright over the hips and gaze over the front hand.",
            "Switch sides."
        ),
        commonMistakes = listOf(
            "Front knee overshooting the ankle.", "Leaning the torso toward the front leg.", "Letting the arms droop below shoulder height."
        ),
        safety = "Knees: keep the front knee over the ankle and never past it (the live coach checks this).",
        modifications = listOf("Bend the front knee less.", "Rest the hands on the hips.")
    ),
    AsanaCodexItem(
        id = "chair_pose", number = 15, sanskritName = "Utkatasana", englishName = "Chair Pose",
        category = AsanaCategory.STANDING, everydayBenefit = "Strengthens the glutes, thighs and back muscles",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 20, breath = BreathPattern(4, 0, 4), defaultView = CodexView.DEG45,
        liveAsana = Asana.UTKATASANA,
        start = PoseAngles.STANDING,
        peak = PoseAngles(trunkPitch = 28.0f, lShoulderFlex = 160.0f, rShoulderFlex = 160.0f, lHipFlex = 44.0f, lKneeFlex = 68.0f,
            rHipFlex = 44.0f, rKneeFlex = 68.0f, lAnkleFlex = -24.0f, rAnkleFlex = -24.0f),
        // Labels and targets equal AsanaSpecs.UTKATASANA (LiveSpecConsistencyTest).
        callouts = listOf(
            JointCallout("Left Knee Flexion", J.L_KNEE, 112, 10), JointCallout("Right Knee Flexion", J.R_KNEE, 112, 10),
            JointCallout("Hip Flexion", J.L_HIP, 108, 12), JointCallout("Trunk Forward Lean", J.TRUNK_TILT, 28, 10),
            JointCallout("Arm Elevation", J.L_SHOULDER, 160, 12)
        ),
        steps = listOf(
            "Stand with the feet together or hip-width apart.",
            "Inhale and raise the arms alongside the ears.",
            "Exhale, bend the knees and sit the hips back as if into a chair.",
            "Keep the weight in the heels and the chest lifted.",
            "Hold for a few breaths, then straighten up."
        ),
        commonMistakes = listOf("Knees moving far past the toes.", "Rounding or over-arching the lower back.", "Lifting the heels."),
        safety = "Knees and lower back: keep the hold short (the live coach caps it at 30 s) and stop if the back or knees ache.",
        modifications = listOf("Hands together at the chest.", "Sit less deep.")
    ),
    AsanaCodexItem(
        id = "high_lunge", number = 16, sanskritName = "Ashta Chandrasana", englishName = "High Crescent Lunge",
        category = AsanaCategory.STANDING, everydayBenefit = "Builds dynamic leg strength and active balance",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 30, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.STANDING,
        peak = PoseAngles(lShoulderFlex = 172.0f, rShoulderFlex = 172.0f, lHipFlex = 85.0f, lKneeFlex = 85.0f, rHipFlex = -65.4f,
            rKneeFlex = 8.0f, rAnkleFlex = 30.0f),
        callouts = listOf(
            JointCallout("Front knee", J.L_KNEE, 95, 8), JointCallout("Back knee", J.R_KNEE, 172, 7),
            JointCallout("Arms overhead", J.L_SHOULDER, 172, 8)
        ),
        steps = listOf(
            "From standing, step one foot far back onto the ball of the foot.",
            "Bend the front knee over the ankle and keep the back leg long.",
            "Lift the torso upright and reach the arms overhead.",
            "Press back through the back heel.",
            "Switch sides."
        ),
        commonMistakes = listOf("Back knee sagging toward the floor.", "Front knee drifting inward."),
        safety = "Knees and balance: keep the front knee over the ankle, and lower the back knee or hold a chair if you wobble.",
        modifications = listOf("Lower the back knee to the floor (Low Lunge).")
    ),
    AsanaCodexItem(
        id = "plank_pose", number = 17, sanskritName = "Phalakasana", englishName = "High Plank",
        category = AsanaCategory.STANDING, everydayBenefit = "Builds core and shoulder stability",
        difficulty = Difficulty.BEGINNER, recommendedHoldSec = 20, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.TABLETOP,
        peak = HIGH_PLANK,
        callouts = listOf(
            JointCallout("Shoulder-hip-ankle line", J.BODY_LINE, 180, 8), JointCallout("Elbows", J.L_ELBOW, 176, 5),
            JointCallout("Shoulder angle", J.L_SHOULDER, 75, 8)
        ),
        steps = listOf(
            "From hands and knees, place the hands under the shoulders.",
            "Step the feet back one at a time until the body is one long line.",
            "Press the floor away and draw the belly in.",
            "Keep the neck long, gazing slightly ahead of the hands."
        ),
        commonMistakes = listOf("Letting the hips sag toward the floor.", "Piking the hips too high.", "Locking the elbows hard."),
        safety = "Wrists: spread the fingers and press through the whole hand; drop the knees or use the forearms if the wrists complain.",
        modifications = listOf("Lower the knees to the floor.", "Forearm plank.")
    ),
    AsanaCodexItem(
        id = "boat_pose", number = 18, sanskritName = "Navasana", englishName = "Boat Pose",
        category = AsanaCategory.STANDING, everydayBenefit = "Strengthens the belly and the hip flexors",
        difficulty = Difficulty.INTERMEDIATE, recommendedHoldSec = 15, breath = BreathPattern(4, 0, 4), defaultView = CodexView.SIDE,
        liveAsana = null,
        start = PoseAngles.SEATED,
        peak = PoseAngles(rootPitch = -45.0f, lShoulderFlex = 45.0f, rShoulderFlex = 45.0f, lHipFlex = 105.0f, lKneeFlex = 10.0f,
            rHipFlex = 105.0f, rKneeFlex = 10.0f),
        callouts = listOf(
            JointCallout("V-sit hip angle", J.L_HIP, 75, 8), JointCallout("Knees", J.L_KNEE, 170, 8),
            JointCallout("Spine lean", J.TRUNK_TILT, 45, 8)
        ),
        steps = listOf(
            "Sit with the knees bent and the feet on the floor, hands behind the thighs.",
            "Lean back slightly with a long spine and lift the feet.",
            "Straighten the legs as far as is comfortable while keeping the chest lifted.",
            "Reach the arms forward alongside the legs.",
            "Lower the feet to rest."
        ),
        commonMistakes = listOf("Rounding the lower back and collapsing the chest.", "Holding the breath."),
        safety = "Lower back: keep the knees bent (shins level) if the back strains; skip or modify if pregnant.",
        modifications = listOf("Keep the knees bent with the shins level.", "Keep the toes on the floor.")
    )
)
