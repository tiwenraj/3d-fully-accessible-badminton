plugins {
    id("com.android.application")
}

android {
    namespace = "com.tiwenraj.badminton"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tiwenraj.football"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Temporary test signing so the first release APK can be installed.
            // Replace with a private upload keystore before production distribution.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
