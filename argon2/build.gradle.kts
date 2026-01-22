plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "org.signal.argon2"
  compileSdk = 35
  // NDK r28 and higher compile 16 KB-aligned by default. Use latest stable version.
  // If encountering issues with SDK Manager, download directly at
  // https://developer.android.com/ndk/downloads
  ndkVersion = "29.0.14206865"

  externalNativeBuild {
    ndkBuild {
      cmake {
        path = file("jni/CMakeLists.txt")
        version = "3.22.1"
      }
    }
  }

  defaultConfig {
    minSdk = 23

    // Limit ABIs you ship to reduce APK size
    ndk {
      abiFilters += listOf("armeabi-v7a", "x86", "arm64-v8a", "x86_64")
    }

    externalNativeBuild {
      cmake {
        // If you need to use NDK < r28, use this
        arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
      }
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  sourceSets {
    getByName("main") {
      // jniLibs.srcDirs("libs")
    }
  }

  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.3.0")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}