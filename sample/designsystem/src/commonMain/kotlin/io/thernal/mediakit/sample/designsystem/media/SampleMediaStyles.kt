package io.thernal.mediakit.sample.designsystem.media

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import io.thernal.mediakit.media.ui.crop.CropRatioChipStyle
import io.thernal.mediakit.media.ui.crop.ImageCropScreenStyle
import io.thernal.mediakit.media.ui.crop.ImageCropperStyle
import io.thernal.mediakit.media.ui.theme.MediaStyles
import io.thernal.mediakit.media.ui.zoom.ZoomableImageStyle
import io.thernal.mediakit.sample.designsystem.SampleTheme

/**
 * The design system's tokens mapped onto media-kit's styles — the one file an app writes in its own
 * design system, so the cropper and the zoomable image look like the rest of the app and change with it.
 * The root installs it:
 *
 * ```
 * SampleTheme {
 *     MediaTheme(styles = sampleMediaStyles()) {
 *         App()
 *     }
 * }
 * ```
 *
 * Only looks are mapped; behaviour (the corner touch slop) keeps the kit's defaults.
 */
@Composable
@ReadOnlyComposable
fun sampleMediaStyles(): MediaStyles {
    val colors = SampleTheme.colors
    val shapes = SampleTheme.shapes
    val spacing = SampleTheme.spacing
    val typography = SampleTheme.typography
    val motion = SampleTheme.motion
    return MediaStyles(
        cropper = ImageCropperStyle(
            scrim = colors.scrim,
            frame = colors.onCanvas,
            grid = colors.line,
            frameWidth = spacing.stroke,
            turnMillis = motion.fastMillis,
        ),
        cropScreen = ImageCropScreenStyle(
            backgroundColor = colors.canvas,
            contentColor = colors.onCanvas,
            textStyle = typography.label,
            topBarPadding = PaddingValues(horizontal = spacing.large, vertical = spacing.medium),
            bottomBarPadding = PaddingValues(vertical = spacing.medium),
            bottomBarSpacing = spacing.large,
            ratioRowPadding = PaddingValues(horizontal = spacing.large),
            buttonPadding = PaddingValues(spacing.small),
            ratioChip = CropRatioChipStyle(
                shape = RoundedCornerShape(shapes.pill),
                borderWidth = spacing.hairline,
                borderColor = colors.line,
                contentColor = colors.onCanvas,
                selectedContainerColor = colors.accent,
                selectedContentColor = colors.onAccent,
                contentPadding = PaddingValues(horizontal = spacing.medium, vertical = spacing.extraSmall),
                spacing = spacing.small,
            ),
        ),
        zoom = ZoomableImageStyle(doubleTapMillis = motion.mediumMillis),
    )
}
