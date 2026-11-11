package com.companion.yogawellness.replay

import com.companion.yogawellness.feedback.CoachUtterance
import com.companion.yogawellness.pose.GravityAligner
import com.companion.yogawellness.pose.PosePipeline
import com.companion.yogawellness.pose.SensorSnapshot
import com.companion.yogawellness.scoring.Asana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RecordReplayTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun codecRoundTrip() {
        val frame = SyntheticPoseBuilder().warriorII(1234)
        val sensors = SensorSnapshot(GravityAligner.checkPhoneTilt(0f, 9.81f, 0f), 0.0123)
        val decoded = PoseClipCodec.decode(PoseClipCodec.encode(frame, sensors))!!
        assertEquals(1234L, decoded.frame.timestampMs)
        assertEquals(33, decoded.frame.landmarks.size)
        frame.landmarks.zip(decoded.frame.landmarks).forEach { (a, b) ->
            assertEquals(a.x, b.x, 1e-5f)
            assertEquals(a.y, b.y, 1e-5f)
            assertEquals(a.visibility, b.visibility, 1e-3f)
        }
        assertEquals(0.0123, decoded.sensors!!.motionSd!!, 1e-4)
        assertTrue(decoded.sensors!!.tilt!!.isUprightOk)
        assertNull(PoseClipCodec.decode(PoseClipCodec.header(0)))
    }

    @Test
    fun recorderStopsAtTwentySecondsAndReplayMatchesLive() {
        val dir = tmp.newFolder("recordings")
        val recorder = LandmarkRecorder(dir)
        val demo = DemoClips.warriorCorrection()
        recorder.start(nowMs = 0L, note = "unit test")

        // "Live" run: process frames while recording them
        val live = PosePipeline()
        val liveScores = ArrayList<Double?>()
        var finished: File? = null
        for (cf in demo.frames) {
            if (finished != null) break
            liveScores += live.process(cf.frame, cf.sensors).score
            finished = recorder.append(cf.frame, cf.sensors)
        }
        assertNotNull("recorder should auto-stop at 20 s", finished)
        assertTrue(!recorder.isRecording)
        assertEquals(1, recorder.listClips().size)

        // Replay the file through a fresh pipeline: identical results
        val clip = LandmarkReplayer.load(finished!!)
        assertEquals(liveScores.size, clip.frames.size)
        assertEquals(20.0, clip.durationSec, 0.05)
        val report = LandmarkReplayer.replayAll(clip, coach = null)
        report.outputs.map { it.score }.zip(liveScores).forEach { (a, b) ->
            if (a == null || b == null) assertEquals(a, b) else assertEquals(b, a, 0.05)
        }
    }

    @Test
    fun demoClipReproducesStageDemo() {
        val report = LandmarkReplayer.replayAll(DemoClips.warriorCorrection())
        println(report.summary())

        val warrior = report.segments.single { it.label.asana == Asana.VIRABHADRASANA_II }
        assertTrue("min=${warrior.minScore}", warrior.minScore!! in 38.0..45.0) // the wrong 125° pose
        assertTrue("max=${warrior.maxScore}", warrior.maxScore!! >= 95.0)      // after correction
        assertTrue(warrior.bestGraceHoldSec > 8.0)
        assertTrue(report.segments.first().label.asana == Asana.TADASANA)
        assertEquals(1, report.praiseCount)
        val texts = report.utterances.map { it.utterance.text }
        // The audience must hear the knee cue while the pose is still wrong (before 11 s)
        val kneeCue = report.utterances.first { it.utterance.text.contains("Bend your front knee deeper toward 90 degrees") }
        assertTrue("cue at ${kneeCue.tSec} s; said=$texts", kneeCue.tSec < 11.0)
        assertTrue(report.utterances.any { it.utterance.kind == CoachUtterance.Kind.PRAISE })
    }

    /**
     * Tuning harness: drop clips recorded on a phone into app/src/test/resources/clips/ and run
     *   ./gradlew :app:testDebugUnitTest --tests '*RecordReplayTest.replayRecordedClips' -i
     * to print a per-clip report (locks, scores, holds, spoken cues).
     */
    @Test
    fun replayRecordedClips() {
        val dir = File("src/test/resources/clips")
        val clips = dir.listFiles { f -> f.name.endsWith(".jsonl") }.orEmpty()
        for (f in clips.sortedBy { it.name }) {
            println(LandmarkReplayer.replayAll(LandmarkReplayer.load(f)).summary())
        }
    }
}
