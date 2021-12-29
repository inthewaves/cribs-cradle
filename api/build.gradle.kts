@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("java-library")
    id("kotlin")
    alias(libs.plugins.gradle.ktlint)
    alias(libs.plugins.ksp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    api(project(":util"))

    implementation(kotlin("reflect"))

    api(libs.moshi.core)
    implementation(libs.moshi.adapters)
    ksp(libs.moshi.codegen)


    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}