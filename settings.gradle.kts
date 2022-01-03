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
            version("compileSdkVersion", "31")
            version("targetSdkVersion", "31")
        }

        create("libs") {
            alias("gradle-ktlint").toPluginId("org.jlleitschuh.gradle.ktlint").version("10.2.0")

            alias("desugar").to("com.android.tools:desugar_jdk_libs:1.1.5")

            version("kotlin", "1.6.10")
            // Support for using version catalogs in buildscript and plugin blocks is in 7.2.0
            // https://github.com/gradle/gradle/pull/17394
            // https://github.com/gradle/gradle/commit/269148642b4499861bced4b028a400f856273bb2
            alias("kotlin-gradle-plugin").to("org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")
            alias("kotlin-serialization-base").to("org.jetbrains.kotlin", "kotlin-serialization").versionRef("kotlin")
            alias("kotlinx-serialization-json").to("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
            alias("kotlinx-coroutines-android").to("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")

            alias("androidx-core-ktx").to("androidx.core:core-ktx:1.7.0")
            alias("androidx-appcompat").to("androidx.appcompat:appcompat:1.4.0")
            alias("androidx-lifecycle-runtime-ktx").to("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
            alias("androidx-activity-compose").to("androidx.activity:activity-compose:1.4.0")

            alias("google-android-material").to("com.google.android.material:material:1.4.0")

            version("accompanist", "0.21.4-beta")
            alias("accompanist-navigation-animation").to("com.google.accompanist", "accompanist-navigation-animation").versionRef("accompanist")
            alias("accompanist-permissions").to("com.google.accompanist", "accompanist-permissions").versionRef("accompanist")
            alias("accompanist-systemuicontroller").to("com.google.accompanist", "accompanist-systemuicontroller").versionRef("accompanist")
            alias("accompanist-insets").to("com.google.accompanist", "accompanist-insets").versionRef("accompanist")
            alias("accompanist-insetsui").to("com.google.accompanist", "accompanist-insets-ui").versionRef("accompanist")

            alias("google-tink").to("com.google.crypto.tink:tink-android:1.6.1")

            alias("signal-argon2").to("org.signal:argon2:13.1")

            version("hilt", "2.40.5")
            alias("dagger-android-gradlePlugin").to("com.google.dagger", "hilt-android-gradle-plugin").versionRef("hilt")
            alias("dagger-android-plugin").toPluginId("dagger.hilt.android.plugin").versionRef("hilt")
            alias("hilt-library").to("com.google.dagger", "hilt-android").versionRef("hilt")
            alias("hilt-compiler").to("com.google.dagger", "hilt-android-compiler").versionRef("hilt")
            version("androidxhilt",  "1.0.0")
            alias("androidx-hilt-work").to("androidx.hilt", "hilt-work").versionRef("androidxhilt")
            alias("androidx-hilt-compiler").to("androidx.hilt", "hilt-compiler").versionRef("androidxhilt")

            alias("androidx-hilt-navigation-compose").to("androidx.hilt:hilt-navigation-compose:1.0.0-rc01")

            alias("dagger-compiler").to("com.google.dagger", "dagger-compiler").versionRef("hilt")
            alias("dagger-dagger").to("com.google.dagger", "dagger").versionRef("hilt")

            // ([^ ]*) = \{ module = "(.*):(.*)", version.ref = "(.*)" \} -> alias("$1").to("$2", "$3").versionRef("$4")
            // ([^ ]*) = "(.*)" -> alias("$1").to("$2")

            version("androidxlifecycle", "2.4.0")
            alias("androidx-lifecycle-runtime").to("androidx.lifecycle", "lifecycle-runtime-ktx").versionRef("androidxlifecycle")
            alias("androidx-lifecycle-viewmodel-compose").to("androidx.lifecycle:lifecycle-viewmodel-compose:2.4.0")
            alias("androidx-lifecycle-viewmodel-ktx").to("androidx.lifecycle", "lifecycle-viewmodel-ktx").versionRef("androidxlifecycle")

            alias("androidx-navigation-compose").to("androidx.navigation:navigation-compose:2.4.0-rc01")

            version("room",  "2.4.0")
            alias("androidx-room-common").to("androidx.room", "room-common").versionRef("room")
            alias("androidx-room-ktx").to("androidx.room", "room-ktx").versionRef("room")
            alias("androidx-room-compiler").to("androidx.room", "room-compiler").versionRef("room")
            alias("androidx-room-paging").to("androidx.room", "room-paging").versionRef("room")
            alias("androidx-room-runtime").to("androidx.room", "room-runtime").versionRef("room")
            alias("androidx-room-testing").to("androidx.room", "room-testing").versionRef("room")

            alias("sqlcipher").to("net.zetetic:android-database-sqlcipher:4.5.0")

            version("paging", "3.1.0")
            alias("androidx-paging-common").to("androidx.paging", "paging-common-ktx").versionRef("paging")
            alias("androidx-paging-runtime").to("androidx.paging", "paging-runtime-ktx").versionRef("paging")
            alias("androidx-paging-compose").to("androidx.paging:paging-compose:1.0.0-alpha14")

            alias("androidx-constraintlayout-compose").to("androidx.constraintlayout:constraintlayout-compose:1.0.0-rc02")

            version("compose", "1.1.0-rc01")
            // TODO: Remove this when all other Compose deps use 1.1.0-rc02 or higher
            version("composecompiler", "1.1.0-rc02")
            alias("compose-runtime").to("androidx.compose.runtime", "runtime").versionRef("compose")
            alias("compose-ui-ui").to("androidx.compose.ui", "ui").versionRef("compose")
            alias("compose-ui-util").to("androidx.compose.ui", "ui-util").versionRef("compose")
            alias("compose-material-material").to("androidx.compose.material", "material").versionRef("compose")
            alias("compose-material-iconsext").to("androidx.compose.material", "material-icons-extended").versionRef("compose")
            alias("compose-ui-tooling-preview").to("androidx.compose.ui", "ui-tooling-preview").versionRef("compose")

            version("okhttp", "4.9.3")
            alias("okhttp-okhttp").to("com.squareup.okhttp3", "okhttp").versionRef("okhttp")
            alias("okhttp-mockwebserver").to("com.squareup.okhttp3", "mockwebserver").versionRef("okhttp")

            alias("okio").to("com.squareup.okio:okio:3.0.0")

            alias("ksp").toPluginId("com.google.devtools.ksp").version("1.6.10-1.0.2")
            version("moshi", "1.13.0")
            alias("moshi-core").to("com.squareup.moshi", "moshi").versionRef("moshi")
            alias("moshi-codegen").to("com.squareup.moshi", "moshi-kotlin-codegen").versionRef("moshi")
            alias("moshi-adapters").to("com.squareup.moshi", "moshi-adapters").versionRef("moshi")
            // alias("valiktor").to("org.valiktor:valiktor-core:0.12.0")

            version("protobuf", "3.19.1")
            alias("protobuf-kotlin-lite").to("com.google.protobuf", "protobuf-kotlin-lite").versionRef("protobuf")
            alias("protobuf-compiler").to("com.google.protobuf", "protoc").versionRef("protobuf")
            alias("protobuf").toPluginId("com.google.protobuf").version("0.8.18")

            alias("datastore").to("androidx.datastore:datastore:1.0.0")

            version("junit5", "5.8.2")
            alias("junit5-androidGradlePlugin").to("de.mannodermaus.gradle.plugins:android-junit5:1.8.2.0")
            alias("junit5-androidGradlePlugin").toPluginId("de.mannodermaus.android-junit5")
            alias("junit5-api").to("org.junit.jupiter", "junit-jupiter-api").versionRef("junit5")
            alias("junit5-engine").to("org.junit.jupiter", "junit-jupiter-engine").versionRef("junit5")
            alias("junit5-params").to("org.junit.jupiter", "junit-jupiter-params").versionRef("junit5")

            version("mockk", "1.12.1")
            alias("mockk-mockk").to("io.mockk", "mockk").versionRef("mockk")
            alias("mockk-agent-jvm").to("io.mockk", "mockk-agent-jvm").versionRef("mockk")

            alias("junit-android").to("junit:junit:4.13.2")
            alias("androidx-test-ext-junit").to("androidx.test.ext:junit:1.1.3")
            alias("androidx-test-espresso-core").to("androidx.test.espresso:espresso-core:3.4.0")
            alias("androidx-compose-ui-test-junit4").to("androidx.compose.ui", "ui-test-junit4").versionRef("compose")
            alias("androidx-compose-ui-tooling").to("androidx.compose.ui", "ui-tooling").versionRef("compose")
        }
    }
}

rootProject.name = "cradle5-trial-app"
include(":app")
include(":api")

include(":data")
include(":util")
include(":domain")
