package io.thernal.mediakit.sample.designsystem

import androidx.compose.runtime.Immutable

/** Every token of the sample's design system, installed with [SampleTheme]. */
@Immutable
data class SampleTokens(
    val colors: SampleColors = SampleColors(),
    val shapes: SampleShapes = SampleShapes(),
    val spacing: SampleSpacing = SampleSpacing(),
    val typography: SampleTypography = SampleTypography(),
    val motion: SampleMotion = SampleMotion(),
)
