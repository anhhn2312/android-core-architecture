import extension.implementations
import extension.kapts

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {
    compileSdk = projectConfig.AndroidConfig.compileSdkVersion
    buildToolsVersion = projectConfig.AndroidConfig.buildToolVersion

    defaultConfig {
        minSdk = projectConfig.AndroidConfig.minSdkVersion
        targetSdk = projectConfig.AndroidConfig.targetSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}