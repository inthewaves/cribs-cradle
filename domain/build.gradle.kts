@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.gradle.ktlint)
    kotlin("kapt")
    id("de.mannodermaus.android-junit5")
}

android {
    compileSdk = appconfig.versions.compileSdkVersion.get().toInt()
    defaultConfig {
        minSdk = appconfig.versions.minSdkVersion.get().toInt()
        // targetSdk = 31
        // versionCode 1
        // versionName "1.0"

        // testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // consumerProguardFiles.add(File("consumer-rules.pro"))
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)

    api(project(":util"))
    api(project(":api"))
    api(project(":data"))

    implementation(kotlin("stdlib"))

    api(libs.moshi.core)
    implementation(libs.moshi.adapters)

    implementation(libs.okhttp.okhttp)
    implementation(libs.okio)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.library)
    kapt(libs.hilt.compiler)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}
