plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.twotechy.inknest"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.twotechy.inknest"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // AGP's default service merge takes precedence over exclusions.
            merges -= "/META-INF/services/**"
            // LuaJ's desktop JSR-223 provider is unused; Android has no javax.script API.
            excludes += "META-INF/services/javax.script.ScriptEngineFactory"
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += "release"
        }
        create("profile") {
            initWith(getByName("benchmark"))
            optimization { enable = false }
        }
    }
    sourceSets {
        getByName("main").baselineProfiles.directories += "src/main/generated/baselineProfiles"
        getByName("benchmark").kotlin.directories += "src/release/java"
        getByName("profile") {
            kotlin.directories += setOf("src/release/java", "src/benchmark/java")
            manifest.srcFile("src/benchmark/AndroidManifest.xml")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // AndroidX platform integration
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)

    // Lifecycle-aware UI state, saved state and coroutine scopes
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.kotlinx.coroutines.android)

    // KOReader data parser
    implementation(libs.luaj.jse)

    // Persistence
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.framework)
    ksp(libs.androidx.room.compiler)

    // Compose UI: the BOM manages versions for all Compose libraries below
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    // Local JVM tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Android instrumentation tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
