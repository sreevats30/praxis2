package com.companion.yogawellness.data

import android.content.Context
import androidx.core.content.edit
import com.companion.yogawellness.scoring.ToleranceProfile

/** Small key-value settings kept in private app storage (never backed up, see manifest). */
class AppPreferences(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var toleranceProfile: ToleranceProfile
        get() = sp.getString(KEY_PROFILE, null)
            ?.let { name -> ToleranceProfile.entries.firstOrNull { it.name == name } }
            ?: ToleranceProfile.STANDARD
        set(value) = sp.edit { putString(KEY_PROFILE, value.name) }

    var useFrontCamera: Boolean
        get() = sp.getBoolean(KEY_FRONT_CAMERA, true)
        set(value) = sp.edit { putBoolean(KEY_FRONT_CAMERA, value) }

    var voiceCoachEnabled: Boolean
        get() = sp.getBoolean(KEY_VOICE, true)
        set(value) = sp.edit { putBoolean(KEY_VOICE, value) }

    fun clearAll() = sp.edit { clear() }

    companion object {
        const val FILE = "yoga_wellness_prefs"
        private const val KEY_PROFILE = "tolerance_profile"
        private const val KEY_FRONT_CAMERA = "front_camera"
        private const val KEY_VOICE = "voice_coach"
    }
}
