// An app's design system, small: the tokens the sample is drawn with, and the one file that maps them
// onto media-kit's styles. It is the sample's, never copied into an app — an app maps its own.
plugins {
    alias(libs.plugins.mediakit.compose)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.media.ui)
            }
        }
    }
}
