plugins {
    alias(libs.plugins.mediakit.compose)
    alias(libs.plugins.mediakit.injection)
}

kotlin {
    // The framework the iOS sample embeds (sample/ios, built by Xcode through
    // `embedAndSignAppleFrameworkForXcode`). `./gradlew build` links it, so it cannot rot unnoticed.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SampleShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.media.api)
                implementation(projects.media.ui)
                implementation(projects.media.wiring)
                implementation(libs.compose.material3)
                implementation(libs.coil.compose)
            }
        }
    }
}
