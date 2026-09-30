package io.thernal.mediakit.media.ui.zoom

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

internal const val MIN_ZOOM = 1f
internal const val MID_ZOOM = 2.5f
internal const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_MILLIS = 300
private const val PAN_BASE_SPEED = 1.35f
private const val PAN_MAX_SPEED = 2.6f
private const val PAN_CONSUME_EPSILON = 0.1f

@Composable
fun rememberZoomableState(): ZoomableState {
    return remember { ZoomableState() }
}

/**
 * Zoom and offset of a [ZoomableImage]: pinch within 1–4×, double tap steps 1× → 2.5× → 4× → 1× around
 * the tapped point, one finger pans a zoomed image (faster the deeper the zoom). The offset never lets
 * the image uncover the view.
 */
@Stable
class ZoomableState {
    var scale by mutableFloatStateOf(MIN_ZOOM)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    private var animation: Job? = null

    /** Back to 1×, centred. */
    fun reset() {
        animation?.cancel()
        scale = MIN_ZOOM
        offset = Offset.Zero
    }

    internal fun pinch(
        zoomChange: Float,
        pan: Offset,
        viewSize: Size,
    ) {
        animation?.cancel()
        scale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        offset = if (scale <= MIN_ZOOM) {
            Offset.Zero
        } else {
            bounded(
                offset = offset + pan,
                scale = scale,
                viewSize = viewSize,
            )
        }
    }

    /** Returns whether the image moved — if not, the gesture belongs to whatever is around it. */
    internal fun panWithOneFinger(
        pan: Offset,
        viewSize: Size,
    ): Boolean {
        animation?.cancel()
        val progress = ((scale - MIN_ZOOM) / (MAX_ZOOM - MIN_ZOOM)).coerceIn(0f, 1f)
        val speed = PAN_BASE_SPEED + (PAN_MAX_SPEED - PAN_BASE_SPEED) * progress
        offset = bounded(offset = offset + pan * speed, scale = scale, viewSize = viewSize)
        return abs(pan.x) > PAN_CONSUME_EPSILON || abs(pan.y) > PAN_CONSUME_EPSILON
    }

    internal fun doubleTap(
        tap: Offset,
        viewSize: Size,
        scope: CoroutineScope,
    ) {
        val startScale = scale
        val startOffset = offset
        val targetScale = nextZoomStep(startScale)
        val targetOffset = doubleTapOffset(
            startScale = startScale,
            startOffset = startOffset,
            targetScale = targetScale,
            tap = tap,
            viewSize = viewSize,
        )
        animation?.cancel()
        animation = scope.launch {
            animate(initialValue = 0f, targetValue = 1f, animationSpec = tween(DOUBLE_TAP_MILLIS)) { progress, _ ->
                scale = startScale + (targetScale - startScale) * progress
                offset = startOffset + (targetOffset - startOffset) * progress
            }
        }
    }
}

internal fun nextZoomStep(scale: Float): Float {
    return when {
        scale < (MIN_ZOOM + MID_ZOOM) / 2f -> MID_ZOOM
        scale < (MID_ZOOM + MAX_ZOOM) / 2f -> MAX_ZOOM
        else -> MIN_ZOOM
    }
}

/** The offset that keeps the tapped point in place while zooming from [startScale] to [targetScale]. */
internal fun doubleTapOffset(
    startScale: Float,
    startOffset: Offset,
    targetScale: Float,
    tap: Offset,
    viewSize: Size,
): Offset {
    if (targetScale <= MIN_ZOOM) {
        return Offset.Zero
    }
    val factor = targetScale / startScale
    val center = Offset(x = viewSize.width / 2f, y = viewSize.height / 2f)
    return bounded(
        offset = startOffset * factor + (center - tap) * (factor - 1f),
        scale = targetScale,
        viewSize = viewSize,
    )
}

internal fun bounded(
    offset: Offset,
    scale: Float,
    viewSize: Size,
): Offset {
    val maxX = (scale - 1f) * viewSize.width / 2f
    val maxY = (scale - 1f) * viewSize.height / 2f
    return Offset(x = offset.x.coerceIn(-maxX, maxX), y = offset.y.coerceIn(-maxY, maxY))
}
