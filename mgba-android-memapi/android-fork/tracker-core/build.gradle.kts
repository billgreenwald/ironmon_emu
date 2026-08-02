plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    // Pure-JVM target: fast, device-free unit tests for the shared logic (runs on Linux CI).
    jvm()

    // Validation-only Kotlin/Native target. Never shipped — it exists so that commonMain is
    // compiled by the Kotlin/Native backend ON LINUX, enforcing the exact same restrictions the
    // iOS targets will (no java.*, common-only @Volatile, etc.). With only JVM-family targets,
    // commonMain is folded into each JVM compile and native/metadata compilation is skipped, so
    // iOS-incompatible code would slip through until macOS CI. linuxX64 catches it here.
    //
    // NOTE: real iOS targets (iosArm64/iosSimulatorArm64) are added in Phase 2 on macOS CI.
    linuxX64()

    sourceSets {
        // kotlin.concurrent.Volatile is @ExperimentalStdlibApi in Kotlin 1.9.x (stable in 2.1).
        // Opt in module-wide so shared singletons can use common-compatible @Volatile.
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
        }
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                implementation("org.jetbrains.kotlinx:atomicfu:0.23.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

android {
    namespace = "hh.game.mgba_android.trackercore"
    compileSdk = 34
    defaultConfig {
        minSdk = 22
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
