package com.companion.yogawellness

import android.app.Application
import com.companion.yogawellness.data.ActiveProfile
import com.companion.yogawellness.data.AppPreferences
import com.companion.yogawellness.data.ProfileSwitcher
import com.companion.yogawellness.data.SessionAutoRecorder
import com.companion.yogawellness.data.WellnessRepository
import com.companion.yogawellness.data.db.WellnessDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Manual dependency container — the app is small enough not to need a DI framework. */
class YogaWellnessApp : Application() {

    /** Outlives screens, e.g. to finish saving a practice session after the user navigates away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val preferences: AppPreferences by lazy { AppPreferences(this) }
    val database: WellnessDatabase by lazy { WellnessDatabase.create(this) }
    val repository: WellnessRepository by lazy {
        WellnessRepository(database, preferences, listOf(recordingsDir, reportsDir))
    }

    /** Dataset being viewed (My Account or a demo persona). In-memory: every cold start is My Account. */
    val activeProfile = ActiveProfile()
    val profileSwitcher: ProfileSwitcher by lazy { ProfileSwitcher(repository, activeProfile) }

    /** Continuous live-session saving; writes run on [appScope], never on the camera thread. */
    val sessionRecorder: SessionAutoRecorder by lazy { SessionAutoRecorder(repository, appScope) }

    /** App-specific external storage: private to the app, but reachable with `adb pull` for tuning. */
    val recordingsDir: File get() = File(getExternalFilesDir(null) ?: filesDir, "recordings")

    /** Exported PDF reports (shared via FileProvider). */
    val reportsDir: File get() = File(cacheDir, "reports")
}
