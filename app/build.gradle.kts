plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ldofertas.equalizer"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ldofertas.equalizer"
        minSdk = 23
        targetSdk = 28
        versionCode = 1
        versionName = "1.0.0"
    }
}
