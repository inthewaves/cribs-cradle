@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.gradle.ktlint)
    alias(libs.plugins.ksp)
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
    namespace = "org.welbodipartnership.cradle5.api"
}

dependencies {
    api(project(":util"))

    coreLibraryDesugaring(libs.desugar)
    // implementation(kotlin("reflect"))

    implementation(libs.androidx.core.ktx)

    api(libs.moshi.core)
    implementation(libs.moshi.adapters)
    ksp(libs.moshi.codegen)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}
