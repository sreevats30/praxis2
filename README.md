# Yoga Motor-Skill & Mental Wellness Companion

Offline Android app built from [`blueprint.md`](blueprint.md) (v5): live pose scoring for 4 asanas + Tadasana
calibration, an offline voice coach, daily/fortnightly wellness check-ins with a crisis guardrail, honest
statistics, and a 1-page PDF report. Everything runs on the phone; the `INTERNET` permission is stripped.

Version 1.1 adds demo profiles that never touch your own data, automatic session saving, a session History,
and a **Learn** library of 32 poses with animated stick-figure demos.

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

## What's in the app (v1.1)

The bottom bar has four tabs: **Home**, **Learn**, **History** and **Insights**. Practice, check-ins, questionnaires
and Settings open full-screen without the bar.

- **Live scoring covers exactly 4 poses:** Vrikshasana (Tree), Virabhadrasana II (Warrior II), Trikonasana
  (Triangle) and Utkatasana (Chair). Tadasana (Mountain) is the calibration pose and is never scored. The camera
  does not recognise any other pose.
- **Learn** lists 32 poses in six everyday categories with search and filters (All, Live-scored, Desk, Morning,
  Standing, Balance, Hips, Evening). Each pose opens an animated stick-figure demo (no video, no images: it is drawn
  procedurally from joint angles) with Front / 45° / Side views, 0.5× / 1× speed, pause at the peak, angle callouts,
  a breathing pacer, steps, common mistakes, easier options and a safety note. The 4 live poses have a
  **Practice this live** button. It opens Practice with a "Target" hint, but scoring is the same for every pose.
  The other 28 are follow-along demos only and are never scored or recorded. With *Remove animations* switched
  on in Android's accessibility settings, demos start paused at the peak pose.
- **Automatic session saving:** every scored pose you hold (locked) for **at least 2.0 seconds** is saved, whatever
  its score. The session is checkpointed every 5 s and again when you press Home, lock the screen, switch camera or
  start a replay, so a crash or a closed app loses at most a few seconds. A red **REC** badge shows the running
  session. **Finish & Save** shows a receipt, and **View History** opens the saved session. Replays are never
  recorded.
- **History** lists every session, newest first: duration, alignment and stability, per-pose hold times, the worst
  joint and the coaching cue. You can delete a session (with confirmation). Sessions logged by hand in older
  versions still show a **Manual** tag.
- **Demo profiles:** after loading demo data in **Settings → Demo data**, profile chips on Insights and History switch
  between **My Account** and three synthetic personas (Demo A · Consistent, Demo B · Exam Gap, Demo C · Null Pattern).
  A new account shows no chips and no demo data. A persona is generated the first
  time you view it and stored **beside** your data, never instead of it. While a demo is shown, an amber banner
  reads *"Viewing demo data (…). Your own data is safe."* with **Back to My Account**. Exported PDFs of a demo
  are labelled DEMO DATA. Anything you record (practice, check-ins, questionnaires) always goes to My Account.
  **Settings → Remove demo data** deletes only the personas. **Delete All My Data** still erases everything.
- **Upgrading from 1.0** keeps your data: the database migrates in place (Room v1 → v2) and never falls back to a
  destructive reset.

## Stage demo script

1. **First run** → accept the consent & safety screen (three boxes).
2. **Insights tab → profile chip "Demo A · Consistent"** shows 42 days of synthetic history (B = exam gap,
   C = null pattern). The amber banner confirms your own data is untouched. Tap **Back to My Account** before
   practising.
3. **Start practice** → prop the phone upright 2–3 m away. The pre-flight pills must all turn green
   (full body, distance, phone steady ≤15° tilt, lighting).
4. Stand in **Tadasana** for 5 s → "Calibrated".
5. **Warrior II with a shallow front knee (~125°)** → red 41% score ring, curved arrow on the knee, voice:
   *"Bend your front knee deeper toward 90 degrees"*. Tap **∑ Explain Score** to show every term of the formula.
6. Bend to ~95° → green, hold timer runs, chime + *"Beautiful. Hold it there."* after 4 s at ≥88%.
7. Do the other side → left-vs-right symmetry. Tap **Finish & Save** → receipt → **View History**: the session
   and both sides are listed with hold times, alignment and the worst joint. (It is saved even if you just press
   Home instead.)
8. **Insights tab** → switch to **Demo A** → weekly trend, symmetry, insight cards (ρ, p, n + caveats) →
   **Export PDF** (marked DEMO DATA) → share. Switch back to **My Account** to show your own session.
9. **Mood reflection (PHQ-9)** → answering item 9 with "Several days" opens the Tele-MANAS 14416 / 112 sheet.
10. **Learn tab** → *14. Warrior II* → the stick figure animates into the pose with angle callouts (front knee 95°,
    arms level). Switch Front / 45° / Side, then **Practice this live**.
11. Privacy proof: Android Studio → `app/src/main/AndroidManifest.xml` → **Merged Manifest** tab shows only
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
| Profiles + demo data (v1.1) | `data/ProfileIds.kt`, `data/ActiveProfile.kt`, `data/ProfileSwitcher.kt`, `ui/profile/` |
| Room 1→2 migration | `data/db/WellnessDatabase.kt` (`MIGRATION_1_2`), `app/schemas/` |
| Session auto-recording | `data/SessionAutoRecorder.kt`, `wellness/DailyAggregator.kt` (`SessionSummarizer`) |
| History | `ui/history/`, `wellness/HistoryAggregator.kt` |
| Learn / Asana Codex | `codex/` (rig, solver, timeline, 32-pose catalog), `ui/codex/` |
| Design system (colours, type, spacing, radii) | `ui/theme/Theme.kt`, `ui/theme/Type.kt` |
| Shared UI components + artwork | `ui/components/Kit.kt`, `ui/components/Artwork.kt` |
| Home summary (streak, week, sparklines) | `wellness/ActivitySummary.kt`, `ui/home/` |

## Design system

**Direction — "practice room".** A dark, warm-neutral interface. The app is used with the phone propped up
across a room, often in low light, and the camera screen is dark by nature, so every screen shares one dark
identity (the app is dark-only). The look comes from typography, spacing and restraint rather than decoration.

- **Colour** (`ui/theme/Theme.kt`, `AppTheme.colors`):
  - *Neutrals* are warm ink, not slate: `canvas` (page), `raised` (sheets, dialogs), `control` (inputs, selector
    tracks, media tiles), `line` / `lineStrong` (hairlines, outlines) and three text steps.
  - *Ivory* (`inverse`) is the action colour: primary buttons and the selected option of a selector.
  - *Jade* (`accent`) means "on target": active tab, progress, aligned joints, positive results.
  - *Amber* and *coral* are reserved for caution and problems; score bands use jade / amber / coral.
  - *Data series* (mood, stress, sleep, practice quality) have their own hues and appear only in charts and meters.
- **Type** (`ui/theme/Type.kt`, both SIL OFL and bundled — the app has no internet permission):
  - *Newsreader* (serif) for what you read as language: screen titles, the greeting, pose names, the findings
    on Insights. Sanskrit names use its italic (`SanskritStyle`).
  - *Inter* for everything you operate or measure: labels, controls, lists, and numbers — always with tabular
    figures (`NumericStyle`). Section headings are small, semibold and sentence case (`SectionStyle`).
- **Space and shape**: a 4-point scale with a 20 dp page gutter (`Space`); radii are 4 dp for small marks, 8 dp
  for anything you press or type into, 12 dp for media and sheets (`Radius`). Selectors use concentric radii.
- **Structure**: content is grouped into `Section`s separated by full-width hairlines and spacing — not cards.
  Raised fills are kept for controls, media tiles, sheets and dialogs. Lists run edge to edge (`ListRow`).
- **Screens** (`ui/components/Kit.kt`): `AppScreen` / `AppLazyScreen` give every screen the same slim bar; top-level
  screens open with a large serif title that hands over to the bar's small title on scroll.
- **Components**: `PrimaryButton` (ivory), `SecondaryButton` (outline), `TextAction`, `IconAction`, `Segmented`,
  `Chip`, `ScaleSelector` (1–10 rating), `SearchField`, `CheckRow`, `RadioRow`, `AppSwitch`, `Stat`, `Meter`,
  `SegmentMeter`, `Ring`, `Tag`, `StatusLabel`, `Notice` (a margin rule, not a box), `EmptyState`, `LoadingState`;
  artwork in `Artwork.kt` is the `PoseGlyph` line figure.
- **Navigation**: a hairline-topped bottom bar; the active tab has a filled icon, full-strength label and a short
  jade bar on the top edge — never colour alone, no pill.
- **Camera HUD**: translucent ink panels over the preview with the same ivory text, jade and score colours.
- **Screenshots**: `./gradlew :app:testDebugUnitTest --tests '*ScreenRenderTest*'` renders every screen (including
  full-length, narrow-phone, demo-data and 2× font variants) to `app/build/screenshots/`.

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
- **Learn demos use a one-segment spine.** Spinal curves (Cat-Cow, Puppy, Cobra) are shown through the head, hip and
  arm angles. Each pose's notes are in a code comment in `codex/Catalog*.kt`, and `ForwardKinematicsTest` checks
  every callout against the solver within ±3° (±5° for twist or whole-body-rotation poses).
