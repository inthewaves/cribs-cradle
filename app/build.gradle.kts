import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

// suppressing due to https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.android.application")
    id("kotlin-parcelize")
    kotlin("android")
    kotlin("kapt")
    alias(libs.plugins.gradle.ktlint)
    id("dagger.hilt.android.plugin")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-Xopt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-Xopt-in=com.google.accompanist.insets.ExperimentalAnimatedInsets",
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-Xopt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

val PRODUCTION_URL = "https://www.medscinet.com/Cradle5/api"
val TEST_URL = "https://www.medscinet.com/Cradle5Test/api"

android {
    compileSdk = appconfig.versions.compileSdkVersion.get().toInt()
    defaultConfig {
        applicationId = "org.welbodipartnership.cradle5"
        minSdk = appconfig.versions.minSdkVersion.get().toInt()
        targetSdk = appconfig.versions.targetSdkVersion.get().toInt()
        versionCode = 29
        versionName = "1.5.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // export NDK symbols for Play Store console
        ndk.debugSymbolLevel = "FULL"

        buildConfigField("String", "PRIVACY_POLICY_URL", "\"https://docs.google.com/document/d/e/2PACX-1vSD9nLQwORfZff2LX72EgLyzas405yutCRUNbXY4XTzNcPnqfpiTymCw4bvqxiwdh0wvk9QS-ufnyRR/pub\"")
        buildConfigField("String", "PRODUCTION_API_URL", "\"$PRODUCTION_URL\"")
        buildConfigField("String", "TEST_API_URL", "\"$TEST_URL\"")

        buildConfigField("String", "DEFAULT_API_URL", "\"$PRODUCTION_URL\"")

        buildConfigField("String", "DEFAULT_ACRA_URL", "\"https://crashes.inthewaves.org/report\"")
        buildConfigField("String", "ACRA_USER", "\"BcwREdr7a6cADLCA\"")
        buildConfigField("String", "ACRA_PASS", "\"2ExH5ObdCnd88XeP\"")
    }

    signingConfigs {
        named("debug") {
            storeFile = rootProject.file("debug.keystore.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        create("release") {
            storeFile = rootProject.file("release.keystore.jks")
            storePassword =
                gradleLocalProperties(rootProject.rootDir).getProperty("cradle5AppKeystorePassword")
            keyAlias = gradleLocalProperties(rootProject.rootDir).getProperty("cradle5AppKeyAlias")
            keyPassword =
                gradleLocalProperties(rootProject.rootDir).getProperty("cradle5AppKeyPassword")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            buildConfigField("String", "DEFAULT_API_URL", "\"$TEST_URL\"")
        }

        release {
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            buildConfigField("String", "DEFAULT_API_URL", "\"$PRODUCTION_URL\"")
        }

        create("staging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".releaseStaging"
            versionNameSuffix = "-staging"
            isDebuggable = false

            matchingFallbacks += listOf("release")

            buildConfigField("String", "DEFAULT_API_URL", "\"$TEST_URL\"")
        }
        // getByName("...")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.get()
    }
    packagingOptions {
        resources {
            // https://github.com/Kotlin/kotlinx.coroutines/issues/2023#issuecomment-858644393
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    namespace = "org.welbodipartnership.cradle5"
}

dependencies {
    api(project(":api"))
    api(project(":data"))
    api(project(":domain"))

    // implementation(kotlin("reflect"))

    coreLibraryDesugaring(libs.desugar)

    implementation(libs.acra.http)
    implementation(libs.acra.toast)
    implementation(libs.acra.limiter)
    implementation(libs.acra.advancedscheduler)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.okhttp.okhttp)

    implementation(libs.google.android.material)
    implementation(libs.compose.ui.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.material.material)
    implementation(libs.compose.material.iconsext)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.insets)
    implementation(libs.accompanist.insetsui)
    implementation(libs.accompanist.systemuicontroller)

    implementation(libs.androidx.paging.compose)

    implementation(libs.hilt.library)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.google.tink)
    implementation(libs.signal.argon2)

    implementation(libs.gms.location)

    testImplementation(libs.junit.android)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Allow references to generated code
// https://developer.android.com/training/dependency-injection/hilt-android#setup
kapt {
    correctErrorTypes = true
}
