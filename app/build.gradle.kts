plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.redyapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.redyapp"
        minSdk = 29
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}
dependencies {
    // This block contains your existing dependencies plus the fix for Room,
    // now written in the correct Kotlin DSL (.kts) format.


    // Room Database
    val room_version = "2.6.1" // Use 'val' instead of 'def'
    implementation(libs.room.runtime)
    // For a pure Java project, use `annotationProcessor` instead of `kapt`.
    // This is the correct configuration even in a .kts build file.
    annotationProcessor(libs.room.compiler)

    // Your existing dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Note: These two lines are likely redundant now that the explicit Room
    // dependencies are added above. You can try removing them if you wish.
    implementation(libs.room.common.jvm)
    implementation(libs.room.runtime.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase Authentication
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth) // For Java
    implementation(libs.play.services.auth)

    // Networking (Retrofit)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor) // Or your preferred version

    // Lottie for animations
    implementation(libs.lottie) // Or your preferred version

    // For Credential Manager
    implementation(libs.credentials) // Check for the latest version
    implementation(libs.credentials.play.services.auth) // For Google integration
}
