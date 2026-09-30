plugins {
    alias(libs.plugins.mediakit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.media.api)
            }
        }
    }
}
