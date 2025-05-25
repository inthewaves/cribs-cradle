// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val agp_version by extra("8.8.2")
  repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$agp_version")
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.kotlin.serialization.base)

        classpath(libs.dagger.android.gradlePlugin)

        classpath(libs.junit5.androidGradlePlugin)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
