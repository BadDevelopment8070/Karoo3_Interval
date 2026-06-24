plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bastiankahuna.karoosmartftp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bastiankahuna.karoosmartftp"
        minSdk = 26
        targetSdk = 34
        versionCode = 14
        versionName = "1.1.4"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("io.hammerhead:karoo-ext:1.1.9")
}
