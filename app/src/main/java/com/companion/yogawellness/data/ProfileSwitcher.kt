package com.companion.yogawellness.data

import com.companion.yogawellness.wellness.MockPersonaSeeder
import com.companion.yogawellness.wellness.Persona
import java.time.LocalDate

/** Switches the viewed dataset, seeding a demo persona the first time it is opened. */
class ProfileSwitcher(
    private val repo: WellnessRepository,
    private val active: ActiveProfile,
    private val todayEpochDay: () -> Long = { LocalDate.now().toEpochDay() }
) {
    /** Selects [profileId]; a demo persona with no rows yet is seeded first. */
    suspend fun select(profileId: String) {
        if (ProfileIds.isDemo(profileId)) {
            val persona = Persona.forProfileId(profileId) ?: return
            if (!repo.hasData(profileId)) repo.seedPersona(MockPersonaSeeder.generate(persona, todayEpochDay()))
        }
        active.select(profileId)
    }

    /** Settings: always (re)seed so the persona's 42-day window ends today, then view it. */
    suspend fun reseedAndSelect(persona: Persona) {
        repo.seedPersona(MockPersonaSeeder.generate(persona, todayEpochDay()))
        active.select(persona.profileId)
    }

    suspend fun removeAllDemoData() {
        repo.removeAllDemoData()
        active.returnToMyAccount()
    }
}
