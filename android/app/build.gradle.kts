plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.r2dapps.cutemascot"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.r2dapps.cutemascot"
        minSdk = 26          // Android 8.0 Oreo (TYPE_APPLICATION_OVERLAY support)
        targetSdk = 34
        versionCode = 14     // v1.3.0
        versionName = "1.3.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // AGP 7.x aaptOptions syntax
    aaptOptions {
        noCompress("mp3", "wav", "m4a")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("org.json:json:20230227")
}
