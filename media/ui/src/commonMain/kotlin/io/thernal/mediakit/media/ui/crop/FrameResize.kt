package io.thernal.mediakit.media.ui.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

private val CropHandle.doesMoveLeftEdge: Boolean
    get() = this == CropHandle.TopLeft || this == CropHandle.BottomLeft

private val CropHandle.doesMoveTopEdge: Boolean
    get() = this == CropHandle.TopLeft || this == CropHandle.TopRight

/** [frame] with the [handle]'s corner dragged, each edge kept inside [bounds] and apart by the minimum size. */
internal fun resizedFree(
    frame: Rect,
    handle: CropHandle,
    delta: Offset,
    bounds: Rect,
): Rect {
    val horizontal = resizedEdges(
        near = frame.left,
        far = frame.right,
        moveNear = handle.doesMoveLeftEdge,
        delta = delta.x,
        boundNear = bounds.left,
        boundFar = bounds.right,
    )
    val vertical = resizedEdges(
        near = frame.top,
        far = frame.bottom,
        moveNear = handle.doesMoveTopEdge,
        delta = delta.y,
        boundNear = bounds.top,
        boundFar = bounds.bottom,
    )
    return Rect(left = horizontal.first, top = vertical.first, right = horizontal.second, bottom = vertical.second)
}

/**
 * [frame] resized from the [handle] keeping [ratio]: the horizontal drag decides the width and the
 * height follows; where the height would leave [bounds] it stops there and the width is taken back.
 */
internal fun resizedLocked(
    frame: Rect,
    handle: CropHandle,
    deltaX: Float,
    bounds: Rect,
    ratio: Float,
): Rect {
    val (left, right) = resizedEdges(
        near = frame.left,
        far = frame.right,
        moveNear = handle.doesMoveLeftEdge,
        delta = deltaX,
        boundNear = bounds.left,
        boundFar = bounds.right,
    )
    val room = if (handle.doesMoveTopEdge) {
        frame.bottom - bounds.top
    } else {
        bounds.bottom - frame.top
    }
    val height = minOf(a = (right - left) / ratio, b = room)
    val width = height * ratio
    val keptLeft = if (handle.doesMoveLeftEdge) {
        (frame.right - width).coerceAtLeast(bounds.left)
    } else {
        frame.left
    }
    val keptRight = if (handle.doesMoveLeftEdge) {
        frame.right
    } else {
        (frame.left + width).coerceAtMost(bounds.right)
    }
    return if (handle.doesMoveTopEdge) {
        Rect(left = keptLeft, top = frame.bottom - height, right = keptRight, bottom = frame.bottom)
    } else {
        Rect(left = keptLeft, top = frame.top, right = keptRight, bottom = frame.top + height)
    }
}

/** One axis of a resize: the dragged edge moves by [delta], within its bound and the minimum size. */
private fun resizedEdges(
    near: Float,
    far: Float,
    moveNear: Boolean,
    delta: Float,
    boundNear: Float,
    boundFar: Float,
): Pair<Float, Float> {
    return if (moveNear) {
        (near + delta).coerceIn(minimumValue = boundNear, maximumValue = far - MIN_FRAME_SIZE) to far
    } else {
        near to (far + delta).coerceIn(minimumValue = near + MIN_FRAME_SIZE, maximumValue = boundFar)
    }
}
