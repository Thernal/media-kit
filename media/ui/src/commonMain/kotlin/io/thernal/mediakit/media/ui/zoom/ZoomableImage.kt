package io.thernal.mediakit.media.ui.zoom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import io.thernal.mediakit.media.ui.theme.MediaTheme

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
    style: ZoomableImageStyle = MediaTheme.styles.zoom,
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
            .zoomGestures(state = state, style = style),
    )
}
