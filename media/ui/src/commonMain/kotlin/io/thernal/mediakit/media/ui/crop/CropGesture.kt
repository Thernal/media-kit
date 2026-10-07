package io.thernal.mediakit.media.ui.crop

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.geometry.Offset

/** One gesture: a corner resizes, inside the frame moves it, outside pans; a second finger zooms. */
internal suspend fun AwaitPointerEventScope.trackCropGesture(
    state: ImageCropState,
    slop: Float,
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    val frameAtStart = state.frameRect
    val handle = detectHandle(position = down.position, frame = frameAtStart, threshold = slop)
    val drag = CropDrag(handle = handle, isInsideFrame = handle == null && frameAtStart.contains(down.position))
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
            previous = drag.apply(state = state, change = pressed.first(), previous = previous)
        }
    } while (event.changes.any { it.pressed })
}

/** What a one-finger drag does, decided where it started. */
private class CropDrag(
    private val handle: CropHandle?,
    private val isInsideFrame: Boolean,
) {
    /** Applies the move from [previous] to [change]'s position; returns that position. */
    fun apply(
        state: ImageCropState,
        change: PointerInputChange,
        previous: Offset,
    ): Offset {
        val delta = change.position - previous
        when {
            handle != null -> state.resizeFrame(handle = handle, delta = delta)
            isInsideFrame -> state.moveFrame(delta)
            else -> state.pan(delta)
        }
        change.consume()
        return change.position
    }
}
