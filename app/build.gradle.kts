plugins {
    id("com.android.application")
    kotlin("android")
}

import java.util.Base64

android {
    namespace = "com.sdvgdeploy.glyphbound"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sdvgdeploy.glyphbound"
        minSdk = 24
        targetSdk = 34
        versionCode = 10
        versionName = "0.2.6"
    }

    val keystoreBase64 = System.getenv("ANDROID_KEYSTORE_BASE64")
    val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
    val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")

    val hasSigningSecrets = !keystoreBase64.isNullOrBlank() &&
        !keystorePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()

    if (hasSigningSecrets) {
        val decodedKeystore = Base64.getDecoder().decode(keystoreBase64)
        val keystoreFile = layout.buildDirectory.file("ci/release.keystore").get().asFile
        keystoreFile.parentFile.mkdirs()
        keystoreFile.writeBytes(decodedKeystore)

        signingConfigs {
            create("release") {
                storeFile = keystoreFile
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasSigningSecrets) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:rules"))
    implementation(project(":core:procgen"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
}
