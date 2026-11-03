plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application)
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

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // AndroidX platform integration
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Lifecycle-aware UI state, saved state and coroutine scopes
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.kotlinx.coroutines.android)

    // Persistence
    implementation(libs.androidx.datastore.preferences)

    // Compose UI: the BOM manages versions for all Compose libraries below
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    // Preview fixtures and tooling are excluded from release
    debugImplementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Local JVM tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Android instrumentation tests
    androidTestImplementation(libs.androidx.junit)
}
