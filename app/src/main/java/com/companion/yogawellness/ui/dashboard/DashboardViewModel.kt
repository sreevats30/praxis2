package com.companion.yogawellness.ui.dashboard

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.report.PdfReportExporter
import com.companion.yogawellness.wellness.DashboardBuilder
import com.companion.yogawellness.wellness.DashboardData
import com.companion.yogawellness.wellness.Persona
import com.companion.yogawellness.wellness.QuestionnaireType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as YogaWellnessApp
    private val repo = container.repository
    val todayEpochDay: Long = LocalDate.now().toEpochDay()
    private val from = todayEpochDay - (DashboardBuilder.WINDOW_DAYS - 1)

    val personaLabel: String?
        get() = container.preferences.demoPersona?.let { n -> Persona.entries.firstOrNull { it.name == n }?.label }

    /** Recomputed (incl. the 5,000-shuffle permutation tests) off the main thread whenever data changes. */
    val data: StateFlow<DashboardData?> = combine(
        repo.observeCheckInsSince(from),
        repo.observeSessionsSince(from),
        repo.observePosesSince(from),
        repo.observeHistory(QuestionnaireType.PHQ9),
        repo.observeHistory(QuestionnaireType.GAD7)
    ) { checkIns, sessions, poses, phq, gad ->
        DashboardBuilder.build(checkIns, sessions, poses, phq + gad, todayEpochDay)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun exportReport(onShare: (Intent) -> Unit, onError: (String) -> Unit) {
        val d = data.value ?: return onError("Nothing to export yet")
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { PdfReportExporter.export(container.reportsDir, d, personaLabel) }
                onShare(PdfReportExporter.shareIntent(getApplication(), file))
            } catch (e: Exception) {
                onError("Could not create the report: ${e.message}")
            }
        }
    }
}
