package com.companion.yogawellness.data

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.scoring.PoseSegmentStats

/** Where [SessionAutoRecorder] checkpoints the live session (implemented by [WellnessRepository]). */
interface LiveSessionStore {
    suspend fun upsertLiveSession(
        sessionId: Long?,
        segments: List<PoseSegmentStats>,
        poseStartedAtMs: Map<AsanaLabel, Long>,
        startedAtMs: Long,
        endedAtMs: Long,
        epochDay: Long,
        toleranceProfile: String
    ): Long?
}
