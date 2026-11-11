package com.companion.yogawellness.feedback

import com.companion.yogawellness.pose.PipelineEvent
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.scoring.Asana

data class CoachUtterance(
    val text: String,
    val kind: Kind,
    /** Play the praise chime before speaking. */
    val chime: Boolean = false,
    /** Interrupt whatever is being spoken (safety only). */
    val interrupt: Boolean = false
) {
    enum class Kind { SAFETY, ANNOUNCEMENT, CORRECTION, PRAISE, REMINDER }
}

/**
 * Rate-limited feedback engine (§3 Stage 4), pure Kotlin so it can be unit-tested and replayed.
 *
 *  1. Safety rules first — they bypass the cooldown (with a short gap) and repeat every 6 s.
 *  2. Worst-weighted-joint cue, only after it has been the primary cue for > 1.2 s.
 *  3. 4.5 s cooldown between utterances, and the same cue at most 2× in a row.
 *  4. Praise + chime when >= 88% has been held for 4 s.
 *  5. A periodic "stop if you feel pain" reminder.
 */
class CoachEngine(
    private val cooldownSec: Double = 4.5,
    private val persistSec: Double = 1.2,
    private val maxRepeats: Int = 2,
    private val safetyPersistSec: Double = 0.5,
    private val safetyRepeatSec: Double = 6.0,
    private val safetyMinGapSec: Double = 1.5,
    private val painReminderSec: Double = 120.0,
    /** Must exceed [cooldownSec] so a queued announcement survives one cooldown. */
    private val eventTtlSec: Double = 5.0
) {
    private data class Pending(val utterance: CoachUtterance, val priority: Int, val expiresAt: Double)

    private val pending = ArrayList<Pending>()
    private var lastSpokenT = Double.NEGATIVE_INFINITY

    private var safetyId: String? = null
    private var safetySince = 0.0
    private var lastSafetySpokenT = Double.NEGATIVE_INFINITY

    private var cueCandidate: String? = null
    private var cueSince = 0.0
    private var lastCue: String? = null
    private var repeatCount = 0
    private var noCueSince = Double.NaN

    private var lastReminderT = Double.NaN

    fun reset() {
        pending.clear()
        lastSpokenT = Double.NEGATIVE_INFINITY
        safetyId = null
        lastSafetySpokenT = Double.NEGATIVE_INFINITY
        resetCorrection()
        lastReminderT = Double.NaN
    }

    private fun resetCorrection() {
        cueCandidate = null
        lastCue = null
        repeatCount = 0
        noCueSince = Double.NaN
    }

    /** Feed every pipeline output; returns at most one utterance to speak now. */
    fun update(out: PipelineOutput): CoachUtterance? {
        val t = out.frame.timestampSec
        collectEvents(out, t)
        pending.removeAll { it.expiresAt < t }

        // 1. Safety first
        val alert = out.safety
        if (alert != null) {
            if (alert.id != safetyId) {
                safetyId = alert.id
                safetySince = t
            }
            if (t - safetySince >= safetyPersistSec &&
                t - lastSafetySpokenT >= safetyRepeatSec &&
                t - lastSpokenT >= safetyMinGapSec
            ) {
                lastSafetySpokenT = t
                return speak(t, CoachUtterance(alert.message, CoachUtterance.Kind.SAFETY, interrupt = true))
            }
        } else {
            safetyId = null
        }

        if (t - lastSpokenT < cooldownSec) {
            trackCue(out, t)
            return null
        }

        // 2. Queued events (endurance cap, announcements, praise) in priority order
        pending.minByOrNull { it.priority }?.let { p ->
            pending.remove(p)
            trackCue(out, t)
            val cue = cueCandidate
            // A pose announcement must never swallow a ready correction: say both in one breath
            // (it counts as the first of the cue's repeats).
            if (p.utterance.text.startsWith(POSE_ANNOUNCE_PREFIX) && cue != null && alert == null &&
                t - cueSince >= persistSec
            ) {
                lastCue = cue
                repeatCount = 1
                return speak(t, CoachUtterance("${p.utterance.text} $cue.", CoachUtterance.Kind.CORRECTION))
            }
            return speak(t, p.utterance)
        }

        // 3. Corrective cue (persisted > 1.2 s, max 2 repeats)
        trackCue(out, t)
        val cue = cueCandidate
        if (cue != null && alert == null && t - cueSince >= persistSec) {
            if (cue != lastCue || repeatCount < maxRepeats) {
                repeatCount = if (cue == lastCue) repeatCount + 1 else 1
                lastCue = cue
                return speak(t, CoachUtterance(cue, CoachUtterance.Kind.CORRECTION))
            }
        }

        // 4. Periodic pain reminder while practising
        if (out.scoringArmed) {
            if (lastReminderT.isNaN()) lastReminderT = t
            if (t - lastReminderT >= painReminderSec) {
                lastReminderT = t
                return speak(t, CoachUtterance(PAIN_REMINDER, CoachUtterance.Kind.REMINDER))
            }
        }
        return null
    }

    private fun trackCue(out: PipelineOutput, t: Double) {
        val label = out.lockedLabel
        val cue = if (label != null && label.asana.isScored && out.score != null) out.alignment?.primaryCorrectiveCue else null
        if (cue != cueCandidate) {
            cueCandidate = cue
            cueSince = t
        }
        if (cue == null) {
            if (noCueSince.isNaN()) noCueSince = t
            // Corrected for 2 s: allow the same cue again if the user slips back.
            if (t - noCueSince >= 2.0) {
                lastCue = null
                repeatCount = 0
            }
        } else {
            noCueSince = Double.NaN
        }
    }

    private fun collectEvents(out: PipelineOutput, t: Double) {
        for (e in out.events) {
            when (e) {
                is PipelineEvent.HoldCapReached -> enqueue(
                    t, 0, CoachUtterance(
                        "That's ${e.seconds} seconds. Rise up slowly and rest. Stop if your lower back or knees ache.",
                        CoachUtterance.Kind.SAFETY
                    )
                )
                is PipelineEvent.ScoringArmed -> enqueue(
                    t, 1, CoachUtterance(
                        "You're all set. Begin in mountain pose to calibrate. Stop if you feel any pain.",
                        CoachUtterance.Kind.ANNOUNCEMENT
                    )
                )
                is PipelineEvent.CalibrationComplete -> enqueue(
                    t, 1, CoachUtterance("Calibrated. Move into your first pose.", CoachUtterance.Kind.ANNOUNCEMENT)
                )
                is PipelineEvent.LockAcquired -> {
                    resetCorrection()
                    // Only the latest pose announcement matters.
                    pending.removeAll { it.utterance.text.startsWith(POSE_ANNOUNCE_PREFIX) }
                    if (e.label.asana.isScored) {
                        val side = if (e.label.asana.hasSides) ", ${e.label.side.label.lowercase()}" else ""
                        enqueue(
                            t, 1,
                            CoachUtterance("$POSE_ANNOUNCE_PREFIX${spokenName(e.label.asana)} pose$side.", CoachUtterance.Kind.ANNOUNCEMENT)
                        )
                    }
                }
                is PipelineEvent.Praise -> enqueue(
                    t, 2, CoachUtterance("Beautiful. Hold it there.", CoachUtterance.Kind.PRAISE, chime = true)
                )
                is PipelineEvent.LockReleased -> resetCorrection()
            }
        }
    }

    private fun enqueue(t: Double, priority: Int, u: CoachUtterance) {
        pending.removeAll { it.utterance.text == u.text }
        pending += Pending(u, priority, t + eventTtlSec)
    }

    private fun speak(t: Double, u: CoachUtterance): CoachUtterance {
        lastSpokenT = t
        return u
    }

    companion object {
        const val PAIN_REMINDER = "Remember: stop immediately if you feel any sharp pain or dizziness."
        const val POSE_ANNOUNCE_PREFIX = "Now in "

        /** Names the TTS voice can pronounce clearly. */
        fun spokenName(asana: Asana): String = when (asana) {
            Asana.TADASANA -> "Mountain"
            Asana.VRIKSHASANA -> "Tree"
            Asana.VIRABHADRASANA_II -> "Warrior two"
            Asana.TRIKONASANA -> "Triangle"
            Asana.UTKATASANA -> "Chair"
        }
    }
}
