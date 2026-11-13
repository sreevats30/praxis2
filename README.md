# Yoga Motor-Skill & Mental Wellness Companion

Offline Android app built from [`blueprint.md`](blueprint.md) (v5): live pose scoring for 4 asanas + Tadasana
calibration, an offline voice coach, daily/fortnightly wellness check-ins with a crisis guardrail, honest
statistics, and a 1-page PDF report. Everything runs on the phone; the `INTERNET` permission is stripped.

> This app is a wellness and self-reflection companion, not a medical or diagnostic device.

## Requirements

| Tool | Version |
|---|---|
| JDK | 17 or 21 (Android Studio's bundled JBR works) |
| Android SDK | Platform 35, Build-Tools 34/35 (Android Studio installs these) |
| Phone | Android 8.0+ (API 26), a camera, ~150 MB free. Use a **real phone** (or an ARM64 emulator): MediaPipe 0.10.14 ships no x86_64 native library, so pose detection fails on the usual x86_64 emulator |
| Internet | Only for the **first build** (Gradle dependencies + the 9 MB MediaPipe model) |

The pose model `pose_landmarker_full.task` is downloaded automatically into `app/src/main/assets/` before the
first build (see `downloadPoseModel` in `app/build.gradle.kts`). It is git-ignored.

## Build, test and install

Open the project folder in Android Studio / Antigravity IDE (it reads `local.properties` / your SDK location),
or use the command line from the repo root.

Run all JVM unit tests (scoring proof table, classifier, coach, stats, Room, screen renders):

```bash
./gradlew :app:testDebugUnitTest
```

Build the debug APK (output: `app/build/outputs/apk/debug/app-debug.apk`):

```bash
./gradlew :app:assembleDebug
```

Lint (must report no issues):

```bash
./gradlew :app:lintDebug
```

Install on a USB-connected phone (USB debugging on):

```bash
./gradlew :app:installDebug
```

Run the on-device checks (real PDF writer + MediaPipe model load) with the phone connected:

```bash
./gradlew :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat` instead of `./gradlew`. If Gradle cannot find the SDK, create `local.properties`
in the repo root containing `sdk.dir=/path/to/Android/Sdk`.

## Stage demo script

1. **First run** → accept the consent & safety screen (three boxes).
2. **Settings → Demo data → Persona A** to fill 42 days of history (B = exam gap, C = null pattern).
3. **Start practice** → prop the phone upright 2–3 m away. The pre-flight pills must all turn green
   (full body, distance, phone steady ≤15° tilt, lighting).
4. Stand in **Tadasana** for 5 s → "Calibrated".
5. **Warrior II with a shallow front knee (~125°)** → red 41% score ring, curved arrow on the knee, voice:
   *"Bend your front knee deeper toward 90 degrees"*. Tap **∑ Explain Score** to show every term of the formula.
6. Bend to ~95° → green, hold timer runs, chime + *"Beautiful. Hold it there."* after 4 s at ≥88%.
7. Do the other side → left-vs-right symmetry. Back out → the session is saved.
8. **Open dashboard** → weekly trend, symmetry, insight cards (ρ, p, n + caveats) → **Export PDF** → share.
9. **Mood reflection (PHQ-9)** → answering item 9 with "Several days" opens the Tele-MANAS 14416 / 112 sheet.
10. Privacy proof: Android Studio → `app/src/main/AndroidManifest.xml` → **Merged Manifest** tab shows only
    `CAMERA` (plus AndroidX's app-private `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`), no `INTERNET`.

**No camera / bad lighting on stage?** Practice screen → ⟲ (replay) → *Demo · Warrior II correction
(synthetic)* plays the same wrong → corrected sequence through the real pipeline, with voice.

## Tuning thresholds with real recordings

1. Practice screen → ⏺ records 20 s of landmarks (never images) to a `.jsonl` clip.
2. Copy clips to the test folder:

```bash
adb pull /sdcard/Android/data/com.companion.yogawellness/files/recordings/ app/src/test/resources/clips/
```

3. Replay them through the exact live pipeline and print a report (locks, scores, holds, spoken cues):

```bash
./gradlew :app:testDebugUnitTest --tests '*RecordReplayTest.replayRecordedClips' -i
```

Joint targets/tolerances live in `scoring/AsanaSpecs.kt`; classifier rules in
`classifier/RuleAndCentroidClassifier.kt`; sway mapping in `scoring/SwayStabilityTracker.kt`.

## Where things are (blueprint section → code)

| Blueprint | Code (`app/src/main/java/com/companion/yogawellness/`) |
|---|---|
| §2.1 manifest privacy | `app/src/main/AndroidManifest.xml`, `res/xml/data_extraction_rules.xml` |
| §5 pre-flight + IMU tilt | `pose/PreflightGuard.kt`, `pose/GravityAligner.kt`, `camera/AccelerometerMonitor.kt` |
| Stage 1 pose + angles + 1€ | `camera/PoseLandmarkerHelper.kt`, `pose/JointAngleExtractor.kt`, `pose/OneEuroFilter.kt` |
| §6 classifier + lock | `classifier/RuleAndCentroidClassifier.kt` |
| §7 scoring + specs | `scoring/AlignmentScorer.kt`, `scoring/AsanaSpecs.kt` |
| Sway, hold, symmetry | `scoring/SwayStabilityTracker.kt`, `scoring/HoldStateMachine.kt`, `scoring/SymmetryAndSession.kt` |
| Pipeline (stages 1–3) | `pose/PosePipeline.kt` |
| Stage 4 coach + safety | `feedback/CoachEngine.kt`, `feedback/SafetyGuards.kt`, `feedback/OfflineTtsCoach.kt` |
| Record / replay | `replay/LandmarkRecorder.kt`, `replay/LandmarkReplayer.kt`, `replay/DemoClips.kt` |
| §9 consent, check-in, PHQ/GAD, crisis | `ui/consent/`, `ui/wellness/`, `wellness/Questionnaires.kt` |
| §9.4 statistics | `stats/RankStatsEngine.kt` |
| Personas | `wellness/MockPersonaSeeder.kt` |
| Dashboard + PDF | `ui/dashboard/`, `wellness/DashboardBuilder.kt`, `report/PdfReportExporter.kt` |
| Room storage | `data/db/`, `data/WellnessRepository.kt` |
| Design system (colours, type, shapes) | `ui/theme/Theme.kt`, `ui/theme/Type.kt` |
| Shared UI components + artwork | `ui/components/Clinical.kt`, `ui/components/Artwork.kt` |
| Home summary (streak, week, sparklines) | `wellness/ActivitySummary.kt`, `ui/home/` |

## Design system

- **Palette**: deep clinical navy → teal hero gradient with a mint/cyan accent that matches the launcher icon;
  semantic colours for mood (violet), stress (orange), sleep (indigo); score bands stay green / amber / red.
  Light and dark themes are both defined in `ui/theme/Theme.kt` (`AppTheme.colors` holds the extended tokens).
- **Type**: Plus Jakarta Sans for headings and numbers, Inter for text. Both are bundled in `res/font/` (the app has
  no internet permission, so no downloadable fonts); their SIL OFL licences ship in `assets/licenses/`.
- **Components** (`ui/components/`): `ClinicalTopBar`, `PanelCard`, `GradientButton`, `SoftButton`, `RingGauge`,
  `ProgressTrack`, `MetricTile`, `Pill`/`GlassPill`, `Callout`, `Reveal` (staggered entrance), plus artwork:
  `HeroDecor`, `PulseLine` (animated ECG trace), `BrandMark` and `PoseGlyph` (keypoint-style figure for each asana).
- **Screenshots**: `./gradlew :app:testDebugUnitTest --tests '*ScreenRenderTest*'` renders every screen (including
  full-length, demo-data and dark-mode variants) to `app/build/screenshots/`.

## Deliberate deviations from the blueprint

- **`JointSpec`** keeps the §8.2 fields but replaces the three landmark ints with a `JointGeometry`, because §7.2
  also needs front/back (side-dependent) joints, L/R averages and angles against vertical/horizontal.
- **Knee angles above 180°**: a straight knee whose vertex sits behind the hip–ankle line in depth is reported as
  360° − θ, so the §4 hyperextension guard (>182°) and "do not lock your knee" cues can actually fire.
- **Weekly trend is two stacked panels** (quality 0–100 above, stress & mood 1–10 below, shared week axis), not a
  dual-axis plot: two y-scales on one plot let arbitrary axis ranges suggest relationships that are not there.
- **Safety beats score**: while a safety rule is violated the hold timer and praise are paused, even if alignment
  is green.
- **Coach**: when a pose lock announcement and a ready correction coincide they are spoken together, so the knee cue
  in the demo is heard while the pose is still wrong.
- **Preview uses FIT_CENTER** (whole analysed frame visible) so "full body in frame" matches what the user sees.
