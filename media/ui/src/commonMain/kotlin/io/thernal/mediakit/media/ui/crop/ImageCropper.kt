package io.thernal.mediakit.media.ui.crop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage

private const val TURN_ANIMATION_MILLIS = 220
private const val GRID_DIVISIONS = 3
private const val SCRIM_ALPHA = 0.6f
private const val GRID_ALPHA = 0.3f

/** The cropper's drawing: the dimmed outside, the frame, its rule-of-thirds grid and corner handles. */
@Immutable
data class ImageCropperStyle(
    val scrim: Color = Color.Black.copy(alpha = SCRIM_ALPHA),
    val frame: Color = Color.White,
    val grid: Color = Color.White.copy(alpha = GRID_ALPHA),
    val frameWidth: Dp = 2.dp,
    val handleLength: Dp = 20.dp,
    val handleWidth: Dp = 4.dp,
    /** How near a corner a touch must start to resize rather than move. */
    val touchSlop: Dp = 40.dp,
)

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

private class AnimatedPose(
    rotationState: State<Float>,
    baseScaleState: State<Float>,
    mirrorState: State<Float>,
) {
    val rotation by rotationState
    val baseScale by baseScaleState
    val mirror by mirrorState
}

@Composable
private fun rememberAnimatedPose(state: ImageCropState): AnimatedPose {
    // Before the mirror the rotation is negated, so the visual pose is the same and nothing animates.
    val visualRotation = if (state.isMirrored) {
        -state.rotation
    } else {
        state.rotation
    }
    // Accumulated without the 360° wrap, so each quarter turn animates the short way round.
    var unwrapped by remember { mutableFloatStateOf(visualRotation) }
    LaunchedEffect(visualRotation) {
        val delta = (visualRotation - unwrapped) % FULL_TURN
        unwrapped += (delta + FULL_TURN + HALF_TURN) % FULL_TURN - HALF_TURN
    }
    val spec = tween<Float>(TURN_ANIMATION_MILLIS)
    val mirrorTarget = if (state.isMirrored) {
        -1f
    } else {
        1f
    }
    return AnimatedPose(
        rotationState = animateFloatAsState(targetValue = -unwrapped, animationSpec = spec, label = "cropRotation"),
        baseScaleState = animateFloatAsState(
            targetValue = state.baseScale,
            animationSpec = spec,
            label = "cropBaseScale",
        ),
        mirrorState = animateFloatAsState(targetValue = mirrorTarget, animationSpec = spec, label = "cropMirror"),
    )
}

@Composable
private fun CropOverlay(
    state: ImageCropState,
    style: ImageCropperStyle,
    modifier: Modifier,
) {
    val slop = with(LocalDensity.current) { style.touchSlop.toPx() }
    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                val frame = state.frameRect
                if (frame != Rect.Zero) {
                    drawScrim(frame = frame, color = style.scrim)
                    drawGrid(frame = frame, color = style.grid, width = style.frameWidth.toPx() / 2f)
                    drawRect(
                        color = style.frame,
                        topLeft = frame.topLeft,
                        size = frame.size,
                        style = Stroke(style.frameWidth.toPx()),
                    )
                    drawHandles(
                        frame = frame,
                        color = style.frame,
                        length = style.handleLength.toPx(),
                        width = style.handleWidth.toPx(),
                    )
                }
            }
            .pointerInput(state) { awaitEachGesture { trackCropGesture(state = state, slop = slop) } },
    )
}

/** One gesture: a corner resizes, inside the frame moves it, outside pans; a second finger zooms. */
private suspend fun AwaitPointerEventScope.trackCropGesture(
    state: ImageCropState,
    slop: Float,
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    val frameAtStart = state.frameRect
    val handle = detectHandle(position = down.position, frame = frameAtStart, threshold = slop)
    val isInsideFrame = handle == null && frameAtStart.contains(down.position)
    var previous = down.position
    var isMultiTouch = false
    do {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        isMultiTouch = isMultiTouch || pressed.size > 1
        if (isMultiTouch) {
            state.zoom(zoomDelta = event.calculateZoom(), panDelta = event.calculatePan())
            pressed.forEach { it.consume() }
        } else if (pressed.size == 1) {
            val change = pressed.first()
            val delta = change.position - previous
            previous = change.position
            when {
                handle != null -> state.resizeFrame(handle = handle, delta = delta)
                isInsideFrame -> state.moveFrame(delta)
                else -> state.pan(delta)
            }
            change.consume()
        }
    } while (event.changes.any { it.pressed })
}

private fun DrawScope.drawScrim(
    frame: Rect,
    color: Color,
) {
    drawRect(color = color, topLeft = Offset.Zero, size = Size(width = size.width, height = frame.top))
    drawRect(
        color = color,
        topLeft = Offset(x = 0f, y = frame.bottom),
        size = Size(width = size.width, height = size.height - frame.bottom),
    )
    drawRect(
        color = color,
        topLeft = Offset(x = 0f, y = frame.top),
        size = Size(width = frame.left, height = frame.height),
    )
    drawRect(
        color = color,
        topLeft = Offset(x = frame.right, y = frame.top),
        size = Size(width = size.width - frame.right, height = frame.height),
    )
}

private fun DrawScope.drawGrid(
    frame: Rect,
    color: Color,
    width: Float,
) {
    for (line in 1 until GRID_DIVISIONS) {
        val x = frame.left + frame.width * line / GRID_DIVISIONS
        val y = frame.top + frame.height * line / GRID_DIVISIONS
        drawLine(
            color = color,
            start = Offset(x = x, y = frame.top),
            end = Offset(x = x, y = frame.bottom),
            strokeWidth = width,
        )
        drawLine(
            color = color,
            start = Offset(x = frame.left, y = y),
            end = Offset(x = frame.right, y = y),
            strokeWidth = width,
        )
    }
}

private fun DrawScope.drawHandles(
    frame: Rect,
    color: Color,
    length: Float,
    width: Float,
) {
    listOf(
        frame.topLeft to Offset(x = 1f, y = 1f),
        frame.topRight to Offset(x = -1f, y = 1f),
        frame.bottomLeft to Offset(x = 1f, y = -1f),
        frame.bottomRight to Offset(x = -1f, y = -1f),
    ).forEach { (corner, direction) ->
        drawLine(
            color = color,
            start = corner,
            end = corner + Offset(x = length * direction.x, y = 0f),
            strokeWidth = width,
        )
        drawLine(
            color = color,
            start = corner,
            end = corner + Offset(x = 0f, y = length * direction.y),
            strokeWidth = width,
        )
    }
}
