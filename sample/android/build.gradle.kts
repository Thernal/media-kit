plugins {
    alias(libs.plugins.mediakit.android.application)
}

dependencies {
    implementation(projects.sample.shared)
    implementation(libs.androidx.activity.compose)
}
