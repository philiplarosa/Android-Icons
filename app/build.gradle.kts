plugins {
    id("com.android.application")
}

// CI passes -PversionCode=<build number> so every build installs over the last one.
val buildNumber = (findProperty("versionCode") as String?)?.toInt() ?: 1

android {
    namespace = "com.philiplarosa.iconpack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.philiplarosa.iconpack"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // One fixed key for every build (local and CI), so updates install over
    // each other without uninstalling. Fine for sideloading; not for Play Store.
    signingConfigs {
        create("sideload") {
            storeFile = file("sideload.keystore")
            storePassword = "android"
            keyAlias = "sideload"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("sideload")
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
    }
}
