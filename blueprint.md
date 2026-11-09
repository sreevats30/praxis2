# Architecture & Technical Blueprint v5 (Final Verified Build Spec): Yoga Motor-Skill & Mental Wellness Companion

## 0. Verification Log: Every Bug Caught & Fixed (v4 → v5 Final)

This v5 document is **mathematically verified and locked for immediate implementation** using **Antigravity IDE + Claude Code**. Every bug identified in the v3 audit has been resolved:

| # | Bug / Concern Identified in Review | Exact Mathematical / Engineering Fix in v4 |
| :--- | :--- | :--- |
| **1** | **Warrior II Demo Numbers Didn't Match Formula** (`125°` knee gave `87%` score instead of failing, because $d_{\max}=45^\circ$ and cap $40 + s_{\min}$ was too loose) | **Recalibrated & Verified (§7.1 & §7.4):** Tightened critical joint $d_{\max} = 30^\circ$ and updated Critical-Joint Cap to $S_{\text{align}} = \min(S_{\text{weighted}}, \, 15.0 + 0.85 \times s_{\text{min\_crit}})$. Now a $125^\circ$ knee in Warrior II ($\mu=95^\circ, \tau=8^\circ, d_{\max}=30^\circ$) yields $\Delta = 22^\circ \rightarrow s_{\text{knee}} = 31.1\% \rightarrow \text{Cap} = \mathbf{41.4\%}$ (fails the $75\%$ hold threshold and triggers immediate red cue!). |
| **2** | **Gravity Rotation Matrix Had Wrong Off-Diagonal Signs & Missing Axis Flip** | **Removed (§5.1 & §8.1):** The rotation-matrix correction is dropped. The **Pre-Flight IMU Tilt Guard** (uses `Sensor.TYPE_ACCELEROMETER` so it works on $100\%$ of budget phones) is the only gravity handling: if phone pitch/roll exceeds $\pm 15^\circ$, the user is asked to re-prop the phone instead of the app correcting landmarks. |
| **3** | **Utkatasana (Chair Pose) vs. $Z$-Damping (`0.25f`) Conflict** | **Per-Asana Camera Angle & `zWeight` (§4 & §7.2):** In Chair Pose, knee flexion occurs in the sagittal plane. We prompt the user to turn **$45^\circ$ or Side-On** for Utkatasana, where the flexion lives cleanly in the $(X, Y)$ image plane! |
| **4** | **MLP Train/Inference Mismatch & Colab Time Sink** | **Rules + Prototype-Centroid Classifier First (§6):** The Problem Statement explicitly permits *"rules, k-NN or a small neural network"*. We ship a deterministic **Geometric Rule + Prototype Distance Classifier** in pure Kotlin on Hour 2 (zero training lag, $100\%$ explainable). If you want the neural net later, the $3,783$-parameter MLP takes 10 minutes to train on recorded JSONL clips. |
| **5** | **Plank (`Phalakasana`) is Strenuous & Prone to Floor Occlusion** | **Removed Plank (§4):** We use **4 core scored asanas** (`Vrikshasana`, `Virabhadrasana II`, `Trikonasana`, `Utkatasana`) + **`Tadasana`** (calibration/neutral) = **5 supported poses**, satisfying the PS requirement of **5–8 asanas** with zero inversions and zero strenuous floor holds. |
| **6** | **Deprecated SQLCipher, Missing Consent / Pain / Schedule Details, Autocorrelation Caveat, Speculative Insight Text** | **All Fixed (§2, §10, §11):** Uses standard `androidx.room` with `android:allowBackup="false"` (and optional current `net.zetetic:sqlcipher-android:4.5.6`), adds the First-Run Consent & Physical Safety Screen, schedules PHQ-9/GAD-7 **every 14 days**, states **Monte Carlo permutation test ($5,000$ shuffles)** with autocorrelation caveat, and removes speculative causal phrasing. |

---

## 1. Realistic Build Velocity with Antigravity IDE + Claude Code

Why a "2-week manual project" fits in **24–36 hours** for 2–3 people with **Antigravity IDE + Claude Code**:
In traditional Android development, teams lose 60% of their time fighting CameraX boilerplate, writing custom Compose Canvas drawing math, debugging Room schemas, and formatting PDFs. Because this blueprint gives you **exact Kotlin data structures, verified formulas, and a modular package tree**, you can run **parallel Claude Code / Antigravity IDE sessions** where each prompt generates a complete, compilable vertical module in 15–20 minutes.

* **Realistic Total Budget (2–3 people):** **24–36 hours.** AI-generated code compiles quickly (roughly 15 hours of prompting and integration), but tuning thresholds on a real phone and a real body takes the rest.
* **Recommended Keep List (100% Achievable in 24–36h with AI Coding):**
  * **Keep:** CameraX + MediaPipe (`full.task`), Angle-Only $\text{1€ Filter}$, Rule + Centroid Classifier (4 Core Asanas + Tadasana), `AsanaSpecs.kt` Scoring + Sway + Hold State Machine, English TTS Coach, `"∑ Explain Score"` Live Bottom Sheet (takes Claude Code 10 mins to generate and wins judges!), Daily Check-In + Fortnightly PHQ-9/GAD-7 + Tele-MANAS `14416` Crisis Guard, Tie-Corrected Spearman Dashboard with 3 Seeded Personas, 1-Page Native `PdfDocument` Export, and JSONL Record/Replay Harness.
  * **Dropped (Zero Judge Penalty, Saves Headache):** Plank Pose, Bhujangasana (Cobra), Hindi TTS voice pack checks, Health Connect OAuth permissions, and raw-coordinate 3D gravity warping (replaced by the rock-solid IMU Pre-Flight Tilt Guard).

---

## 2. Locked Tech Stack & Gradle Dependencies

| Component | Exact Library / API | Why It Is Zero-Risk |
| :--- | :--- | :--- |
| **UI & Canvas** | **Kotlin 2.0 + Jetpack Compose (Material 3)** | Single-activity Compose architecture; custom `Canvas` overlay renders skeleton, angle arcs, and correction arrows smoothly. |
| **Camera** | `androidx.camera:camera-camera2:1.3.4`, `camera-lifecycle`, `camera-view` | `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` at `640x480` `RGBA_8888`. |
| **Pose Model** | `com.google.mediapipe:tasks-vision:0.10.14` | Bundle `pose_landmarker_full.task` in `app/src/main/assets/`. Runs in `LIVE_STREAM` mode at 20–30 Hz on mid-range phones. |
| **Sensors** | Android `Sensor.TYPE_ACCELEROMETER` | Universal across all Android devices (even budget phones without a gyroscope); powers the Pre-Flight Phone Tilt & Steadiness Guard. |
| **Smoothing** | **One-Euro Filter (`OneEuroFilter.forDegrees()`)** | Smooths the 12 core joint angles directly (`minCutoff = 1.2 Hz, beta = 0.04`), eliminating score jitter with $< 15$ lines of math. |
| **Database** | `androidx.room:room-ktx:2.6.1` (`allowBackup="false"`) | Local on-device SQLite storage. Optional drop-in: `net.zetetic:sqlcipher-android:4.5.6` with `SupportOpenHelperFactory`. |
| **Voice & Audio** | `android.speech.tts.TextToSpeech` + `ToneGenerator` | Built-in Android SDK APIs—zero external dependencies. |
| **PDF Export** | `android.graphics.pdf.PdfDocument` | Built-in Android SDK class that renders a 1-page Weekly Report directly to PDF. |

### 2.1 Privacy Proof in `AndroidManifest.xml` (How to Show It on Stage)
Because some third-party SDKs silently merge `INTERNET` permission, add this explicit removal rule to your `app/src/main/AndroidManifest.xml`. The `tools` namespace must be declared on the `<manifest>` tag or the build will fail:
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.CAMERA" />
    <!-- Explicitly strip INTERNET permission even if a transitive library requests it -->
    <uses-permission android:name="android.permission.INTERNET" tools:node="remove" />

    <!-- <application ... android:allowBackup="false"> ... -->
</manifest>
```
During the demo, show the **merged manifest** (Android Studio: open `AndroidManifest.xml` → **Merged Manifest** tab) to prove the app requests **only Camera** and declares no `INTERNET` permission. Do not use Android App Info for this: it does not display a "no internet" status.

---

## 3. System Architecture

```
+-----------------------------------------------------------------------------------+
|               ANDROID DEVICE (100% OFFLINE — INTERNET PERMISSION STRIPPED)        |
|                                                                                   |
|  [First-Run Consent, Non-Diagnostic Disclaimer & Physical Safety Check Screen]    |
|         │                                                                         |
|         ▼                                                                         |
|  [CameraX Analyzer: KEEP_ONLY_LATEST, 640x480, Un-mirrored + Rotation Degrees]    |
|         │                                                                         |
|         ▼                                                                         |
|  +-----------------------------------------------------------------------------+  |
|  | PRE-FLIGHT GUARD (Must be Green to Start Scoring)                           |  |
|  |  1. Full Body Visible: Nose(0), Wrists(15,16), Ankles(27,28) vis >= 0.60    |  |
|  |  2. Distance Framing: Torso height is 18% to 38% of frame height            |  |
|  |  3. Phone Upright & Steady: Accelerometer tilt <= 15 deg, motion < 0.3 m/s² |  |
|  |  4. Lighting Check: Mean frame luminance >= 50 (0-255)                      |  |
|  +-----------------------------------------------------------------------------+  |
|         │                                                                         |
|         ▼  [Background Pipeline Coroutine: Dispatchers.Default]                   |
|  +-----------------------------------------------------------------------------+  |
|  | STAGE 1: MEDIAPIPE POSE + ANGLE EXTRACTION + 1€ FILTER                      |  |
|  |  - MediaPipe PoseLandmarker (LIVE_STREAM) -> 33 Image + 33 World Landmarks  |  |
|  |  - Compute 12 core biomechanical joint angles (with per-asana zWeight)      |  |
|  |  - Filter angles via OneEuroFilter.forDegrees(minCutoff=1.2, beta=0.04)     |  |
|  +-----------------------------------------------------------------------------+  |
|         │                                                │                        |
|         ▼ (Smoothed Angles + Normalized Landmarks)       ▼ (Mirrored X for UI)    |
|  +-------------------------------------------+    +----------------------------+  |
|  | STAGE 2: RULES + CENTROID CLASSIFIER      |    | COMPOSE CANVAS OVERLAY     |  |
|  |  - Deterministic Geometric Rule Classifier|    |  - Color-coded skeleton    |  |
|  |  - Automatic Left / Right Side Detector   |    |  - Joint angle degree tags |  |
|  |  - Temporal Lock: 14 of 18 frames (~0.6s) |    |  - Curved corrective arrows|  |
|  |    with 1.0s unlock hysteresis            |    |  - "∑ Explain Score" Sheet |  |
|  +-------------------------------------------+    +----------------------------+  |
|         │                                                                         |
|         ▼ (Locked Asana + Detected Side)                                          |
|  +-----------------------------------------------------------------------------+  |
|  | STAGE 3: RECALIBRATED BIOMECHANICAL SCORING ENGINE                          |  |
|  |  - Alignment (0-100): Deadband + Power(1.2) penalty, visibility-renormalized|  |
|  |    weights, Tightened Cap: S_align = min(S_weighted, 15 + 0.85 * s_min_crit)|  |
|  |  - Stability (0-100): Detrended hip-midpoint sway SD normalized by          |  |
|  |    calibrated/median torso length L_torso                                   |  |
|  |  - Hold Timer: Reports BOTH Strict Continuous Hold & Grace-Adjusted Hold    |  |
|  |  - Symmetry: Intra-pose (L vs R joints) & Inter-pose (L vs R session holds) |  |
|  +-----------------------------------------------------------------------------+  |
|         │                                                │                        |
|         ▼                                                ▼                        |
|  +-------------------------------------------+    +----------------------------+  |
|  | STAGE 4: RATE-LIMITED FEEDBACK ENGINE     |    | STAGE 5: WELLNESS & STATS  |  |
|  |  1. Safety Rules First (Tree knee, etc.)  |    |  - 30s Daily Sliders       |  |
|  |  2. Worst Weighted Joint (>1.2s persist)  |    |  - Bi-Weekly PHQ-9 / GAD-7 |  |
|  |  3. 4.5s TTS Cooldown + Max 2x Repeat     |    |    + Tele-MANAS 14416 Guard|  |
|  |  4. Praise + Chime on >=88% Hold for 4s   |    |  - Tie-Corrected Spearman  |  |
|  |  5. Periodic "Stop if you feel pain" cue  |    |    + 5,000-Shuffle Monte   |  |
|  +-------------------------------------------+    |    Carlo p-value           |  |
|                                                   +----------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 4. The 4 Core Asanas + Tadasana Calibration Roster

This roster satisfies the **5–8 asanas** requirement, covers standing balance, asymmetric strength, lateral hinge, and bilateral squat (**Side-On Camera stretch goal**), and avoids inversions or dangerous floor strains:

| # | Asana | Camera Angle & `zWeight` | Variants | What It Demonstrates in Demo | Built-In Physical Safety Rule |
| :-: | :--- | :--- | :---: | :--- | :--- |
| **0** | **Tadasana** *(Mountain Pose)* | **Front** (`zWeight = 0.25f`) | Neutral | 5-second body proportion calibration ($\tilde{L}_{\text{torso}}$) & rest state between poses | None (Neutral standing baseline) |
| **1** | **Vrikshasana** *(Tree Pose)* | **Front** (`zWeight = 0.25f`) | Left & Right | Single-leg sway stability, hold timer, **L-vs-R symmetry comparison** | **MCL Knee Guard:** Lifted ankle must not press directly against the standing knee joint ($\lambda_{\text{tree}} \notin [0.88, 1.12]$). |
| **2** | **Virabhadrasana II** *(Warrior II)* | **Front** (`zWeight = 0.25f`) | Left & Right | **Primary "Deliberately Wrong $\rightarrow$ Live Correction" Demo** (shallow front knee $125^\circ \rightarrow 95^\circ$) | **Knee-Over-Ankle Guard:** Front knee horizontal coordinate must not overshoot past the front ankle by $> 15\%$ of lower-leg length. |
| **3** | **Trikonasana** *(Triangle Pose)* | **Front** (`zWeight = 0.25f`) | Left & Right | Lateral spine tilt, straight knees, vertical arm stacking | **Hyperextension Guard:** Front knee angle must not exceed $182^\circ$. |
| **4** | **Utkatasana** *(Chair Pose)* | **$45^\circ$ or Side-On** (`zWeight = 0.15f`) | Symmetric | Bilateral knee/hip flexion & left-right intra-pose symmetry (*solves the sagittal depth problem cleanly!*) | **Endurance & Back Guard:** Hold capped at $30\text{ s}$; prompt user to stop if lower back or knees ache. |

*(Optional 6th scored asana if desired: **Virabhadrasana I / High Lunge** from $45^\circ$/Side-On—uses the exact same front/back knee rules as Warrior II with arms raised overhead (`Shoulder Flexion = 165°`).)*

---

## 5. Pre-Flight Guard & Camera / IMU Handling

### 5.1 Pre-Flight Framing & Tilt Checklist
Before the scoring engine activates, `PreflightGuard.kt` checks 4 fast conditions and displays a clean status pill at the top of the camera screen:
1. **Whole Body in Frame:** Landmarks `0` (Nose), `15/16` (Wrists), and `27/28` (Ankles) have `visibility >= 0.60` and normalized image coordinates $x, y \in [0.04, 0.96]$.
2. **Camera Distance Check:** 2D torso length $\Vert P_{\text{mid\_shldr}} - P_{\text{mid\_hip}} \Vert_2$ is between $18\%$ and $38\%$ of frame height ($1.8\text{ m}$ to $3.0\text{ m}$).
3. **Phone Tilt & Steadiness (`Sensor.TYPE_ACCELEROMETER`):**
   * Pitch tilt angle from upright: $\theta_{\text{pitch}} = \vert \text{atan2}(a_z, \sqrt{a_x^2 + a_y^2}) \vert \le 15^\circ$. If tilted too far backward/forward on the floor, prompts: *"Prop your phone slightly more upright ($<15^\circ$ tilt) for accurate vertical alignment."*
   * Device movement variance $< 0.3\text{ m/s}^2$ (ensures phone is propped up, not held in a shaking hand).
4. **Lighting Check:** Mean luminance $Y$ of the center $50\%$ crop $\ge 50$ (out of $255$).

---

## 6. Rules-First + Prototype Asana Classifier (`RuleAndCentroidClassifier.kt`)

Why this beats training a black-box neural network on noisy web images during a hackathon:
1. **Zero Train/Test Domain Shift:** It uses the **exact same gravity-checked angles and torso-normalized distances** that your scoring engine uses.
2. **Works at Hour 2:** No waiting for Yoga-82 downloads or Colab epochs; works across all body heights/builds out of the box because every rule is purely angular or torso-normalized.
3. **100% Explainable to Judges:** You can show the exact decision tree AND prototype distance score.

### 6.1 Deterministic Classification Decision Table
Evaluated on every frame using the smoothed angles and torso-normalized positions ($L_{\text{torso}} = \Vert P_{\text{mid\_shldr}} - P_{\text{mid\_hip}} \Vert_2$):

| Priority | Asana Class | Geometric Classification Condition (All Scale-Invariant) | Automatic Side Detection (`LEFT` vs `RIGHT`) |
| :-: | :--- | :--- | :--- |
| **1** | **Vrikshasana** *(Tree Pose)* | One foot lifted: vertical ankle separation $\vert y_{\text{ankle, L}} - y_{\text{ankle, R}}\vert > 0.22 L_{\text{torso}}$ AND lifted ankle is near the standing leg horizontally ($\Vert x_{\text{ankle, lifted}} - x_{\text{knee, standing}}\Vert < 0.35 L_{\text{torso}}$) AND standing knee $> 155^\circ$. | If $y_{\text{ankle, L}} < y_{\text{ankle, R}}$ (Left foot higher in image): `LEFT_LIFTED`, else `RIGHT_LIFTED` |
| **2** | **Utkatasana** *(Chair Pose)* | Both feet on floor ($\vert y_{\text{ankle, L}} - y_{\text{ankle, R}}\vert < 0.15 L_{\text{torso}}$), narrow-to-hip stance ($\text{stanceWidth} < 0.85 L_{\text{torso}}$), AND **both knees flexed** ($\theta_{\text{knee, L}} \in [75^\circ, 148^\circ]$ and $\theta_{\text{knee, R}} \in [75^\circ, 148^\circ]$). | `SYMMETRIC` |
| **3** | **Virabhadrasana II** *(Warrior II)* | Wide stance ($\text{stanceWidth} > 1.10 L_{\text{torso}}$), upright trunk ($\theta_{\text{trunk\_tilt}} < 25^\circ$), one knee flexed ($\theta_{\text{knee, front}} \in [70^\circ, 142^\circ]$), back knee straight ($\theta_{\text{knee, back}} > 152^\circ$), AND arms abducted ($\theta_{\text{shldr, L/R}} \in [60^\circ, 120^\circ]$). | Side with smaller knee angle ($\min(\theta_{\text{knee, L}}, \theta_{\text{knee, R}})$) is `FRONT_LEFT` or `FRONT_RIGHT` |
| **4** | **Trikonasana** *(Triangle Pose)* | Wide stance ($\text{stanceWidth} > 1.00 L_{\text{torso}}$), **both knees straight** ($\theta_{\text{knee, L}} > 152^\circ$ and $\theta_{\text{knee, R}} > 152^\circ$), AND lateral trunk tilt $\theta_{\text{trunk\_tilt}} \in [35^\circ, 95^\circ]$. | Side with lower wrist ($y_{\text{wrist}}$ larger) is `LEADING_LEFT` or `LEADING_RIGHT` |
| **5** | **Tadasana** *(Mountain / Neutral)* | Standing upright ($\theta_{\text{trunk\_tilt}} < 15^\circ$), both knees $> 162^\circ$, stance width $< 0.75 L_{\text{torso}}$, arms relaxed by sides ($\theta_{\text{shldr}} < 35^\circ$). | `NEUTRAL_CALIBRATION` |

### 6.2 Temporal Hysteresis Lock
To prevent flicker when moving between poses:
* **Lock Condition:** The same asana label must be detected in **$\ge 14$ of the last $18$ frames** ($\approx 0.6\text{ s}$ at $25\text{ FPS}$).
* **Unlock Condition:** Once locked into an asana, the user stays in that asana's scoring mode until the pose is rejected for **$25$ consecutive frames** ($\approx 1.0\text{ s}$). This guarantees that adjusting a knee or arm during a pose never drops the lock!

---

## 7. Verified Alignment Scoring Formula & Complete `AsanaSpecs.kt`

### 7.1 Recalibrated Formula (Verified Against Demo Script Numbers!)

For each joint $i$ in the locked asana, with user profile tolerance multiplier $m_\tau \in \{1.35\text{ (Beginner)}, 1.0\text{ (Standard)}, 0.75\text{ (Advanced)}\}$:

1. **Deadband Excess Deviation ($\Delta_i$):**
   $$\Delta_i = \max\left(0, \, \vert\theta_i - \mu_i\vert - m_\tau \tau_i\right)$$
2. **Joint Score ($s_i \in [0, 100]$):**
   $$s_i = 100 \times \max\left(0, \, 1 - \left(\frac{\Delta_i}{d_{\max, i}}\right)^{1.2}\right)$$
3. **Visibility-Renormalized Weighted Mean ($S_{\text{weighted}}$):** Over visible joints $U$ (where all 3 landmarks have `visibility >= 0.55`):
   $$S_{\text{weighted}} = \frac{\sum_{i \in U} w_i s_i}{\sum_{i \in U} w_i}$$
4. **Tightened Critical-Joint Cap ($S_{\text{align}}$):**
   $$S_{\text{align}} = \min\left(S_{\text{weighted}}, \, 15.0 + 0.85 \times \min_{i \in U_{\text{critical}}} s_i\right)$$

#### Proof Table (Why This Cap Formula Is Mathematical Gold on Stage)
Let's test **Virabhadrasana II (Warrior II)** where all other joints are perfect ($100\%$), while the user varies their **Front Knee** ($\mu = 95^\circ, \tau = \pm 8^\circ, d_{\max} = 30^\circ, w = 0.30, \text{critical} = \text{true}$):

| User's Front Knee Angle $\theta_{\text{knee}}$ | Excess Deviation $\Delta$ | Front Knee Score $s_{\text{knee}}$ | Uncapped Weighted Avg $S_{\text{weighted}}$ | Critical Cap ($15 + 0.85 s_{\text{knee}}$) | **Final Displayed Score $S_{\text{align}}$** | Hold Timer Status ($\ge 75\%$?) |
| :-: | :-: | :-: | :-: | :-: | :-: | :--- |
| **$130^\circ$ (Very shallow / lazy)** | $27^\circ$ | $11.9\%$ | $73.6\%$ | **$25.1\%$** | **$25\%$ (Red)** | Paused — Speaks knee cue |
| **$125^\circ$ (Stage Demo Wrong Pose)** | $22^\circ$ | $31.1\%$ | $79.3\%$ | **$41.4\%$** | **$41\%$ (Red)** | Paused — Speaks knee cue |
| **$115^\circ$ (Halfway corrected)** | $12^\circ$ | $66.7\%$ | $90.0\%$ | **$71.7\%$** | **$72\%$ (Amber)** | Paused — Close to tolerance |
| **$108^\circ$ (Slightly high, playable)** | $5^\circ$ | $88.4\%$ | $96.5\%$ | $90.1\%$ | **$90\%$ (Green)** | **HOLDING (Timer runs!)** |
| **$96^\circ$ (Inside $95^\circ \pm 8^\circ$ deadband)** | $0^\circ$ | $100.0\%$ | $100.0\%$ | $100.0\%$ | **$100\%$ (Green)** | **HOLDING + Praise Chime!** |

### 7.2 Complete Master Reference Table (`AsanaSpecs.kt`)

| Asana | Joint Name | Landmarks `(A, Vertex B, C)` | Target $\mu_i$ | Tol $\tau_i$ | $d_{\max}$ | Weight $w_i$ | Critical? | Spoken Cue When $\theta_i > \mu_i + \tau_i$ | Spoken Cue When $\theta_i < \mu_i - \tau_i$ |
| :--- | :--- | :--- | :-: | :-: | :-: | :-: | :-: | :--- | :--- |
| **1. Vrikshasana** *(Tree Pose)* | Standing Knee | `F_Hip, F_Knee, F_Ankle` | $178^\circ$ | $\pm 7^\circ$ | $30^\circ$ | `0.30` | **Yes** | *"Keep a micro-softness; do not lock your standing knee"* | *"Firm and straighten your standing leg"* |
| | Lifted Knee Flexion | `B_Hip, B_Knee, B_Ankle` | $75^\circ$ | $\pm 20^\circ$ | $45^\circ$ | `0.20` | No | *"Draw your lifted foot slightly higher"* | *"Relax your lifted foot angle"* |
| | Pelvic Level | `L_Hip—R_Hip vs Horizontal` | $0^\circ$ | $\pm 6^\circ$ | $22^\circ$ | `0.20` | **Yes** | *"Level your hips horizontally"* | *"Level your hips so neither side drops"* |
| | Torso Verticality | `MidShldr—MidHip vs Vertical` | $0^\circ$ | $\pm 5^\circ$ | $22^\circ$ | `0.20` | **Yes** | *"Stand tall and stack your shoulders over your hips"* | *"Bring your spine upright"* |
| | Overhead Elbows | `Shldr, Elbow, Wrist (Avg)` | $168^\circ$ | $\pm 12^\circ$ | $40^\circ$ | `0.10` | No | *"Relax your shoulders away from your ears"* | *"Reach your arms straighter overhead"* |
| **2. Virabhadrasana II** *(Warrior II)* | Front Knee | `F_Hip, F_Knee, F_Ankle` | $95^\circ$ | $\pm 8^\circ$ | $30^\circ$ | `0.30` | **Yes** | *"Bend your front knee deeper toward 90 degrees"* | *"Ease up slightly so your knee stays above your ankle"* |
| | Back Knee | `B_Hip, B_Knee, B_Ankle` | $175^\circ$ | $\pm 7^\circ$ | $30^\circ$ | `0.20` | **Yes** | *"Avoid hyperextending your back knee"* | *"Straighten and anchor your back leg"* |
| | Front Arm Level | `F_Hip, F_Shldr, F_Elbow` | $90^\circ$ | $\pm 7^\circ$ | $35^\circ$ | `0.15` | No | *"Lower your front arm to shoulder height"* | *"Lift your front arm up to shoulder level"* |
| | Back Arm Level | `B_Hip, B_Shldr, B_Elbow` | $90^\circ$ | $\pm 7^\circ$ | $35^\circ$ | `0.15` | No | *"Lower your back arm to shoulder height"* | *"Raise your back arm to shoulder height"* |
| | Torso Verticality | `MidShldr—MidHip vs Vertical` | $0^\circ$ | $\pm 6^\circ$ | $25^\circ$ | `0.20` | **Yes** | *"Bring your torso upright; avoid leaning over your front leg"* | *"Center your chest directly above your hips"* |
| **3. Trikonasana** *(Triangle Pose)* | Front Knee | `F_Hip, F_Knee, F_Ankle` | $175^\circ$ | $\pm 7^\circ$ | $30^\circ$ | `0.25` | **Yes** | *"Keep a soft micro-bend in your front knee"* | *"Straighten your front leg as you hinge sideways"* |
| | Back Knee | `B_Hip, B_Knee, B_Ankle` | $176^\circ$ | $\pm 7^\circ$ | $30^\circ$ | `0.20` | No | *"Keep your back leg steady"* | *"Firm and straighten your back leg"* |
| | Trunk Lateral Tilt | `MidShldr—MidHip vs Vertical` | $68^\circ$ | $\pm 10^\circ$ | $32^\circ$ | `0.30` | **Yes** | *"Come up slightly to keep your lower spine long"* | *"Hinge deeper sideways from your front hip"* |
| | Vertical Arm Line | `F_Wrist, MidShldr, B_Wrist` | $174^\circ$ | $\pm 8^\circ$ | $35^\circ$ | `0.15` | No | *"Stack your top arm directly over your shoulder"* | *"Open your chest and reach your top arm straight up"* |
| | Neck Alignment | `Nose, MidShldr, MidHip` | $174^\circ$ | $\pm 8^\circ$ | $30^\circ$ | `0.10` | No | *"Keep your neck long in line with your spine"* | *"Align your head with your spine"* |
| **4. Utkatasana** *(Chair - $45^\circ$/Side)* | Left Knee Flexion | `L_Hip(23), L_Knee(25), L_Ankle(27)` | $112^\circ$ | $\pm 10^\circ$ | $32^\circ$ | `0.25` | **Yes** | *"Sit your hips back and bend your knees deeper"* | *"Raise your hips slightly to protect your knees"* |
| | Right Knee Flexion | `R_Hip(24), R_Knee(26), R_Ankle(28)` | $112^\circ$ | $\pm 10^\circ$ | $32^\circ$ | `0.25` | **Yes** | *"Bend both knees evenly into the chair position"* | *"Raise your hips slightly"* |
| | Hip Flexion | `Shldr, Hip, Knee (Avg L/R)` | $108^\circ$ | $\pm 12^\circ$ | $35^\circ$ | `0.20` | **Yes** | *"Shift your weight toward your heels and crease your hips"* | *"Lift your chest so your torso doesn't rest on your thighs"* |
| | Trunk Forward Lean | `MidShldr—MidHip vs Vertical` | $28^\circ$ | $\pm 10^\circ$ | $30^\circ$ | `0.15` | No | *"Lift your chest higher; avoid leaning too far forward"* | *"Allow a natural forward hinge from the hips"* |
| | Arm Elevation | `Hip, Shldr, Elbow (Avg L/R)` | $160^\circ$ | $\pm 12^\circ$ | $40^\circ$ | `0.15` | No | *"Relax your neck and shoulders"* | *"Reach your arms higher alongside your ears"* |

---

## 8. Production Kotlin Code Blueprints (Mathematically Verified)

### 8.1 Verified IMU Tilt Guard (`GravityAligner.kt`)

```kotlin
package com.companion.yogawellness.pose

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class PhoneTiltStatus(
    val isUprightOk: Boolean,
    val pitchDeg: Double,
    val rollDeg: Double,
    val warningMessage: String?
)

object GravityAligner {

    /**
     * Works on 100% of Android devices using Sensor.TYPE_ACCELEROMETER (ax, ay, az).
     * Checks if phone is propped reasonably upright (within +-15 deg pitch/roll).
     */
    fun checkPhoneTilt(ax: Float, ay: Float, az: Float): PhoneTiltStatus {
        val norm = sqrt((ax * ax + ay * ay + az * az).toDouble())
        if (norm < 1.0) return PhoneTiltStatus(true, 0.0, 0.0, null)

        // In portrait Android, +Y points up out of the top of the phone, +Z out of screen
        val pitchDeg = Math.toDegrees(atan2(az.toDouble(), sqrt((ax * ax + ay * ay).toDouble())))
        val rollDeg = Math.toDegrees(atan2(ax.toDouble(), ay.toDouble()))

        val warn = when {
            abs(pitchDeg) > 15.0 -> "Tilt your phone more upright (currently tilted ${pitchDeg.toInt()}°)"
            abs(rollDeg) > 15.0  -> "Level your phone horizontally"
            else -> null
        }
        return PhoneTiltStatus(warn == null, pitchDeg, rollDeg, warn)
    }
}
```

### 8.2 Verified Recalibrated Alignment Scorer (`AlignmentScorer.kt`)

```kotlin
package com.companion.yogawellness.scoring

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class JointSpec(
    val name: String,
    val landmarkA: Int,
    val vertexB: Int,
    val landmarkC: Int,
    val targetDeg: Double,
    val toleranceDeg: Double,
    val maxDeviationDeg: Double = 30.0,
    val weight: Double,
    val isCritical: Boolean = false,
    val cueWhenTooLarge: String,
    val cueWhenTooSmall: String
)

data class JointEvaluation(
    val spec: JointSpec,
    val measuredDeg: Double,
    val excessDeg: Double,
    val jointScore: Double,
    val weightedDeficit: Double,
    val directionalCue: String?
)

data class AlignmentScoreResult(
    val finalScore: Double?,          // null if < 60% of body weight is visible
    val weightedScore: Double,
    val criticalCapValue: Double,
    val isCappedByCritical: Boolean,
    val evaluations: List<JointEvaluation>,
    val primaryCorrectiveCue: String?
)

object AlignmentScorer {

    fun evaluate(
        smoothedAngles: Map<String, Double>,
        jointVisibilityOk: Map<String, Boolean>,
        specs: List<JointSpec>,
        toleranceMultiplier: Double = 1.0
    ): AlignmentScoreResult {
        val evals = mutableListOf<JointEvaluation>()
        var weightedSum = 0.0
        var activeWeightSum = 0.0
        var minCriticalScore = 100.0
        val totalSpecWeight = specs.sumOf { it.weight }

        for (spec in specs) {
            val theta = smoothedAngles[spec.name] ?: Double.NaN
            if (theta.isNaN() || jointVisibilityOk[spec.name] != true) continue

            val signedDiff = theta - spec.targetDeg
            val effectiveTol = spec.toleranceDeg * toleranceMultiplier
            val excess = max(0.0, abs(signedDiff) - effectiveTol)
            val normalizedPenalty = (excess / spec.maxDeviationDeg).coerceIn(0.0, 1.0).pow(1.2)
            val score = 100.0 * (1.0 - normalizedPenalty)

            val cue = if (excess > 0.0) {
                if (signedDiff > 0.0) spec.cueWhenTooLarge else spec.cueWhenTooSmall
            } else null

            val deficit = spec.weight * (100.0 - score)
            evals += JointEvaluation(spec, theta, excess, score, deficit, cue)

            weightedSum += spec.weight * score
            activeWeightSum += spec.weight
            if (spec.isCritical) minCriticalScore = min(minCriticalScore, score)
        }

        if (activeWeightSum / totalSpecWeight < 0.60) {
            return AlignmentScoreResult(null, 0.0, 100.0, false, evals, "Step back so your full body is visible")
        }

        val rawWeighted = weightedSum / activeWeightSum
        // Tightened Critical-Joint Cap: 125-deg knee in Warrior II (s_knee=31.1) caps total at 41.4%
        val criticalCap = 15.0 + 0.85 * minCriticalScore
        val finalScore = min(rawWeighted, criticalCap).coerceIn(0.0, 100.0)

        val worstJoint = evals.maxByOrNull { it.weightedDeficit }
        val topCue = worstJoint?.takeIf { it.jointScore < 82.0 }?.directionalCue

        return AlignmentScoreResult(
            finalScore = finalScore,
            weightedScore = rawWeighted,
            criticalCapValue = criticalCap,
            isCappedByCritical = criticalCap < rawWeighted - 0.5,
            evaluations = evals.sortedByDescending { it.weightedDeficit },
            primaryCorrectiveCue = topCue
        )
    }
}
```

---

## 9. Wellness, Ethics, Privacy & Statistical Honesty Checklist

Every single ethics and wellness requirement from Pages 2–4 of the Problem Statement is locked in:

1. **First-Run Consent & Physical Safety Screen (`ConsentAndSafetyScreen.kt`):**
   * Explicitly states: *"This app is a wellness and self-reflection companion, not a medical or diagnostic device."*
   * Lists physical safety rules: *"Practice on a non-slip surface. Stop immediately if you feel any sharp pain, dizziness, or joint strain."* (Also repeated as a gentle reminder banner at the start of every session).
   * Requires user consent for local camera processing and storing data only on your device, with a 1-tap **"Delete All My Data"** button in Settings.
2. **Daily Check-In (< 30 Seconds):**
   * 3 clean 1–10 sliders: **Mood**, **Stress**, **Sleep Quality**, plus a 1-tap Pre/Post-Session Tension rating (*"Self-rated tension went from 7 to 4 after today's practice"*).
3. **Bi-Weekly (Fortnightly) PHQ-9 & GAD-7 Questionnaires:**
   * Scheduled **once every 14 days** (matching the official *"Over the last 2 weeks, how often have you been bothered by..."* wording from `phqscreeners.com`).
   * **Non-Diagnostic Presentation:** Displays the raw score ($0\text{–}27$ for PHQ-9, $0\text{–}21$ for GAD-7) as a *"Self-Reflection Check-In Score"* over time—**never** displaying clinical labels like *"Moderately Severe Depression"*.
   * **Immediate Item 9 Distress Guardrail:** If the user selects $\ge 1$ (*Several days* or higher) on PHQ-9 Question 9 (thoughts of self-harm), immediately display a calm, dismissible support sheet with one-tap calling for **India's National Tele-MANAS Helpline (`14416` / `1800-891-4416`)** and **Emergency Services (`112`)**.
4. **Statistical Honesty in `RankStatsEngine.kt`:**
   * Computes **tie-corrected Spearman's $\rho$** (Pearson correlation on average-ranked vectors) and a **5,000-shuffle Monte Carlo permutation $p$-value** across daily logs ($n \ge 14$).
   * Includes explicit caveats in every generated insight card:
     * *"Correlation reflects a pattern in your personal logs and does not prove causation—other factors like sleep and workload also vary."*
     * *"Note: Daily self-reports are naturally autocorrelated across consecutive days, and a few weeks of logs is a small sample."*
   * Never speculates about psychological motives when $\rho > 0$; simply reports: *"No downward trend between practice minutes and stress was observed in this window ($\rho = +0.18, p = 0.34, n = 28$ days)."*

---

## 10. How to Build This in 24–36 Hours with Antigravity IDE + Claude Code

Divide your team and run these **5 sequential AI-coding prompts** in **Claude Code / Antigravity IDE** using this file (`yoga_wellness_architecture.md`) as your context file:

* **Prompt 1 (Hour 0–3 — Project Skeleton & Data Contracts):**
  > *"Read `yoga_wellness_architecture.md`. Initialize an Android Jetpack Compose project (`minSdk = 26`) with CameraX, MediaPipe Tasks Vision (`0.10.14`), and Room. Strip `INTERNET` permission in `AndroidManifest.xml`. Create `AsanaSpecs.kt` with the exact 4-asana table from §7.2, `OneEuroFilter.kt`, `GravityAligner.kt`, and `AlignmentScorer.kt` from §8, plus a JUnit test verifying the Warrior II proof table in §7.1."*
* **Prompt 2 (Hour 3–10 — Live CameraX + MediaPipe + Skeleton Overlay + Classifier):**
  > *"Build `PoseLandmarkerHelper.kt` (`LIVE_STREAM` mode, passing `imageProxy.imageInfo.rotationDegrees`), `PreflightGuard.kt`, `RuleAndCentroidClassifier.kt` (§6.1 with 14-of-18 temporal lock), `SwayStabilityTracker.kt`, and `HoldStateMachine.kt`. Build `PracticeScreen.kt` with a Compose `Canvas` overlay that mirrors X coordinates for the front camera, draws green/amber/red joints, curved correction arrows, and a live `'∑ Explain Score'` bottom sheet."*
* **Prompt 3 (Hour 10–15 — Offline TTS Coach + Record/Replay Harness):**
  > *"Build `OfflineTtsCoach.kt` with the 4.5-second rate limiter, 1.2-second error persistence threshold, safety-first priority queue (including the Tree Pose $\lambda_{\text{tree}}$ knee guard), and praise chimes. Also build `LandmarkRecorder.kt` and `LandmarkReplayer.kt` so we can record 20-second `.jsonl` pose clips and replay them through the scoring pipeline."*
* **Prompt 4 (Hour 15–22 — Wellness Module, Crisis Guardrail & Spearman Dashboard):**
  > *"Build the Room database entities/DAOs, `ConsentAndSafetyScreen.kt`, the 30-second Daily Check-In screen, the bi-weekly PHQ-9/GAD-7 screens with the immediate Tele-MANAS (`14416` / `112`) crisis modal on Item 9, `RankStatsEngine.kt` (tie-corrected Spearman + 5,000-shuffle Monte Carlo permutation test), and `MockPersonaSeeder.kt` (seeding 42 days of realistic data for Persona A: Consistent Improvement, Persona B: Exam Gap, and Persona C: Null Pattern)."*
* **Prompt 5 (Hour 22–26 — Dashboard Charts & 1-Page Weekly PDF Report):**
  > *"Build `WellnessDashboardScreen.kt` with a Compose `Canvas` dual-axis trend chart (Weekly Practice Quality vs Stress/Mood), Left-vs-Right symmetry card, and plain-language statistical insight cards. Build `PdfReportExporter.kt` using `android.graphics.pdf.PdfDocument` to export and share the 1-page Weekly Progress Report via `FileProvider`."*

* **Hour 26–36 — On-Device Tuning & Demo Rehearsal:** Tune thresholds on a real phone and body using the record/replay harness, then rehearse the demo script end to end.
