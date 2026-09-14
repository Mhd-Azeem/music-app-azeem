plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.wavelength.music.sarigamademo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wavelength.music.sarigamademo"
        minSdk = 24
        targetSdk = 34
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "0.1-demo"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
