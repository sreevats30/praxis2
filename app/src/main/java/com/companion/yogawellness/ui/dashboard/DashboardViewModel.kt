package com.companion.yogawellness.ui.dashboard

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.report.PdfReportExporter
import com.companion.yogawellness.wellness.DashboardBuilder
import com.companion.yogawellness.wellness.DashboardData
import com.companion.yogawellness.wellness.QuestionnaireType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as YogaWellnessApp
    private val repo = container.repository
    val todayEpochDay: Long = LocalDate.now().toEpochDay()
    private val from = todayEpochDay - (DashboardBuilder.WINDOW_DAYS - 1)

    /** Dashboard data tagged with the profile it was computed from. */
    data class ProfileDashboard(val profileId: String, val data: DashboardData) {
        /** Non-null for demo data, so an exported demo PDF is always labelled as such. */
        val demoLabel: String? get() = if (ProfileIds.isDemo(profileId)) ProfileIds.displayName(profileId) else null
    }

    /** Recomputed (incl. the 5,000-shuffle permutation tests) off the main thread whenever data or the profile changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val profileData: StateFlow<ProfileDashboard?> = container.activeProfile.id
        .flatMapLatest { pid ->
            combine(
                repo.observeCheckInsSince(from, pid),
                repo.observeSessionsSince(from, pid),
                repo.observePosesSince(from, pid),
                repo.observeHistory(QuestionnaireType.PHQ9, pid),
                repo.observeHistory(QuestionnaireType.GAD7, pid)
            ) { checkIns, sessions, poses, phq, gad ->
                ProfileDashboard(pid, DashboardBuilder.build(checkIns, sessions, poses, phq + gad, todayEpochDay))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val data: StateFlow<DashboardData?> = profileData
        .map { it?.data }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun exportReport(onShare: (Intent) -> Unit, onError: (String) -> Unit) {
        val pd = profileData.value ?: return onError("Nothing to export yet")
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { PdfReportExporter.export(container.reportsDir, pd.data, pd.demoLabel) }
                onShare(PdfReportExporter.shareIntent(getApplication(), file))
            } catch (e: Exception) {
                onError("Could not create the report: ${e.message}")
            }
        }
    }
}
