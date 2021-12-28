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
        create("libs") {
            alias("gradle-ktlint").toPluginId("org.jlleitschuh.gradle.ktlint").version("10.2.0")

            version("kotlin", "1.6.10")
            // Support for using version catalogs in buildscript and plugin blocks is in 7.2.0
            // https://github.com/gradle/gradle/pull/17394
            // https://github.com/gradle/gradle/commit/269148642b4499861bced4b028a400f856273bb2
            alias("kotlin-gradle-plugin").to("org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")
            alias("kotlin-serialization-base").to("org.jetbrains.kotlin", "kotlin-serialization").versionRef("kotlin")
            alias("kotlinx-serialization-json").to("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
            alias("kotlinx-coroutines-android").to("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

            alias("androidx-core-ktx").to("androidx.core:core-ktx:1.7.0")
            alias("androidx-appcompat").to("androidx.appcompat:appcompat:1.4.0")
            alias("androidx-lifecycle-runtime-ktx").to("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
            alias("androidx-activity-compose").to("androidx.activity:activity-compose:1.4.0")

            alias("google-android-material").to("com.google.android.material:material:1.4.0")

            alias("google-accompanist-permissions").to("com.google.accompanist:accompanist-permissions:0.21.4-beta")
            alias("google-tink").to("com.google.crypto.tink:tink-android:1.6.1")

            alias("signal-argon2").to("org.signal:argon2:13.1")

            version("compose", "1.1.0-rc01")
            // TODO: Remove this when all other Compose deps use 1.1.0-rc02 or higher
            version("composecompiler", "1.1.0-rc02")
            alias("compose-ui-ui").to("androidx.compose.ui", "ui").versionRef("compose")
            alias("compose-material").to("androidx.compose.material", "material").versionRef("compose")
            alias("compose-ui-tooling-preview").to("androidx.compose.ui", "ui-tooling-preview").versionRef("compose")

            alias("ksp").toPluginId("com.google.devtools.ksp").version("1.6.10-1.0.2")
            version("moshi", "1.13.0")
            alias("moshi-core").to("com.squareup.moshi", "moshi").versionRef("moshi")
            alias("moshi-codegen").to("com.squareup.moshi", "moshi-kotlin-codegen").versionRef("moshi")
            alias("moshi-adapters").to("com.squareup.moshi", "moshi-adapters").versionRef("moshi")
            // alias("valiktor").to("org.valiktor:valiktor-core:0.12.0")

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

