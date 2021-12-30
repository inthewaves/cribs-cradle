@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.gradle.ktlint)
    alias(libs.plugins.ksp)
    id("de.mannodermaus.android-junit5")
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        // targetSdk = 31
        // versionCode 1
        // versionName "1.0"

        // testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // consumerProguardFiles.add(File("consumer-rules.pro"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":util"))

    implementation(kotlin("reflect"))

    api(libs.moshi.core)
    implementation(libs.moshi.adapters)
    ksp(libs.moshi.codegen)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}
