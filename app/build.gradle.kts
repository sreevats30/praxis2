import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.companion.yogawellness"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.companion.yogawellness"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // MediaPipe memory-maps the model, so it must be stored uncompressed in the APK.
    androidResources {
        noCompress += "task"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // The exported Room schemas are packaged for the on-device migration test.
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")

    lint {
        abortOnError = true
        // Library versions are locked by the blueprint (§2), so "newer version available" is noise.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// ---------------------------------------------------------------------------------------------
// MediaPipe pose model (§2): pose_landmarker_full.task is fetched once into src/main/assets.
// The file is git-ignored; this task runs automatically before every build and is a no-op
// once the file exists. To pin a model offline, drop the file into app/src/main/assets/.
// ---------------------------------------------------------------------------------------------
val poseModelUrl =
    "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task"
val poseModelFile = layout.projectDirectory.file("src/main/assets/pose_landmarker_full.task").asFile

val downloadPoseModel by tasks.registering {
    description = "Downloads pose_landmarker_full.task into src/main/assets if it is missing."
    outputs.file(poseModelFile)
    onlyIf { !poseModelFile.exists() }
    doLast {
        poseModelFile.parentFile.mkdirs()
        val partial = File(poseModelFile.parentFile, poseModelFile.name + ".part")
        URI(poseModelUrl).toURL().openStream().use { input ->
            partial.outputStream().use { output -> input.copyTo(output) }
        }
        check(partial.length() > 1_000_000) { "Pose model download looks truncated (${partial.length()} bytes)" }
        check(partial.renameTo(poseModelFile)) { "Could not move model into assets" }
        logger.lifecycle("Downloaded pose model to $poseModelFile (${poseModelFile.length()} bytes)")
    }
}
tasks.named("preBuild") { dependsOn(downloadPoseModel) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Camera + pose model (§2)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mediapipe.tasks.vision)

    // Local on-device storage (§2)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
    // Robolectric renders the Compose screens on the JVM (no emulator needed) for smoke/screenshot tests
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // On-device checks (real PdfDocument + MediaPipe): ./gradlew :app:connectedDebugAndroidTest
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
