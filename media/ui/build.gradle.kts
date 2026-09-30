plugins {
    alias(libs.plugins.mediakit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            // Nothing is re-exported: a feature that shows the cropper declares api and Coil itself when
            // its own code uses their types (media/api/README.md → Dependencies you declare).
            dependencies {
                implementation(projects.media.api)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.coil.compose)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.exifinterface)
            }
        }
    }
}
