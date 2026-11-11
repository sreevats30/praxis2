package com.companion.yogawellness.wellness

import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.PoseRecordEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import com.companion.yogawellness.scoring.SymmetryCalculator
import com.companion.yogawellness.stats.CorrelationResult
import com.companion.yogawellness.stats.RankStatsEngine

/** Left-vs-right comparison for one sided asana over the dashboard window. */
data class SymmetryRow(
    val asana: Asana,
    val leftHoldSec: Double,
    val rightHoldSec: Double,
    val leftScore: Double?,
    val rightScore: Double?,
    val holdSymmetryPct: Double?
)

data class InsightCard(val title: String, val result: CorrelationResult, val text: String)

data class Headline(
    val practiceMinutes: Double,
    val sessions: Int,
    val meanQuality: Double?,
    val previousWeekQuality: Double?,
    val checkInDays: Int
)

data class TensionSummary(val meanBefore: Double, val meanAfter: Double, val days: Int)

data class DashboardData(
    val fromEpochDay: Long,
    val toEpochDay: Long,
    val daily: List<DailyRow>,
    val weeks: List<WeeklyRow>,
    /** Last 7 days. */
    val headline: Headline,
    val symmetry: List<SymmetryRow>,
    val insights: List<InsightCard>,
    val phq: List<QuestionnaireEntity>,
    val gad: List<QuestionnaireEntity>,
    val tension: TensionSummary?
) {
    val isEmpty: Boolean get() = daily.none { it.stress != null || it.practiceMinutes > 0 }
}

/** Builds everything the dashboard and the PDF report show (§10 Prompt 5). Pure Kotlin. */
object DashboardBuilder {
    const val WINDOW_DAYS = 42

    fun build(
        checkIns: List<DailyCheckInEntity>,
        sessions: List<PracticeSessionEntity>,
        poses: List<PoseRecordEntity>,
        questionnaires: List<QuestionnaireEntity>,
        todayEpochDay: Long,
        windowDays: Int = WINDOW_DAYS
    ): DashboardData {
        val from = todayEpochDay - (windowDays - 1)
        val inWindow = sessions.filter { it.epochDay in from..todayEpochDay }
        val daily = DailyAggregator.combine(checkIns, inWindow, from, todayEpochDay)
        val weeks = DailyAggregator.weekly(daily)

        val last7 = daily.takeLast(7)
        val prev7 = daily.dropLast(7).takeLast(7)
        val headline = Headline(
            practiceMinutes = last7.sumOf { it.practiceMinutes },
            sessions = inWindow.count { it.epochDay > todayEpochDay - 7 },
            meanQuality = weightedQuality(last7),
            previousWeekQuality = weightedQuality(prev7),
            checkInDays = last7.count { it.stress != null }
        )

        val insights = listOf(
            "Practice & stress" to Pair("practice minutes", DailyAggregator.minutesVs(daily) { it.stress }),
            "Practice & mood" to Pair("practice minutes", DailyAggregator.minutesVs(daily) { it.mood })
        ).map { (title, spec) ->
            val (xLabel, series) = spec
            val yLabel = if (title.endsWith("stress")) "self-rated stress" else "self-rated mood"
            val r = RankStatsEngine.analyze(series.first, series.second)
            InsightCard(title, r, RankStatsEngine.insight(r, xLabel, yLabel))
        }

        val tensionDays = checkIns.filter { it.epochDay in from..todayEpochDay && it.preTension != null && it.postTension != null }
        val tension = if (tensionDays.isEmpty()) null else TensionSummary(
            tensionDays.map { it.preTension!!.toDouble() }.average(),
            tensionDays.map { it.postTension!!.toDouble() }.average(),
            tensionDays.size
        )

        return DashboardData(
            fromEpochDay = from,
            toEpochDay = todayEpochDay,
            daily = daily,
            weeks = weeks,
            headline = headline,
            symmetry = symmetry(poses),
            insights = insights,
            phq = questionnaires.filter { it.type == QuestionnaireType.PHQ9.name }.sortedBy { it.epochDay },
            gad = questionnaires.filter { it.type == QuestionnaireType.GAD7.name }.sortedBy { it.epochDay },
            tension = tension
        )
    }

    private fun weightedQuality(rows: List<DailyRow>): Double? {
        val practiced = rows.filter { it.practiceQuality != null && it.practiceMinutes > 0 }
        if (practiced.isEmpty()) return null
        return practiced.sumOf { it.practiceQuality!! * it.practiceMinutes } / practiced.sumOf { it.practiceMinutes }
    }

    /** Inter-pose symmetry: mean best (grace) hold per side, per sided asana. */
    fun symmetry(poses: List<PoseRecordEntity>): List<SymmetryRow> =
        poses.mapNotNull { p ->
            val asana = Asana.entries.firstOrNull { it.name == p.asana } ?: return@mapNotNull null
            val side = PoseSide.entries.firstOrNull { it.name == p.side }?.workingSide ?: return@mapNotNull null
            if (!asana.hasSides) null else Triple(asana, side, p)
        }
            .groupBy { it.first }
            .mapNotNull { (asana, recs) ->
                val left = recs.filter { it.second == BodySide.LEFT }.map { it.third }
                val right = recs.filter { it.second == BodySide.RIGHT }.map { it.third }
                if (left.isEmpty() || right.isEmpty()) return@mapNotNull null
                val lh = left.map { it.bestGraceHoldSec }.average()
                val rh = right.map { it.bestGraceHoldSec }.average()
                SymmetryRow(
                    asana, lh, rh,
                    left.mapNotNull { it.meanScore }.takeIf { it.isNotEmpty() }?.average(),
                    right.mapNotNull { it.meanScore }.takeIf { it.isNotEmpty() }?.average(),
                    SymmetryCalculator.holdSymmetry(lh, rh)
                )
            }
            .sortedBy { it.asana.ordinal }
}
