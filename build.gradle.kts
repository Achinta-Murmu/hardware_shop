// Top-level build file. Plugins are declared here once and applied in :app.

buildscript {
    dependencies {
        // AGP 9 compiles Kotlin by itself ("built-in Kotlin"). This line only pins
        // WHICH Kotlin version it uses, so it matches the Compose compiler plugin below.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
