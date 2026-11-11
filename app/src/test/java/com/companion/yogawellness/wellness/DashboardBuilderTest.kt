package com.companion.yogawellness.wellness

import com.companion.yogawellness.stats.RankStatsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardBuilderTest {

    private val today = 20_000L

    fun personaDashboard(p: Persona): DashboardData {
        val d = MockPersonaSeeder.generate(p, today)
        return DashboardBuilder.build(
            d.checkIns, d.sessions.map { it.session }, d.sessions.flatMap { it.poses }, d.questionnaires, today
        )
    }

    @Test
    fun personaADashboard() {
        val dash = personaDashboard(Persona.A_CONSISTENT)
        assertEquals(42, dash.daily.size)
        assertEquals(6, dash.weeks.size)
        val first = dash.weeks.first()
        val last = dash.weeks.last()
        assertTrue("quality ${first.practiceQuality} -> ${last.practiceQuality}", last.practiceQuality!! > first.practiceQuality!! + 10)
        assertTrue("stress ${first.meanStress} -> ${last.meanStress}", last.meanStress!! < first.meanStress!! - 2)
        assertEquals(3, dash.symmetry.size) // Tree, Warrior II, Triangle
        dash.symmetry.forEach { assertTrue(it.rightHoldSec < it.leftHoldSec) }
        assertEquals(listOf("Practice & stress", "Practice & mood"), dash.insights.map { it.title })
        dash.insights.forEach {
            assertTrue(it.text.contains(RankStatsEngine.CAVEAT_CAUSATION))
            assertTrue(it.text.contains(RankStatsEngine.CAVEAT_AUTOCORRELATION))
        }
        assertTrue(dash.insights[0].text.startsWith("Days with more practice minutes tended to coincide with lower self-rated stress"))
        assertNotNull(dash.tension)
        assertTrue(dash.tension!!.meanAfter < dash.tension!!.meanBefore)
        assertEquals(3, dash.phq.size)
        assertTrue(dash.headline.checkInDays in 5..7)
    }

    @Test
    fun personaCInsightReportsNoPattern() {
        val dash = personaDashboard(Persona.C_NULL)
        val stress = dash.insights.first()
        assertTrue(stress.result.pValue!! > 0.05)
        assertTrue(stress.text, stress.text.startsWith("No downward trend") || stress.text.startsWith("A slight downward tendency"))
    }

    @Test
    fun emptyDataIsEmpty() {
        val dash = DashboardBuilder.build(emptyList(), emptyList(), emptyList(), emptyList(), today)
        assertTrue(dash.isEmpty)
        assertTrue(dash.symmetry.isEmpty())
        assertTrue(dash.insights.all { !it.result.sufficient })
    }
}
