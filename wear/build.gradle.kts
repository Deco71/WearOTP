import java.util.Properties
import kotlin.collections.component1
import kotlin.collections.component2

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}


val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

localProperties.forEach { (key, value) ->
    project.ext.set(key.toString(), value)
}

android {
    if (localProperties.getProperty("debug_keystore_path") != null) {
        signingConfigs {
            getByName("debug") {
                storeFile = file(localProperties.getProperty("debug_keystore_path"))
                storePassword = localProperties.getProperty("debug_keystore_password")
                keyPassword = localProperties.getProperty("debug_key_password")
                keyAlias = localProperties.getProperty("debug_key_alias")
            }
        }
    }
    namespace = "com.decoapps.wearotp.wear"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }


    defaultConfig {
        applicationId = "com.decoapps.wearotp"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.datastore.core)
    implementation(libs.datastore.preferences)

    val composeBom = platform("androidx.compose:compose-bom:2026.01.01")

    // General compose dependencies
    implementation(composeBom)
    implementation(libs.activity.compose)
    implementation(libs.androidx.ui.tooling.preview)
    // Other compose dependencies

    // Compose for Wear OS dependencies
    implementation(libs.compose.material3)
    implementation(libs.material.icons.extended)

    // Foundation is additive, so you can use the mobile version in your Wear OS app.
    implementation(libs.compose.foundation)

    // Wear OS preview annotations
    implementation(libs.androidx.compose.ui.tooling)

    // View model and lifecycle dependencies
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    // If you are using Compose Navigation, use the Wear OS version (NOT THE MOBILE VERSION).
    // Uncomment the line below and update the version number.
    implementation(libs.androidx.compose.navigation)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v113)
    androidTestImplementation(libs.androidx.espresso.core.v340)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)

}