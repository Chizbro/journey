plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.journey.probe"
    // 35 is a floor, not a preference: connect-client requires it.
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.journey.probe"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // If this version fails to resolve, check the current one — this library moves.
    // Anything >= 1.1.0-alpha11 has what the probe needs.
    implementation("androidx.health.connect:connect-client:1.1.0-alpha11")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
