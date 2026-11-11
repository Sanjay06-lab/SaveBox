import java.util.Properties

// Build file for the SaveBox app module.
// Versions are NOT written here - they live in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application)      // Android app + built-in Kotlin support
    alias(libs.plugins.kotlin.compose)           // Jetpack Compose compiler
    alias(libs.plugins.kotlin.serialization)     // @Serializable (navigation routes, backup files)
    alias(libs.plugins.ksp)                      // Code generation for Room
    alias(libs.plugins.room)                     // Room schema export (needed for migrations)
}

/**
 * Reads keystore.properties (project root) if it exists. It holds the release key's
 * location and passwords, so it must NEVER be committed to git (.gitignore excludes it).
 */
val releaseKeystore: Properties? = rootProject.file("keystore.properties")
    .takeIf { file -> file.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use { stream -> load(stream) } } }

android {
    namespace = "com.savebox.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.savebox.app"
        minSdk = 26          // Android 8.0
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ----- Release signing -----
    // Your private upload key is described in keystore.properties (NOT in git).
    // See README "How to build a release APK". Without that file, release builds are signed
    // with the debug key: fine for testing on your own phone, NOT for the Play Store.
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = rootProject.file(releaseKeystore.getProperty("storeFile"))
                storePassword = releaseKeystore.getProperty("storePassword")
                keyAlias = releaseKeystore.getProperty("keyAlias")
                keyPassword = releaseKeystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 removes unused code and resources and shortens names:
            // a smaller, faster APK. Rules that keep needed code are in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true   // lets us show BuildConfig.VERSION_NAME on the Settings screen
    }
}

room {
    // Room writes a JSON description of every database version here.
    // Keep this folder in git: it is used to write and test migrations.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Jetpack Compose + Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Room (local database)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (app preferences: theme, onboarding, reminders)
    implementation(libs.androidx.datastore.preferences)

    // WorkManager (savings reminders)
    implementation(libs.androidx.work.runtime.ktx)

    // Glance (home-screen widget)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // JSON for navigation routes and backup files
    implementation(libs.kotlinx.serialization.json)

    // Coroutines (declared directly so the app and the tests use the same version)
    implementation(libs.kotlinx.coroutines.android)

    // Unit tests (run on your computer, no phone needed)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests (run on a phone/emulator)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
