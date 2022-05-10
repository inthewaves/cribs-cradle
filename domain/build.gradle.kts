@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.gradle.ktlint)
    kotlin("kapt")
    id("de.mannodermaus.android-junit5")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlin.RequiresOptIn",
        )
    }
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
  namespace = "org.welbodipartnership.cradle5.domain"
}

dependencies {
    coreLibraryDesugaring(libs.desugar)

    api(project(":util"))
    api(project(":api"))
    api(project(":data"))

    implementation(kotlin("stdlib"))

    api(libs.moshi.core)
    api(libs.moshi.adapters)

    api(libs.compose.runtime)

    implementation(libs.okhttp.okhttp)
    implementation(libs.okio)

    api(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.library)
    kapt(libs.hilt.compiler)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}
