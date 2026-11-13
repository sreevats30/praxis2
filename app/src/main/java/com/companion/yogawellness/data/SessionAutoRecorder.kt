package com.companion.yogawellness.data

import android.util.Log
import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.scoring.PoseSegmentStats
import com.companion.yogawellness.wellness.SessionSummarizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

data class ReceiptPose(
    val name: String,
    val sideLabel: String?,
    val lockedSec: Double,
    val qualitySec: Double,
    val meanScore: Double?
)

data class SessionReceipt(val poses: List<ReceiptPose>, val totalSec: Double, val meanScore: Double?)

/**
 * Saves the live practice session continuously so nothing is lost to Home presses, screen lock,
 * camera switches or a crash (Phase 2).
 *
 *  - **Lazy:** no row exists until some scored pose has been locked for
 *    [SessionSummarizer.MIN_POSE_LOCK_SEC]; empty sessions are never written.
 *  - **Idempotent:** the first qualifying checkpoint inserts the session; later ones update that
 *    same row and replace its pose rows ([LiveSessionStore.upsertLiveSession]).
 *  - **Serialised and ordered:** one [Mutex] guards all DB work, so a checkpoint and a finalize
 *    can never interleave or create two rows; and every write carries a per-session sequence
 *    number, so an older snapshot that reaches the mutex late can never overwrite a newer one
 *    (launch order on a multi-threaded dispatcher is not lock-acquisition order).
 *  - **Capture now, write later:** state is snapshotted synchronously on the caller's thread and
 *    written on [scope] (the app-level scope), so the camera thread never touches the database
 *    and cancelling a screen's scope cannot cancel a write.
 *  - **Never throws:** failures are logged (no user text) and retried on the next tick.
 *
 * Replays must never call into this class.
 */
class SessionAutoRecorder(
    private val store: LiveSessionStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val checkpointEveryMs: Long = 5_000L,
    private val epochDayOf: (Long) -> Long = { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay() },
    /** How often [tick] actually looks at the segments (it is called on every camera frame). */
    private val sampleEveryMs: Long = 200L,
    private val log: (String, Throwable) -> Unit = { msg, e -> Log.w(TAG, msg, e) }
) {
    /** REC badge state. */
    data class Rec(val active: Boolean = false, val elapsedSec: Int = 0, val posesSaved: Int = 0)

    /** One recording session. [dbId] and [writtenSeq] are only touched under [writeMutex]. */
    private class Handle {
        var startedAtMs = -1L
        var activeSinceMs = -1L
        var dbId: Long? = null
        /** Sequence number of the last scheduled write (under `lock`). */
        var writesScheduled = 0
        /** Sequence number of the newest snapshot already in the database. */
        var writtenSeq = 0
        val poseStartedAt = HashMap<AsanaLabel, Long>()
    }

    private val lock = Any()
    private val writeMutex = Mutex()
    private var handle = Handle()
    private var lastSampleMs = Long.MIN_VALUE / 2
    private var lastCheckpointMs = Long.MIN_VALUE / 2
    private var retryNeeded = false
    private val pending = LinkedHashSet<Job>()

    private val _rec = MutableStateFlow(Rec())
    val rec: StateFlow<Rec> = _rec.asStateFlow()

    /** Start the REC clock (e.g. when calibration completes) even before a pose locks. */
    fun markActive() {
        val now = clock()
        synchronized(lock) {
            begin(now)
            if (handle.activeSinceMs < 0) handle.activeSinceMs = now
            publishLocked(now)
        }
    }

    /** Call on every processed LIVE frame. Cheap: returns at once unless a sample is due. */
    fun tick(segments: () -> List<PoseSegmentStats>, toleranceProfile: String) {
        val now = clock()
        synchronized(lock) {
            if (now - lastSampleMs < sampleEveryMs) return
            lastSampleMs = now
        }
        val segs = segments()
        synchronized(lock) {
            begin(now)
            val h = handle
            for (s in segs) {
                if (s.label.asana.isScored && s.lockedSec > 0) {
                    h.poseStartedAt.putIfAbsent(s.label, now)
                    if (h.activeSinceMs < 0) h.activeSinceMs = now
                }
            }
            publishLocked(now)
            val qualifies = SessionSummarizer.qualifying(segs).isNotEmpty()
            val due = retryNeeded || h.writesScheduled == 0 || now - lastCheckpointMs >= checkpointEveryMs
            if (qualifies && due) scheduleWriteLocked(h, segs, toleranceProfile, now, final = false)
        }
    }

    /** Persist now and keep the session open (lifecycle ON_STOP / app backgrounded). */
    fun checkpointNow(segments: List<PoseSegmentStats>, toleranceProfile: String) {
        val now = clock()
        synchronized(lock) {
            begin(now)
            if (SessionSummarizer.qualifying(segments).isNotEmpty()) {
                scheduleWriteLocked(handle, segments, toleranceProfile, now, final = false)
            }
        }
    }

    /**
     * Persist now, close this session and start a fresh one (Finish & Save, camera switch, replay
     * start, screen closed). Returns a receipt immediately from [segments], or null if nothing was
     * held for [SessionSummarizer.MIN_POSE_LOCK_SEC].
     */
    fun finalizeAndRotate(segments: List<PoseSegmentStats>, toleranceProfile: String): SessionReceipt? {
        val now = clock()
        val kept = SessionSummarizer.qualifying(segments)
        synchronized(lock) {
            val old = handle
            handle = Handle() // the next session can never reuse the old row id
            lastSampleMs = Long.MIN_VALUE / 2
            lastCheckpointMs = Long.MIN_VALUE / 2
            retryNeeded = false
            _rec.value = Rec()
            if (old.startedAtMs < 0) old.startedAtMs = now
            if (kept.isNotEmpty()) scheduleWriteLocked(old, segments, toleranceProfile, now, final = true)
        }
        if (kept.isEmpty()) return null
        val total = kept.sumOf { it.lockedSec }
        val scored = kept.filter { it.meanScore != null }
        return SessionReceipt(
            poses = kept.map {
                ReceiptPose(
                    name = it.label.asana.displayName,
                    sideLabel = if (it.label.asana.hasSides) it.label.side.label else null,
                    lockedSec = it.lockedSec,
                    qualitySec = it.qualitySec,
                    meanScore = it.meanScore
                )
            },
            totalSec = total,
            meanScore = if (scored.isEmpty()) null else scored.sumOf { it.meanScore!! * it.lockedSec } / scored.sumOf { it.lockedSec }
        )
    }

    /** Waits until every scheduled write has finished (tests; optional on shutdown). */
    suspend fun awaitPendingWrites() {
        while (true) {
            val jobs = synchronized(lock) { pending.toList() }
            if (jobs.isEmpty()) return
            jobs.joinAll()
        }
    }

    // ------------------------------------------------------------------------------------------

    private fun begin(now: Long) {
        if (handle.startedAtMs < 0) handle.startedAtMs = now
    }

    private fun publishLocked(now: Long) {
        val h = handle
        val active = h.activeSinceMs >= 0
        val elapsed = if (active) ((now - h.activeSinceMs) / 1000).toInt() else 0
        _rec.value = _rec.value.copy(active = active, elapsedSec = elapsed)
    }

    private fun scheduleWriteLocked(
        h: Handle,
        segments: List<PoseSegmentStats>,
        toleranceProfile: String,
        endedAtMs: Long,
        final: Boolean
    ) {
        lastCheckpointMs = endedAtMs
        retryNeeded = false
        val seq = ++h.writesScheduled
        val starts = HashMap(h.poseStartedAt)
        val startedAt = h.startedAtMs
        val posesSaved = SessionSummarizer.qualifying(segments).size
        val job = scope.launch {
            // A final write is retried a few times; a checkpoint is retried by the next tick.
            val attempts = if (final) 3 else 1
            for (attempt in 1..attempts) {
                val ok = writeMutex.withLock {
                    // A newer snapshot of this session is already stored: this one is stale.
                    if (seq <= h.writtenSeq) return@withLock true
                    try {
                        h.dbId = store.upsertLiveSession(
                            h.dbId, segments, starts, startedAt, endedAtMs, epochDayOf(startedAt), toleranceProfile
                        )
                        h.writtenSeq = seq
                        true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log("Session checkpoint failed (attempt $attempt)", e)
                        false
                    }
                }
                if (ok) {
                    synchronized(lock) {
                        if (h === handle && seq == h.writtenSeq) _rec.value = _rec.value.copy(posesSaved = posesSaved)
                    }
                    break
                }
                if (!final) {
                    synchronized(lock) { if (h === handle) retryNeeded = true }
                    break
                }
                delay(250L * attempt)
            }
        }
        pending += job
        job.invokeOnCompletion { synchronized(lock) { pending.remove(job) } }
    }

    companion object {
        private const val TAG = "SessionAutoRecorder"
    }
}
