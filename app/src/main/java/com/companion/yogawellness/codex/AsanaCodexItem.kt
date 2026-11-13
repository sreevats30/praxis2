package com.companion.yogawellness.codex

import com.companion.yogawellness.scoring.Asana

enum class CodexView(val label: String, val yawDeg: Float) {
    FRONT("Front", 0f),
    DEG45("45°", 45f),
    SIDE("Side", 90f)
}

enum class Difficulty(val label: String) { BEGINNER("Beginner"), INTERMEDIATE("Intermediate") }

/** A target angle drawn on the skeleton; the label shows the solver-measured value. */
data class JointCallout(val label: String, val joint: CalloutJoint, val targetDeg: Int, val tolDeg: Int)

/** One Learn-tab pose (§5.5). Offline content only: no images, no video. */
data class AsanaCodexItem(
    val id: String,
    val number: Int,
    val sanskritName: String,
    val englishName: String,
    val category: AsanaCategory,
    /** "Daily-life benefit" badge: everyday feel/function, never a medical claim. */
    val everydayBenefit: String,
    val difficulty: Difficulty,
    val recommendedHoldSec: Int,
    val breath: BreathPattern,
    val defaultView: CodexView,
    /** Non-null only for the four poses the camera can score live. */
    val liveAsana: Asana?,
    val start: PoseAngles,
    val peak: PoseAngles,
    val callouts: List<JointCallout>,
    val steps: List<String>,
    val commonMistakes: List<String>,
    val safety: String,
    val modifications: List<String> = emptyList()
) {
    val isLive: Boolean get() = liveAsana != null

    /** Screen-reader description of the demo, built from the callouts. */
    fun demoDescription(): String =
        "$englishName demo. " + callouts.joinToString(", ") { "${it.label.lowercase()} ${it.targetDeg}°" } + "."
}

object CodexText {
    const val GLOBAL_DISCLAIMER =
        "Not medical advice. Skip or modify any pose that causes pain, dizziness or joint strain, and check with a " +
            "clinician if you are pregnant or have an injury or condition."
    const val FOLLOW_ALONG_NOTE = "Follow along with the demo. Live scoring is available for 4 poses."
}
