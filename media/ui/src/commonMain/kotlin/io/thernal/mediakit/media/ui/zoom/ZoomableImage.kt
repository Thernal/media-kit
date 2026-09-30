package io.thernal.mediakit.media.ui.zoom

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage

/**
 * An image the user can pinch, double-tap and pan — a full-screen photo viewer's content. [model] is
 * anything Coil loads: a URL (with the app's Coil network fetcher), a `MediaFile`'s bytes, a file path.
 */
@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    state: ZoomableState = rememberZoomableState(),
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
            .graphicsLayer {
                scaleX = state.scale
                scaleY = state.scale
                translationX = state.offset.x
                translationY = state.offset.y
            }
            .zoomGestures(state),
    )
}

/** The gestures of [ZoomableImage], for any content that should zoom the same way. */
@Composable
fun Modifier.zoomGestures(state: ZoomableState): Modifier {
    val scope = rememberCoroutineScope()
    return this
        .pointerInput(state) {
            detectTapGestures(
                onDoubleTap = { tap -> state.doubleTap(tap = tap, viewSize = size.toSize(), scope = scope) },
            )
        }
        .pointerInput(state) {
            awaitEachGesture {
                var isMultiTouch = false
                do {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.count { it.pressed }
                    isMultiTouch = isMultiTouch || pressed > 1
                    if (isMultiTouch) {
                        state.pinch(
                            zoomChange = event.calculateZoom(),
                            pan = event.calculatePan(),
                            viewSize = size.toSize(),
                        )
                        event.consumePressed()
                    } else if (pressed == 1 && state.scale > MIN_ZOOM) {
                        // At 1× a one-finger drag is left to the parent (a pager, a dismiss gesture).
                        if (state.panWithOneFinger(pan = event.calculatePan(), viewSize = size.toSize())) {
                            event.consumePressed()
                        }
                    }
                } while (event.changes.any { it.pressed })
            }
        }
}

private fun PointerEvent.consumePressed() {
    changes.filter { it.pressed }.forEach { it.consume() }
}
