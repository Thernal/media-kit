package io.thernal.mediakit.media.ui.zoom

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import io.thernal.mediakit.media.ui.theme.MediaTheme

/** The gestures of [ZoomableImage], for any content that should zoom the same way. */
@Composable
fun Modifier.zoomGestures(
    state: ZoomableState,
    style: ZoomableImageStyle = MediaTheme.styles.zoom,
): Modifier {
    val scope = rememberCoroutineScope()
    return this
        .pointerInput(state) {
            detectTapGestures(
                onDoubleTap = { tap ->
                    state.doubleTap(
                        tap = tap,
                        viewSize = size.toSize(),
                        scope = scope,
                        durationMillis = style.doubleTapMillis,
                    )
                },
            )
        }
        .pointerInput(state) { awaitEachGesture { trackZoomGesture(state) } }
}

/** One gesture: a second finger pinches from then on; one finger pans only a zoomed image. */
private suspend fun AwaitPointerEventScope.trackZoomGesture(state: ZoomableState) {
    var isMultiTouch = false
    do {
        val event = awaitPointerEvent()
        val pressed = event.changes.count { it.pressed }
        isMultiTouch = isMultiTouch || pressed > 1
        if (isMultiTouch) {
            state.pinch(zoomChange = event.calculateZoom(), pan = event.calculatePan(), viewSize = size.toSize())
            event.consumePressed()
        } else if (pressed == 1) {
            panWithOneFinger(state = state, event = event, viewSize = size.toSize())
        }
    } while (event.changes.any { it.pressed })
}

/** At 1× a one-finger drag is left to the parent (a pager, a dismiss gesture). */
private fun panWithOneFinger(
    state: ZoomableState,
    event: PointerEvent,
    viewSize: Size,
) {
    if (state.scale > MIN_ZOOM && state.panWithOneFinger(pan = event.calculatePan(), viewSize = viewSize)) {
        event.consumePressed()
    }
}

private fun PointerEvent.consumePressed() {
    changes.filter { it.pressed }.forEach { it.consume() }
}
