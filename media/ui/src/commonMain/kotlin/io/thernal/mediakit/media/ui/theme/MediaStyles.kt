package io.thernal.mediakit.media.ui.theme

import androidx.compose.runtime.Immutable
import io.thernal.mediakit.media.ui.crop.ImageCropScreenStyle
import io.thernal.mediakit.media.ui.crop.ImageCropperStyle
import io.thernal.mediakit.media.ui.zoom.ZoomableImageStyle

/**
 * Every look the kit's UI draws in one value — the place an app maps its design system's tokens to,
 * once, in its own design-system module. Each composable can still take its own style; this is what it
 * gets when it does not.
 */
@Immutable
data class MediaStyles(
    val cropper: ImageCropperStyle = ImageCropperStyle(),
    val cropScreen: ImageCropScreenStyle = ImageCropScreenStyle(),
    val zoom: ZoomableImageStyle = ZoomableImageStyle(),
)
