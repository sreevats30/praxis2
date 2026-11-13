package com.companion.yogawellness.wellness

import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.PoseRecordEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

enum class Persona(
    val label: String,
    val description: String,
    /** The [ProfileIds] dataset this persona is stored under — never MY_ACCOUNT. */
    val profileId: String,
    /** Short chip label. */
    val shortLabel: String
) {
    A_CONSISTENT(
        "Persona A · Consistent Improvement", "Practises most days; alignment improves and self-rated stress drifts down.",
        ProfileIds.PERSONA_A, "Demo A"
    ),
    B_EXAM_GAP(
        "Persona B · Exam Gap", "Regular practice, a two-week exam gap with little practice and high stress, then a return.",
        ProfileIds.PERSONA_B, "Demo B"
    ),
    C_NULL(
        "Persona C · Null Pattern", "Practice and stress vary independently — the dashboard should report no pattern.",
        ProfileIds.PERSONA_C, "Demo C"
    );

    companion object {
        fun forProfileId(profileId: String): Persona? = entries.firstOrNull { it.profileId == profileId }
    }
}

data class SessionWithPoses(val session: PracticeSessionEntity, val poses: List<PoseRecordEntity>)

data class PersonaData(
    val persona: Persona,
    val checkIns: List<DailyCheckInEntity>,
    val sessions: List<SessionWithPoses>,
    val questionnaires: List<QuestionnaireEntity>
)

/**
 * Seeds 42 days of realistic demo data (§10 Prompt 4) so the dashboard and statistics can be shown
 * on stage without weeks of real logging. Deterministic for a given seed.
 */
object MockPersonaSeeder {
    const val DAYS = 42

    fun generate(persona: Persona, todayEpochDay: Long, seed: Int = 42): PersonaData {
        val rnd = Random(seed * 31 + persona.ordinal)
        val first = todayEpochDay - (DAYS - 1)
        val checkIns = ArrayList<DailyCheckInEntity>()
        val sessions = ArrayList<SessionWithPoses>()

        for (i in 0 until DAYS) {
            val day = first + i
            val p = i / (DAYS - 1.0)
            val plan = when (persona) {
                Persona.A_CONSISTENT -> {
                    val practiced = rnd.nextDouble() < 0.85
                    val minutes = if (practiced) 12 + 18 * p + rnd.gauss(2.5) else 0.0
                    DayPlan(
                        practiced, minutes,
                        quality = 62 + 25 * p + rnd.gauss(4.0),
                        stress = 7.5 - 4.0 * p - 0.06 * (minutes - 15) + rnd.gauss(0.8),
                        mood = 4.5 + 3.5 * p + rnd.gauss(0.8),
                        sleep = 5.0 + 2.5 * p + rnd.gauss(0.8),
                        rightHoldRatio = 0.78 + 0.17 * p
                    )
                }

                Persona.B_EXAM_GAP -> {
                    val exam = i in 16..29
                    val practiced = rnd.nextDouble() < if (exam) 0.15 else 0.85
                    val minutes = if (!practiced) 0.0 else if (exam) 6 + rnd.gauss(1.5) else 20 + rnd.gauss(3.0)
                    DayPlan(
                        practiced, minutes,
                        quality = (if (exam) 64.0 else 76.0) + rnd.gauss(4.0),
                        stress = (if (exam) 8.2 else 4.4) + rnd.gauss(0.8),
                        mood = (if (exam) 4.0 else 6.5) + rnd.gauss(0.8),
                        sleep = (if (exam) 3.8 else 6.8) + rnd.gauss(0.8),
                        rightHoldRatio = 0.85
                    )
                }

                Persona.C_NULL -> {
                    val practiced = rnd.nextDouble() < 0.6
                    DayPlan(
                        practiced, if (practiced) rnd.nextDouble(10.0, 30.0) else 0.0,
                        quality = 72 + rnd.gauss(6.0),
                        stress = rnd.nextDouble(2.5, 7.5),
                        mood = rnd.nextDouble(3.5, 7.5),
                        sleep = rnd.nextDouble(4.0, 8.0),
                        rightHoldRatio = 0.95
                    )
                }
            }

            val stress = plan.stress.clamp10()
            if (rnd.nextDouble() < 0.95) {
                val pre = if (plan.practiced) (stress + rnd.nextInt(-1, 2)).coerceIn(1, 10) else null
                checkIns += DailyCheckInEntity(
                    epochDay = day,
                    mood = plan.mood.clamp10(),
                    stress = stress,
                    sleepQuality = plan.sleep.clamp10(),
                    preTension = pre,
                    postTension = pre?.let { (it - 2 - rnd.nextInt(0, 2)).coerceIn(1, 10) },
                    updatedAtMs = day * DAY_MS + 21 * HOUR_MS,
                    profileId = persona.profileId
                )
            }
            if (plan.practiced && plan.minutes > 1.0) {
                sessions += session(day, plan, rnd, persona.profileId)
            }
        }

        val (phq, gad) = when (persona) {
            Persona.A_CONSISTENT -> listOf(12, 8, 5) to listOf(10, 7, 4)
            Persona.B_EXAM_GAP -> listOf(6, 7, 13) to listOf(5, 6, 12)
            Persona.C_NULL -> listOf(7, 6, 7) to listOf(6, 7, 6)
        }
        val questionnaires = listOf(0, 14, 28).flatMapIndexed { k, offset ->
            val day = first + offset
            listOf(
                questionnaire(QuestionnaireType.PHQ9, day, phq[k], rnd, persona.profileId),
                questionnaire(QuestionnaireType.GAD7, day, gad[k], rnd, persona.profileId)
            )
        }
        return PersonaData(persona, checkIns, sessions, questionnaires)
    }

    private data class DayPlan(
        val practiced: Boolean,
        val minutes: Double,
        val quality: Double,
        val stress: Double,
        val mood: Double,
        val sleep: Double,
        /** Right-side hold as a fraction of the left-side hold (inter-pose symmetry). */
        val rightHoldRatio: Double
    )

    private fun session(day: Long, plan: DayPlan, rnd: Random, profileId: String): SessionWithPoses {
        val activeSec = plan.minutes * 60.0
        val quality = plan.quality.coerceIn(30.0, 99.0)
        val start = day * DAY_MS + 7 * HOUR_MS
        val labels = listOf(
            Asana.VRIKSHASANA to PoseSide.LEFT_LIFTED, Asana.VRIKSHASANA to PoseSide.RIGHT_LIFTED,
            Asana.VIRABHADRASANA_II to PoseSide.FRONT_LEFT, Asana.VIRABHADRASANA_II to PoseSide.FRONT_RIGHT,
            Asana.TRIKONASANA to PoseSide.LEADING_LEFT, Asana.TRIKONASANA to PoseSide.LEADING_RIGHT,
            Asana.UTKATASANA to PoseSide.SYMMETRIC
        )
        val share = activeSec / labels.size
        val poses = labels.map { (asana, side) ->
            val leftHold = (12.0 + quality / 5.0 + rnd.gauss(2.0)).coerceAtLeast(3.0)
            val hold = when {
                asana == Asana.UTKATASANA -> leftHold.coerceAtMost(30.0)
                side.workingSide == com.companion.yogawellness.pose.BodySide.RIGHT -> leftHold * plan.rightHoldRatio
                else -> leftHold
            }
            PoseRecordEntity(
                sessionId = 0, asana = asana.name, side = side.name, lockedSec = share,
                meanScore = (quality + rnd.gauss(3.0)).coerceIn(20.0, 100.0),
                bestStrictHoldSec = hold * 0.8, bestGraceHoldSec = hold,
                meanStability = (60 + quality / 3 + rnd.gauss(4.0)).coerceIn(0.0, 100.0)
            )
        }
        val session = PracticeSessionEntity(
            epochDay = day, startedAtMs = start, endedAtMs = start + (activeSec * 1000 * 1.4).toLong(),
            activeSec = activeSec, meanScore = quality, profile = "STANDARD", profileId = profileId
        )
        return SessionWithPoses(session, poses)
    }

    /** Spreads [total] across the items (0–3 each). PHQ-9 item 9 is always 0 in demo data. */
    private fun questionnaire(type: QuestionnaireType, day: Long, total: Int, rnd: Random, profileId: String): QuestionnaireEntity {
        val n = type.items.size
        val maxItem = if (type == QuestionnaireType.PHQ9) n - 1 else n
        val answers = IntArray(n)
        var remaining = total.coerceAtMost(maxItem * 3)
        while (remaining > 0) {
            val i = rnd.nextInt(maxItem)
            if (answers[i] < 3) { answers[i]++; remaining-- }
        }
        return QuestionnaireEntity(
            type = type.name, epochDay = day, createdAtMs = day * DAY_MS + 20 * HOUR_MS,
            answers = Questionnaires.encodeAnswers(answers.toList()), totalScore = answers.sum(),
            profileId = profileId
        )
    }

    private fun Random.gauss(sd: Double): Double {
        val u1 = nextDouble().coerceAtLeast(1e-12)
        val u2 = nextDouble()
        return sd * sqrt(-2.0 * ln(u1)) * cos(2 * Math.PI * u2)
    }

    private fun Double.clamp10(): Int = roundToInt().coerceIn(1, 10)

    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L
}
