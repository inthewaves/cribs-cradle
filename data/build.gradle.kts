@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    alias(libs.plugins.ksp)
    alias(libs.plugins.gradle.ktlint)
    id("de.mannodermaus.android-junit5")
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        // targetSdk = 31
        // versionCode 1
        // versionName "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles.add(File("consumer-rules.pro"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":util"))

    implementation(libs.androidx.core.ktx)

    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)

    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.library)
    kapt(libs.hilt.compiler)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // implementation 'androidx.core:core-ktx:1.7.0'
    // implementation 'androidx.appcompat:appcompat:1.4.0'
    // implementation 'com.google.android.material:material:1.4.0'
    // testImplementation 'junit:junit:4.+'
    // androidTestImplementation 'androidx.test.ext:junit:1.1.3'
    // androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'
}
