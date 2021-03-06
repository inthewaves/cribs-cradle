import com.google.protobuf.gradle.builtins
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.android.library")
    id("kotlin-parcelize")
    kotlin("android")
    kotlin("kapt")
    alias(libs.plugins.ksp)
    alias(libs.plugins.gradle.ktlint)
    alias(libs.plugins.protobuf)
    id("de.mannodermaus.android-junit5")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

android {
    compileSdk = appconfig.versions.compileSdkVersion.get().toInt()
    defaultConfig {
        minSdk = appconfig.versions.minSdkVersion.get().toInt()
        // targetSdk = 31
        // versionCode 1
        // versionName "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles.add(File("consumer-rules.pro"))
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main") {

            // https://github.com/google/protobuf-gradle-plugin/pull/433/files
            fun com.android.build.api.dsl.AndroidSourceSet.proto(action: SourceDirectorySet.() -> Unit) {
                (this as? ExtensionAware)!!
                    .extensions
                    .getByName("proto")
                    .let { it as? SourceDirectorySet }!!
                    .apply(action)
            }

            proto {
                srcDir("src/main/proto")
            }
        }
    }
    namespace = "org.welbodipartnership.cradle5.data"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

/*
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
 */

protobuf {
    protoc {
        artifact = libs.protobuf.compiler.get().toString()
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }

                id("kotlin") {}
            }
        }
    }
}

dependencies {
    api(project(":util"))

    coreLibraryDesugaring(libs.desugar)

    // implementation(kotlin("reflect"))

    implementation(libs.androidx.core.ktx)

    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher)

    implementation(libs.hilt.library)
    kapt(libs.hilt.compiler)

    api(libs.google.tink)

    implementation(libs.compose.runtime)

    implementation(libs.moshi.core)
    ksp(libs.moshi.codegen)

    api(libs.protobuf.kotlin.lite)
    implementation(libs.datastore)

    implementation(libs.signal.argon2)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit5.api)
    testImplementation(libs.mockk.mockk)
    testImplementation(libs.mockk.agent.jvm)
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
