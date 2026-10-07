package io.thernal.mediakit.sample.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSampleTokens = staticCompositionLocalOf { SampleTokens() }

/** Installs [tokens] for everything below — the sample's screens and, through the mapping, media-kit's components. */
@Composable
fun SampleTheme(
    tokens: SampleTokens = SampleTokens(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSampleTokens provides tokens) {
        content()
    }
}

object SampleTheme {
    val colors: SampleColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleTokens.current.colors

    val shapes: SampleShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleTokens.current.shapes

    val spacing: SampleSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleTokens.current.spacing

    val typography: SampleTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleTokens.current.typography

    val motion: SampleMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalSampleTokens.current.motion
}
