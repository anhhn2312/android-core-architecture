import extension.implementations
import extension.kapts
import projectConfig.AndroidConfig
import projectDependencies.Modules
import projectDependencies.ProjectDependencies

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

android {
    compileSdk = AndroidConfig.compileSdkVersion
    buildToolsVersion = AndroidConfig.buildToolVersion

    defaultConfig {
        minSdk = AndroidConfig.minSdkVersion
        targetSdk = AndroidConfig.targetSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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

    dexOptions{
        jumboMode = true
        javaMaxHeapSize = "4g"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", ".aar"))))

    implementation(project(path = Modules.CORE_RESOURCE))
    implementation(project(path = Modules.CORE_DATA))
    implementation(project(path = Modules.CORE_EXTENSION))
    implementation(project(path = Modules.CORE_CONFIG))

    implementations(ProjectDependencies.extrasLibs)
    implementations(ProjectDependencies.supportLibs)
    implementations(ProjectDependencies.logging)
    implementations(ProjectDependencies.networking)
    implementations(ProjectDependencies.di)
    implementations(ProjectDependencies.firebase)

    kapts(ProjectDependencies.processing)
}