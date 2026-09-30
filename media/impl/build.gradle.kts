import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    alias(libs.plugins.mediakit.compose)
}

kotlin {
    // The camera writes into a file this module's own FileProvider shares (res/xml, AndroidManifest).
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        androidResources.enable = true
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.media.api)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core)
                implementation(libs.androidx.exifinterface)
            }
        }
    }
}
