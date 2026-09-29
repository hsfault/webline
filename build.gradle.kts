plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hsfault.webline"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hsfault.webline"
        minSdk = 30
        targetSdk = 34
        val run = System.getenv("GITHUB_RUN_NUMBER") ?: "1"
        versionCode = run.toInt()
        versionName = "0.1.$run"
    }

    signingConfigs {
        create("release") {
            val path = System.getenv("WEBLINE_KEYSTORE_PATH")
            if (path != null && file(path).exists()) {
                storeFile = file(path)
                storePassword = System.getenv("WEBLINE_KEYSTORE_PASSWORD")
                keyAlias = "webline"
                keyPassword = System.getenv("WEBLINE_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 optimization: Compose needs this to run smoothly on mid-range phones.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Installs Compose's pre-compiled startup/scroll profiles so the app is fast from first launch.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
}