
plugins {
  id("com.android.application")
  kotlin("android")
  // suppressing due to https://youtrack.jetbrains.com/issue/KTIJ-19369
  @Suppress("DSL_SCOPE_VIOLATION")
  alias(libs.plugins.gradle.ktlint)
}

android {
  compileSdk = 31

  defaultConfig {
    applicationId = "org.welbodipartnership.cradle5"
    minSdk = 21
    targetSdk = 31
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables.useSupportLibrary = true
  }

  buildTypes {
    debug {
      buildConfigField("String", "SERVER_URL", "\"0\"")
    }

    release {
      buildConfigField("String", "SERVER_URL", "\"0\"")

      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }

    // getByName("...")
  }
  compileOptions {
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
    kotlinCompilerExtensionVersion = libs.versions.composecompiler.get()
  }
  packagingOptions {
    resources {
      // https://github.com/Kotlin/kotlinx.coroutines/issues/2023#issuecomment-858644393
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  api(project(":api"))

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)

  implementation(libs.google.android.material)
  implementation(libs.compose.ui.ui)
  implementation(libs.compose.material)
  implementation(libs.compose.ui.tooling.preview)

  implementation(libs.kotlinx.coroutines.android)

  implementation(libs.google.tink)
  implementation(libs.signal.argon2)

  testImplementation(libs.junit.android)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
