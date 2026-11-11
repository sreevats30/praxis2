package com.companion.yogawellness.wellness

/**
 * PHQ-9 and GAD-7 (Spitzer, Williams, Kroenke and colleagues; educational grant from Pfizer Inc.;
 * no permission required to reproduce, translate, display or distribute).
 *
 * Presented only as a non-diagnostic "Self-Reflection Check-In Score" (§9.3) — no severity labels.
 */
enum class QuestionnaireType(
    val shortName: String,
    val title: String,
    val items: List<String>,
    /** Scheduled once every 14 days, matching the "last 2 weeks" wording. */
    val intervalDays: Int = 14
) {
    PHQ9(
        "PHQ-9", "Mood reflection (PHQ-9)",
        listOf(
            "Little interest or pleasure in doing things",
            "Feeling down, depressed, or hopeless",
            "Trouble falling or staying asleep, or sleeping too much",
            "Feeling tired or having little energy",
            "Poor appetite or overeating",
            "Feeling bad about yourself — or that you are a failure or have let yourself or your family down",
            "Trouble concentrating on things, such as reading the newspaper or watching television",
            "Moving or speaking so slowly that other people could have noticed? Or the opposite — being so fidgety or restless that you have been moving around a lot more than usual",
            "Thoughts that you would be better off dead or of hurting yourself in some way"
        )
    ),
    GAD7(
        "GAD-7", "Worry reflection (GAD-7)",
        listOf(
            "Feeling nervous, anxious or on edge",
            "Not being able to stop or control worrying",
            "Worrying too much about different things",
            "Trouble relaxing",
            "Being so restless that it is hard to sit still",
            "Becoming easily annoyed or irritable",
            "Feeling afraid as if something awful might happen"
        )
    );

    val maxScore: Int get() = items.size * 3
}

object Questionnaires {
    const val STEM = "Over the last 2 weeks, how often have you been bothered by any of the following problems?"
    val OPTIONS = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
    const val ATTRIBUTION =
        "PHQ-9 and GAD-7 were developed by Drs. Robert L. Spitzer, Janet B.W. Williams, Kurt Kroenke and colleagues, " +
            "with an educational grant from Pfizer Inc. No permission required to reproduce, translate, display or distribute."
    const val NON_DIAGNOSTIC =
        "This is a self-reflection check-in, not a diagnosis. Scores are shown only so you can notice changes over time."

    /** Index of PHQ-9 item 9 (thoughts of self-harm). */
    const val PHQ9_SELF_HARM_ITEM = 8

    fun score(answers: List<Int?>): Int = answers.sumOf { it ?: 0 }

    fun isComplete(type: QuestionnaireType, answers: List<Int?>): Boolean =
        answers.size == type.items.size && answers.all { it != null && it in 0..3 }

    /** Item 9 >= 1 ("Several days" or more) triggers the crisis support sheet immediately (§9.3). */
    fun needsCrisisSupport(type: QuestionnaireType, answers: List<Int?>): Boolean =
        type == QuestionnaireType.PHQ9 && (answers.getOrNull(PHQ9_SELF_HARM_ITEM) ?: 0) >= 1

    fun nextDueEpochDay(type: QuestionnaireType, lastEpochDay: Long?): Long? = lastEpochDay?.plus(type.intervalDays)

    fun isDue(type: QuestionnaireType, lastEpochDay: Long?, todayEpochDay: Long): Boolean =
        lastEpochDay == null || todayEpochDay >= lastEpochDay + type.intervalDays

    fun encodeAnswers(answers: List<Int?>): String = answers.joinToString(",") { (it ?: 0).toString() }

    fun decodeAnswers(s: String): List<Int> = if (s.isBlank()) emptyList() else s.split(',').map { it.trim().toInt() }
}

/** India's national mental-health helpline and emergency number (§9.3). */
object CrisisLines {
    const val TELE_MANAS_SHORT = "14416"
    const val TELE_MANAS_TOLL_FREE = "1-800-891-4416"
    const val TELE_MANAS_TOLL_FREE_DIAL = "18008914416"
    const val EMERGENCY = "112"
}
