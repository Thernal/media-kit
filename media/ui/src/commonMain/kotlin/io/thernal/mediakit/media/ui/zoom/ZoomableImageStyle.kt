package io.thernal.mediakit.media.ui.zoom

import androidx.compose.runtime.Immutable

/** How `ZoomableImage` (and `zoomGestures`) moves. Neutral defaults; map the app's motion in `MediaStyles`. */
@Immutable
data class ZoomableImageStyle(
    /** How long a double tap takes to zoom in or back out. */
    val doubleTapMillis: Int = DEFAULT_DOUBLE_TAP_MILLIS,
)

private const val DEFAULT_DOUBLE_TAP_MILLIS = 300
