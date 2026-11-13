package com.companion.yogawellness.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.ActiveProfile
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.data.WellnessRepository
import com.companion.yogawellness.wellness.HistoryAggregator
import com.companion.yogawellness.wellness.HistorySummary
import com.companion.yogawellness.wellness.SessionWithPoses
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val profileId: String = ProfileIds.MY_ACCOUNT,
    val sessions: List<SessionWithPoses> = emptyList(),
    val summary: HistorySummary = HistorySummary(0, 0.0, null, null),
    val loading: Boolean = true
) {
    /** Demo datasets are read-only: no delete, no manual logging. */
    val canEdit: Boolean get() = profileId == ProfileIds.MY_ACCOUNT
}

class HistoryViewModel(
    private val repo: WellnessRepository,
    private val activeProfile: ActiveProfile
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HistoryUiState> = activeProfile.id
        .flatMapLatest { pid ->
            combine(repo.observeAllSessions(pid), repo.observeAllPoses(pid)) { sessions, poses ->
                val items = HistoryAggregator.group(sessions, poses)
                HistoryUiState(pid, items, HistoryAggregator.summary(items), loading = false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    /** No-op for demo profiles (the repository also only deletes My Account rows). */
    fun deleteSession(sessionId: Long) {
        if (activeProfile.id.value != ProfileIds.MY_ACCOUNT) return
        viewModelScope.launch { repo.deleteSession(sessionId) }
    }

    fun logManual(minutes: Int) {
        if (minutes !in WellnessRepository.MANUAL_MINUTES_RANGE) return
        viewModelScope.launch {
            repo.logManualSession(minutes)
            activeProfile.returnToMyAccount()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as YogaWellnessApp
                HistoryViewModel(app.repository, app.activeProfile)
            }
        }
    }
}
