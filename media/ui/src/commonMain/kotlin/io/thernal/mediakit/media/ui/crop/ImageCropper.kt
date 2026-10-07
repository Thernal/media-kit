package io.thernal.mediakit.media.ui.crop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage

/**
 * The image under a movable, resizable crop frame. One finger on a corner resizes, inside the frame
 * moves it, outside pans a zoomed image; two fingers zoom. Rotation, mirror and ratio are [state]'s
 * operations; the result is `state.crop()`. No chrome — see `ImageCropScreen` for a whole screen.
 */
@Composable
fun ImageCropper(
    state: ImageCropState,
    modifier: Modifier = Modifier,
    style: ImageCropperStyle = ImageCropperStyle(),
) {
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var intrinsicSize by remember { mutableStateOf(Size.Zero) }
    LaunchedEffect(key1 = containerSize, key2 = intrinsicSize) {
        if (containerSize.width > 0f && intrinsicSize.width > 0f) {
            state.onLayout(containerSize = containerSize, intrinsicSize = intrinsicSize)
        }
    }
    val pose = rememberAnimatedPose(state)
    val mirrorSign = if (state.isMirrored) {
        -1f
    } else {
        1f
    }
    Box(modifier = modifier.onSizeChanged { containerSize = it.toSize() }) {
        AsyncImage(
            model = state.file.bytes,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onSuccess = { success -> intrinsicSize = success.painter.intrinsicSize },
            modifier = Modifier
                .fillMaxSize()
                // The mirror is a screen-space flip, outside the rotation.
                .graphicsLayer { scaleX = pose.mirror }
                .graphicsLayer {
                    scaleX = state.imageScale * pose.baseScale
                    scaleY = state.imageScale * pose.baseScale
                    translationX = state.imageOffset.x * mirrorSign
                    translationY = state.imageOffset.y
                    rotationZ = pose.rotation
                },
        )
        CropOverlay(state = state, style = style, modifier = Modifier.fillMaxSize())
    }
}
