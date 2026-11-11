# Recorded pose clips for the replay harness

Put `.jsonl` clips recorded in the app (Practice screen → ⏺ record button) here, then run:

    ./gradlew :app:testDebugUnitTest --tests '*RecordReplayTest.replayRecordedClips' -i

Each clip is replayed through the exact live pipeline and a report is printed: locked poses,
score min/mean/max, best holds, stability, and every cue the coach would have spoken.

Getting clips off a phone (debug build):

    adb pull /sdcard/Android/data/com.companion.yogawellness/files/recordings/ app/src/test/resources/clips/

Clips contain only landmark coordinates (never camera images). Only commit clips of people who
have agreed to it.
