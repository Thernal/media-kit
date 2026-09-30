plugins {
    alias(libs.plugins.mediakit.compose)
    alias(libs.plugins.mediakit.injection)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.media.api)
                implementation(projects.media.impl)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.media.testing)
            }
        }
    }
}
