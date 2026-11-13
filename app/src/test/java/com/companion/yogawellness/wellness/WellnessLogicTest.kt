package com.companion.yogawellness.wellness

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSegmentStats
import com.companion.yogawellness.scoring.PoseSide
import com.companion.yogawellness.stats.RankStatsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WellnessLogicTest {

    @Test
    fun questionnaireScoringAndCrisisTrigger() {
        val phq = MutableList<Int?>(9) { 0 }
        assertFalse(Questionnaires.needsCrisisSupport(QuestionnaireType.PHQ9, phq))
        phq[8] = 1 // "Several days" on item 9
        assertTrue(Questionnaires.needsCrisisSupport(QuestionnaireType.PHQ9, phq))
        assertEquals(1, Questionnaires.score(phq))
        assertFalse(Questionnaires.needsCrisisSupport(QuestionnaireType.GAD7, List(7) { 3 }))
        assertEquals(27, QuestionnaireType.PHQ9.maxScore)
        assertEquals(21, QuestionnaireType.GAD7.maxScore)
        assertFalse(Questionnaires.isComplete(QuestionnaireType.PHQ9, listOf(0, 1, null, 0, 0, 0, 0, 0, 0)))
        assertEquals(listOf(0, 1, 2, 3), Questionnaires.decodeAnswers(Questionnaires.encodeAnswers(listOf(0, 1, 2, 3))))
    }

    @Test
    fun questionnairesAreScheduledEveryFourteenDays() {
        assertTrue(Questionnaires.isDue(QuestionnaireType.PHQ9, null, 100))
        assertFalse(Questionnaires.isDue(QuestionnaireType.PHQ9, 100, 113))
        assertTrue(Questionnaires.isDue(QuestionnaireType.PHQ9, 100, 114))
        assertEquals(114L, Questionnaires.nextDueEpochDay(QuestionnaireType.GAD7, 100))
    }

    @Test
    fun dailyAggregationJoinsCheckInsAndSessions() {
        val checkIns = listOf(
            DailyCheckInEntity(epochDay = 10, mood = 6, stress = 4, sleepQuality = 7, updatedAtMs = 0),
            DailyCheckInEntity(epochDay = 12, mood = 5, stress = 6, sleepQuality = 5, updatedAtMs = 0)
        )
        val sessions = listOf(
            PracticeSessionEntity(epochDay = 10, startedAtMs = 0, endedAtMs = 0, activeSec = 600.0, meanScore = 80.0, profile = "STANDARD"),
            PracticeSessionEntity(epochDay = 10, startedAtMs = 1, endedAtMs = 1, activeSec = 300.0, meanScore = 50.0, profile = "STANDARD"),
            PracticeSessionEntity(epochDay = 11, startedAtMs = 2, endedAtMs = 2, activeSec = 120.0, meanScore = 70.0, profile = "STANDARD")
        )
        val rows = DailyAggregator.combine(checkIns, sessions, 10, 12)
        assertEquals(3, rows.size)
        assertEquals(15.0, rows[0].practiceMinutes, 1e-9)
        assertEquals(70.0, rows[0].practiceQuality!!, 1e-9) // time-weighted (80*600 + 50*300) / 900
        assertNull(rows[1].stress)
        assertEquals(0.0, rows[2].practiceMinutes, 0.0)
        val (minutes, stress) = DailyAggregator.minutesVs(rows) { it.stress }
        assertEquals(2, minutes.size) // only days with a check-in
        assertEquals(listOf(4.0, 6.0), stress.toList())
    }

    @Test
    fun sessionSummarizerSkipsTinySessionsAndWeightsScores() {
        val tree = AsanaLabel(Asana.VRIKSHASANA, PoseSide.LEFT_LIFTED)
        val warrior = AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT)
        // v1.1 rule: a pose is kept from 2.0 s locked (v1.0 dropped sessions under 5 s in total).
        assertNull(SessionSummarizer.toEntities(listOf(PoseSegmentStats(tree, 1.9, 50, 80.0, 2.0, 2.0, 90.0)), 0, 1, 0, "STANDARD"))
        val (s, poses) = SessionSummarizer.toEntities(
            listOf(PoseSegmentStats(tree, 30.0, 700, 90.0, 20.0, 25.0, 85.0), PoseSegmentStats(warrior, 10.0, 250, 50.0, 2.0, 3.0, 70.0)),
            0, 60_000, 5, "BEGINNER"
        )!!
        assertEquals(40.0, s.activeSec, 1e-9)
        assertEquals(80.0, s.meanScore!!, 1e-9)
        assertEquals(2, poses.size)
        assertEquals("LEFT_LIFTED", poses[0].side)
    }

    // ---- Personas: each must actually show the pattern it is named after -------------------

    private fun minutesVsStress(p: Persona): Pair<DoubleArray, DoubleArray> {
        val today = 20_000L
        val d = MockPersonaSeeder.generate(p, today)
        val rows = DailyAggregator.combine(d.checkIns, d.sessions.map { it.session }, today - 41, today)
        return DailyAggregator.minutesVs(rows) { it.stress }
    }

    @Test
    fun personasHaveFortyTwoDaysAndFortnightlyQuestionnaires() {
        for (p in Persona.entries) {
            val d = MockPersonaSeeder.generate(p, 20_000L)
            assertTrue(d.checkIns.all { it.epochDay in 19_959L..20_000L })
            assertTrue("${p.name} check-ins=${d.checkIns.size}", d.checkIns.size >= 36)
            assertEquals(listOf(19_959L, 19_973L, 19_987L), d.questionnaires.filter { it.type == "PHQ9" }.map { it.epochDay })
            // Demo data never contains a positive self-harm item
            d.questionnaires.filter { it.type == "PHQ9" }.forEach { assertEquals(0, Questionnaires.decodeAnswers(it.answers)[8]) }
            d.questionnaires.forEach { assertEquals(it.totalScore, Questionnaires.decodeAnswers(it.answers).sum()) }
            assertTrue(d.checkIns.all { it.mood in 1..10 && it.stress in 1..10 && it.sleepQuality in 1..10 })
        }
    }

    @Test
    fun personaAShowsSignificantNegativeAssociation() {
        val (x, y) = minutesVsStress(Persona.A_CONSISTENT)
        val r = RankStatsEngine.analyze(x, y)
        println("Persona A: " + RankStatsEngine.describe(r))
        assertTrue(r.rho!! < -0.4)
        assertTrue(r.pValue!! < 0.05)
    }

    @Test
    fun personaBExamGapShowsNegativeAssociation() {
        val (x, y) = minutesVsStress(Persona.B_EXAM_GAP)
        val r = RankStatsEngine.analyze(x, y)
        println("Persona B: " + RankStatsEngine.describe(r))
        assertTrue(r.rho!! < -0.4)
        assertTrue(r.pValue!! < 0.05)
    }

    @Test
    fun personaCShowsNoPattern() {
        val (x, y) = minutesVsStress(Persona.C_NULL)
        val r = RankStatsEngine.analyze(x, y)
        println("Persona C: " + RankStatsEngine.describe(r))
        assertTrue(r.pValue!! > 0.05)
        assertTrue(kotlin.math.abs(r.rho!!) < 0.3)
    }
}
