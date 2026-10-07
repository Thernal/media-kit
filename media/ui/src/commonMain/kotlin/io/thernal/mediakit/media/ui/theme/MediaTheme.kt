package io.thernal.mediakit.media.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalMediaStyles = staticCompositionLocalOf { MediaStyles() }

/** Installs [styles] for the cropper, the crop screen and zoomable images below. The app's root wraps its content once. */
@Composable
fun MediaTheme(
    styles: MediaStyles,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMediaStyles provides styles) {
        content()
    }
}

object MediaTheme {
    val styles: MediaStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalMediaStyles.current
}
