import java.io.ByteArrayOutputStream
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.smartsolar.microgrid"
    // Pinned to what is actually installed under
    // %LOCALAPPDATA%/Android/Sdk/platforms. Bumping this needs the
    // matching platform to be installed via SDK Manager first.
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartsolar.microgrid"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Silence the "targetSdk out of date" and "compileSdk newer
    // available" lint chirps. They're advisory, not build errors,
    // and we'll bump both together once we install the platform.
    lint {
        disable += "OldTargetApi"
        disable += "ExpiredTargetSdkVersion"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    // Android core libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // RecyclerView for lists (reservations, slots)
    implementation(libs.androidx.recyclerview)

    // Swipe-to-refresh on list screens
    implementation(libs.androidx.swiperefreshlayout)

    // Retrofit for communicating with the C# Web API
    implementation(libs.retrofit)

    // Gson converter for converting JSON responses
    // into Kotlin data classes
    implementation(libs.retrofit.converter.gson)

    // OkHttp for HTTP communication
    implementation(libs.okhttp)

    // OkHttp logging interceptor for development/debugging
    implementation(libs.okhttp.logging.interceptor)

    // Gson JSON library
    implementation(libs.gson)

    // Kotlin coroutines for background operations
    implementation(libs.kotlinx.coroutines.android)

    // Unit testing
    testImplementation(libs.junit)

    // Android testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ---------------------------------------------------------------
// Auto-reverse the phone's port 5147 back to the developer PC
// after every deploy.
//
// The API client uses http://localhost:5147/ from the phone. The
// device's own localhost only reaches the developer PC when an
// `adb reverse tcp:5147 tcp:5147` mapping is in place, and that
// mapping is torn down every time the app is (re)installed.
//
// This hook re-applies the mapping automatically at the end of
// installDebug / installRelease, so the app just works after
// every Android Studio run without any manual steps.
//
// If adb isn't found or no devices are attached, we log and
// continue -- we never fail the build over a dev convenience.
// ---------------------------------------------------------------

// ---------------------------------------------------------------
// Dedicated "always run" task that re-applies the adb reverse
// tunnel for the API port. We attach it to every install/deploy
// variant Android Studio might use (installDebug / installRelease
// and the "Apply Changes" family) so the tunnel survives a Studio
// Run even when Gradle skips the install task as UP-TO-DATE.
// ---------------------------------------------------------------

val portForwardHostToDevice = 5147

val adbReverseApiPort by tasks.registering {
    group = "smartsolar dev"
    description = "Re-applies adb reverse tcp:$portForwardHostToDevice for the phone."

    // Force Gradle to run this every time regardless of caching.
    outputs.upToDateWhen { false }

    doLast {
        val sdkDir = android.sdkDirectory
        val adb = listOf(
            File(sdkDir, "platform-tools/adb.exe"),
            File(sdkDir, "platform-tools/adb"),
        ).firstOrNull { candidate -> candidate.exists() }

        if (adb == null) {
            logger.warn(
                "adb reverse: adb not found under " +
                        "${sdkDir.absolutePath}/platform-tools",
            )
            return@doLast
        }

        // Skip cleanly if nothing is attached (CI, phone unplugged)
        // so we never fail a build over a dev convenience.
        val devicesStdout = ByteArrayOutputStream()
        exec {
            commandLine(adb.absolutePath, "devices")
            standardOutput = devicesStdout
            isIgnoreExitValue = true
        }
        val hasDevice = devicesStdout.toString()
            .lineSequence()
            .drop(1)
            .any { line -> line.trim().endsWith("\tdevice") }

        if (!hasDevice) {
            logger.lifecycle(
                "adb reverse: no attached device -- " +
                        "skipping tcp:$portForwardHostToDevice tunnel.",
            )
            return@doLast
        }

        val result = exec {
            commandLine(
                adb.absolutePath,
                "reverse",
                "tcp:$portForwardHostToDevice",
                "tcp:$portForwardHostToDevice",
            )
            isIgnoreExitValue = true
        }

        if (result.exitValue == 0) {
            logger.lifecycle(
                "adb reverse: tcp:$portForwardHostToDevice " +
                        "-> host tcp:$portForwardHostToDevice (ready).",
            )
        } else {
            logger.warn(
                "adb reverse: command returned exit " +
                        "${result.exitValue} -- login may fail " +
                        "until it succeeds.",
            )
        }
    }
}

// finalizedBy runs even when the target task is UP-TO-DATE or
// fails, so this fires on every Run from Android Studio.
tasks.matching { task ->
    val name = task.name
    name == "installDebug" ||
            name == "installRelease" ||
            name.startsWith("install") && name.endsWith("Debug") ||
            name.startsWith("apply") && name.endsWith("Debug")
}.configureEach {
    finalizedBy(adbReverseApiPort)
}