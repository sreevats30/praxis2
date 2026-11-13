package com.companion.yogawellness.ui.wellness

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.MockPersonaSeeder
import com.companion.yogawellness.wellness.Persona
import com.companion.yogawellness.wellness.QuestionnaireType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Shared state for the check-in, questionnaire, settings and home screens. */
class WellnessViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as YogaWellnessApp
    private val repo = container.repository
    val prefs = container.preferences

    val todayEpochDay: Long = LocalDate.now().toEpochDay()

    private fun <T> flowState(initial: T, f: kotlinx.coroutines.flow.Flow<T>): StateFlow<T> =
        f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val todayCheckIn: StateFlow<DailyCheckInEntity?> = flowState(null, repo.observeCheckIn(todayEpochDay))
    val latestPhq: StateFlow<QuestionnaireEntity?> = flowState(null, repo.observeLatest(QuestionnaireType.PHQ9))
    val latestGad: StateFlow<QuestionnaireEntity?> = flowState(null, repo.observeLatest(QuestionnaireType.GAD7))
    val phqHistory: StateFlow<List<QuestionnaireEntity>> = flowState(emptyList(), repo.observeHistory(QuestionnaireType.PHQ9))
    val gadHistory: StateFlow<List<QuestionnaireEntity>> = flowState(emptyList(), repo.observeHistory(QuestionnaireType.GAD7))

    /** Week strip, streak and sparklines for the home screen. */
    val activity: StateFlow<ActivitySummary> = flowState(
        ActivitySummary.empty(todayEpochDay),
        combine(
            repo.observeCheckInsSince(todayEpochDay - (ActivitySummary.LOOKBACK_DAYS - 1)),
            repo.observeSessionsSince(todayEpochDay - (ActivitySummary.LOOKBACK_DAYS - 1))
        ) { checkIns, sessions -> ActivitySummary.from(checkIns, sessions, todayEpochDay) }.flowOn(Dispatchers.Default)
    )

    fun history(type: QuestionnaireType) = if (type == QuestionnaireType.PHQ9) phqHistory else gadHistory
    fun latest(type: QuestionnaireType) = if (type == QuestionnaireType.PHQ9) latestPhq else latestGad

    fun acceptConsent() {
        prefs.consentAcceptedAtMs = System.currentTimeMillis()
    }

    fun setProfile(p: ToleranceProfile) {
        prefs.toleranceProfile = p
    }

    fun saveCheckIn(mood: Int, stress: Int, sleep: Int, pre: Int?, post: Int?, onSaved: () -> Unit) {
        viewModelScope.launch {
            repo.saveCheckIn(todayEpochDay, mood, stress, sleep, pre, post)
            onSaved()
        }
    }

    fun saveQuestionnaire(type: QuestionnaireType, answers: List<Int?>, onSaved: (QuestionnaireEntity) -> Unit) {
        viewModelScope.launch { onSaved(repo.saveQuestionnaire(type, todayEpochDay, answers)) }
    }

    fun loadPersona(persona: Persona, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.replaceWithPersona(MockPersonaSeeder.generate(persona, todayEpochDay))
            onDone()
        }
    }

    fun deleteAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteAllData()
            onDone()
        }
    }
}
