plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.smartsolar.microgrid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartsolar.microgrid"
        minSdk = 24
        targetSdk = 34
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