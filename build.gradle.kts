// Top-level build file.
// Plugins are declared here with "apply false" so that every module uses the
// same versions (taken from gradle/libs.versions.toml). The app module applies them.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
