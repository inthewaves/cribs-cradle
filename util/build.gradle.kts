plugins {
    id("java-library")
    id("kotlin")
    kotlin("kapt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api(libs.moshi.core)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.dagger.dagger)
    kapt(libs.dagger.compiler)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}