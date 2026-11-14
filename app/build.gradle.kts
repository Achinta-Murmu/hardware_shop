plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// The cloud build passes a rising number so each new APK installs over the last one.
fun env(name: String, default: String): String = (System.getenv(name) ?: default).trim()

android {
    namespace = "com.galla.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.galla.app"
        minSdk = 26
        targetSdk = 36
        versionCode = env("GALLA_VERSION_CODE", "1").toInt()
        versionName = env("GALLA_VERSION_NAME", "0.1.0")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // A throwaway key that lives in the repo so every preview build has the SAME
        // signature. That lets a new APK install over the old one without uninstalling.
        // It is NOT a Play Store key. Create a private one before publishing.
        create("preview") {
            storeFile = rootProject.file("keystore/galla-preview.p12")
            storePassword = "gallapreview"
            keyAlias = "gallapreview"
            keyPassword = "gallapreview"
            storeType = "pkcs12"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("preview")
        }
        release {
            // Not shrunk yet: R8 rules get added once the app is tested on real phones.
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("preview")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.graphics.shapes)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Scanning and making QR codes, and printing stickers and receipts. All on the phone.
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.zxing.core)
    implementation(libs.androidx.print)

    // On-device walkthrough test (app/src/androidTest): opens every screen and saves screenshots.
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
}
