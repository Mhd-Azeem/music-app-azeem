import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.wavelength.music"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wavelength.music"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Defaults to the project's own Cloudflare Workers deployment of
        // https://github.com/sumitkolhe/jiosaavn-api. Override via local.properties'
        // JIOSAAVN_BASE_URL if you deploy your own instance instead.
        val jioSaavnBaseUrl = localProperties.getProperty("JIOSAAVN_BASE_URL")
            ?: "https://jiosaavn-api.azeemzahira111.workers.dev/api/"
        buildConfigField("String", "JIOSAAVN_BASE_URL", "\"$jioSaavnBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signed with the auto-provisioned debug key so it stays a straightforward sideload
            // build (no keystore secret to manage) while still getting R8's code shrinking/
            // optimization, which the debug build type intentionally skips for fast iteration.
            // Note: CI runs on a fresh runner each time with no persisted ~/.android, so this key
            // isn't guaranteed stable across builds — same pre-existing limitation the old
            // debug-only pipeline had, unrelated to this change. If installing a "latest" update
            // over an older one ever fails with a signature mismatch, uninstall first.
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    ksp(libs.moshi.codegen)
    implementation(libs.okhttp.logging)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)

    implementation(libs.haze)

    implementation(libs.kotlinx.coroutines.android)
}
