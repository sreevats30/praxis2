package com.companion.yogawellness.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.companion.yogawellness.pose.PipelineOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * English voice coach using the built-in Android TextToSpeech + ToneGenerator (§2, zero extra
 * dependencies). Only voices that do not need a network connection are selected, so coaching keeps
 * working with the INTERNET permission stripped and the phone offline.
 */
class OfflineTtsCoach(context: Context) : TextToSpeech.OnInitListener {

    private val engine = CoachEngine()
    private val tts = TextToSpeech(context.applicationContext, this)
    private val tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()

    @Volatile
    private var ready = false

    @Volatile
    var enabled: Boolean = true

    private val _lastUtterance = MutableStateFlow<CoachUtterance?>(null)
    /** Last thing the coach said, shown as an on-screen caption (also useful with sound off). */
    val lastUtterance: StateFlow<CoachUtterance?> = _lastUtterance.asStateFlow()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech init failed: $status")
            return
        }
        val offlineEnglish = runCatching {
            tts.voices.orEmpty().filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
        }.getOrDefault(emptyList())
        val preferred = offlineEnglish.firstOrNull { it.locale.country == "IN" }
            ?: offlineEnglish.firstOrNull { it.locale.country == "GB" }
            ?: offlineEnglish.firstOrNull { it.locale.country == "US" }
            ?: offlineEnglish.firstOrNull()
        if (preferred != null) {
            tts.voice = preferred
        } else {
            tts.language = Locale.US
        }
        tts.setSpeechRate(0.95f)
        ready = true
    }

    /** Call for every pipeline output (any thread). */
    fun onOutput(out: PipelineOutput) {
        val u = engine.update(out) ?: return
        _lastUtterance.value = u
        if (!enabled) return
        if (u.chime) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        if (ready) {
            val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS, "true") }
            val mode = if (u.interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(u.text, mode, params, "coach-${System.nanoTime()}")
        }
    }

    fun reset() {
        engine.reset()
        _lastUtterance.value = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
        tone?.release()
    }

    companion object {
        private const val TAG = "OfflineTtsCoach"
    }
}
