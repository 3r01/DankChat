plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.flxrs.dankchat.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Benchmarks run on the managed device, absolute numbers are not representative
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
        buildConfigField("String", "TARGET_APP_ID", "\"com.flxrs.dankchat\"")
    }

    buildFeatures {
        buildConfig = true
    }

    targetProjectPath = ":app"

    buildTypes {
        // Mirrors the app's sacrificial perf build type, benchmarks run against it on a
        // connected device via connectedBenchmarkPerfAndroidTest
        create("perf") {
            isDebuggable = false
            buildConfigField("String", "TARGET_APP_ID", "\"com.flxrs.dankchat.perf\"")
        }
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
}
