dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Compatible builds for newer versions at Kotlin can be found e.g. for Kotlin 1.6.10,
        // https://developer.android.com/jetpack/androidx/releases/compose-compiler#1.1.0-rc01
        // maven { url = URI("https://androidx.dev/snapshots/builds/8003490/artifacts/repository") }
    }

    enableFeaturePreview("VERSION_CATALOGS")
    versionCatalogs {
        create("appconfig"){
            version("minSdkVersion", "23") // Android 6.0
            version("compileSdkVersion", "33")
            version("targetSdkVersion", "33")
        }

        create("libs") {
            plugin("gradle-ktlint", "org.jlleitschuh.gradle.ktlint").version("10.2.0")

            library("desugar", "com.android.tools:desugar_jdk_libs:1.1.5")

            version("kotlin", "1.6.10")
            // Support for using version catalogs in buildscript and plugin blocks is in 7.2.0
            // https://github.com/gradle/gradle/pull/17394
            // https://github.com/gradle/gradle/commit/269148642b4499861bced4b028a400f856273bb2
            library("kotlin-gradle-plugin", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")
            library("kotlin-serialization-base", "org.jetbrains.kotlin", "kotlin-serialization").versionRef("kotlin")
            library("kotlinx-serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
            library("kotlinx-coroutines-android", "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.2")

            version("acra", "5.9.6")
            library("acra-http", "ch.acra", "acra-http").versionRef("acra")
            library("acra-toast", "ch.acra", "acra-toast").versionRef("acra")
            library("acra-limiter", "ch.acra", "acra-limiter").versionRef("acra")
            library("acra-advancedscheduler", "ch.acra", "acra-advanced-scheduler").versionRef("acra")

            library("androidx-core-ktx", "androidx.core:core-ktx:1.9.0")
            library("androidx-appcompat", "androidx.appcompat:appcompat:1.5.1")

            version("androidxlifecycle", "2.4.1")
            library("androidx-lifecycle-runtime-ktx", "androidx.lifecycle", "lifecycle-runtime-ktx").versionRef("androidxlifecycle")
            library("androidx-lifecycle-process", "androidx.lifecycle", "lifecycle-process").versionRef("androidxlifecycle")
            library("androidx-lifecycle-runtime", "androidx.lifecycle", "lifecycle-runtime-ktx").versionRef("androidxlifecycle")
            library("androidx-lifecycle-viewmodel-compose", "androidx.lifecycle:lifecycle-viewmodel-compose:2.4.1")
            library("androidx-lifecycle-viewmodel-ktx", "androidx.lifecycle", "lifecycle-viewmodel-ktx").versionRef("androidxlifecycle")

            library("androidx-work-runtime", "androidx.work:work-runtime-ktx:2.7.1")

            library("google-android-material", "com.google.android.material:material:1.6.1")

            version("accompanist", "0.23.1")
            library("accompanist-navigation-animation", "com.google.accompanist", "accompanist-navigation-animation").versionRef("accompanist")
            library("accompanist-permissions", "com.google.accompanist", "accompanist-permissions").versionRef("accompanist")
            library("accompanist-systemuicontroller", "com.google.accompanist", "accompanist-systemuicontroller").versionRef("accompanist")
            library("accompanist-insets", "com.google.accompanist", "accompanist-insets").versionRef("accompanist")
            library("accompanist-insetsui", "com.google.accompanist", "accompanist-insets-ui").versionRef("accompanist")

            library("google-tink", "com.google.crypto.tink:tink-android:1.7.0")

            library("signal-argon2", "org.signal:argon2:13.1")

            version("hilt", "2.42")
            library("dagger-android-gradlePlugin", "com.google.dagger", "hilt-android-gradle-plugin").versionRef("hilt")
            plugin("dagger-android-plugin", "dagger.hilt.android.plugin").versionRef("hilt")
            library("hilt-library", "com.google.dagger", "hilt-android").versionRef("hilt")
            library("hilt-compiler", "com.google.dagger", "hilt-android-compiler").versionRef("hilt")
            version("androidxhilt",  "1.0.0")
            library("androidx-hilt-work", "androidx.hilt", "hilt-work").versionRef("androidxhilt")
            library("androidx-hilt-compiler", "androidx.hilt", "hilt-compiler").versionRef("androidxhilt")

            library("androidx-hilt-navigation-compose", "androidx.hilt:hilt-navigation-compose:1.0.0")

            library("dagger-compiler", "com.google.dagger", "dagger-compiler").versionRef("hilt")
            library("dagger-dagger", "com.google.dagger", "dagger").versionRef("hilt")

            // ([^ ]*) = \{ module = "(.*):(.*)", version.ref = "(.*)" \} -> library("$1", "$2", "$3").versionRef("$4")
            // ([^ ]*) = "(.*)" -> library("$1", "$2")

            library("androidx-navigation-compose", "androidx.navigation:navigation-compose:2.5.3")

            version("room",  "2.4.2")
            library("androidx-room-common", "androidx.room", "room-common").versionRef("room")
            library("androidx-room-ktx", "androidx.room", "room-ktx").versionRef("room")
            library("androidx-room-compiler", "androidx.room", "room-compiler").versionRef("room")
            library("androidx-room-paging", "androidx.room", "room-paging").versionRef("room")
            library("androidx-room-runtime", "androidx.room", "room-runtime").versionRef("room")
            library("androidx-room-testing", "androidx.room", "room-testing").versionRef("room")

            library("androidx-activity-compose", "androidx.activity:activity-compose:1.4.0")

            library("sqlcipher", "net.zetetic:android-database-sqlcipher:4.5.2")

            version("paging", "3.1.1")
            library("androidx-paging-common", "androidx.paging", "paging-common-ktx").versionRef("paging")
            library("androidx-paging-runtime", "androidx.paging", "paging-runtime-ktx").versionRef("paging")
            library("androidx-paging-compose", "androidx.paging:paging-compose:1.0.0-alpha17")

            library("androidx-constraintlayout-compose", "androidx.constraintlayout:constraintlayout-compose:1.0.1")

            version("compose", "1.1.1")
            library("compose-runtime", "androidx.compose.runtime", "runtime").versionRef("compose")
            library("compose-ui-ui", "androidx.compose.ui", "ui").versionRef("compose")
            library("compose-ui-util", "androidx.compose.ui", "ui-util").versionRef("compose")
            library("compose-material-material", "androidx.compose.material", "material").versionRef("compose")
            library("compose-material-iconsext", "androidx.compose.material", "material-icons-extended").versionRef("compose")
            library("compose-ui-tooling-preview", "androidx.compose.ui", "ui-tooling-preview").versionRef("compose")

            version("okhttp", "4.10.0")
            library("okhttp-okhttp", "com.squareup.okhttp3", "okhttp").versionRef("okhttp")
            library("okhttp-mockwebserver", "com.squareup.okhttp3", "mockwebserver").versionRef("okhttp")

            library("okio", "com.squareup.okio:okio:3.2.0")

            plugin("ksp", "com.google.devtools.ksp").version("1.6.10-1.0.2")
            version("moshi", "1.13.0")
            library("moshi-core", "com.squareup.moshi", "moshi").versionRef("moshi")
            library("moshi-codegen", "com.squareup.moshi", "moshi-kotlin-codegen").versionRef("moshi")
            library("moshi-adapters", "com.squareup.moshi", "moshi-adapters").versionRef("moshi")
            // library("valiktor", "org.valiktor:valiktor-core:0.12.0")

            version("protobuf", "3.21.9")
            library("protobuf-kotlin-lite", "com.google.protobuf", "protobuf-kotlin-lite").versionRef("protobuf")
            library("protobuf-compiler", "com.google.protobuf", "protoc").versionRef("protobuf")
            plugin("protobuf", "com.google.protobuf").version("0.8.18")

            library("datastore", "androidx.datastore:datastore:1.0.0")

            library("gms-location", "com.google.android.gms:play-services-location:21.0.1")

            version("junit5", "5.8.2")
            library("junit5-androidGradlePlugin", "de.mannodermaus.gradle.plugins:android-junit5:1.8.2.0")
            // plugin("junit5-androidGradlePlugin", "de.mannodermaus.android-junit5")
            library("junit5-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit5")
            library("junit5-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit5")
            library("junit5-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("junit5")

            version("mockk", "1.12.4")
            library("mockk-mockk", "io.mockk", "mockk").versionRef("mockk")
            library("mockk-agent-jvm", "io.mockk", "mockk-agent-jvm").versionRef("mockk")

            library("junit-android", "junit:junit:4.13.2")
            library("androidx-test-ext-junit", "androidx.test.ext:junit:1.1.3")
            library("androidx-test-espresso-core", "androidx.test.espresso:espresso-core:3.4.0")
            library("androidx-compose-ui-test-junit4", "androidx.compose.ui", "ui-test-junit4").versionRef("compose")
            library("androidx-compose-ui-tooling", "androidx.compose.ui", "ui-tooling").versionRef("compose")
        }
    }
}

rootProject.name = "cradle5-trial-app"
include(":app")
include(":api")

include(":data")
include(":util")
include(":domain")
