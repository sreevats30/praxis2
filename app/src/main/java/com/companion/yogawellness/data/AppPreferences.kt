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

    /** Time the first-run consent & safety screen was accepted (0 = not yet). */
    var consentAcceptedAtMs: Long
        get() = sp.getLong(KEY_CONSENT, 0L)
        set(value) = sp.edit { putLong(KEY_CONSENT, value) }

    val hasConsent: Boolean get() = consentAcceptedAtMs > 0L

    /** Name of the seeded demo persona currently loaded, if any. */
    var demoPersona: String?
        get() = sp.getString(KEY_PERSONA, null)
        set(value) = sp.edit { if (value == null) remove(KEY_PERSONA) else putString(KEY_PERSONA, value) }

    fun clearAll() = sp.edit { clear() }

    companion object {
        const val FILE = "yoga_wellness_prefs"
        private const val KEY_PROFILE = "tolerance_profile"
        private const val KEY_FRONT_CAMERA = "front_camera"
        private const val KEY_VOICE = "voice_coach"
        private const val KEY_CONSENT = "consent_accepted_at"
        private const val KEY_PERSONA = "demo_persona"
    }
}
