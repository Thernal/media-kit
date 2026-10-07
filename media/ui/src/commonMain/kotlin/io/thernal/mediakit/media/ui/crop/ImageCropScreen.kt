package io.thernal.mediakit.media.ui.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.ui.theme.MediaTheme
import kotlinx.coroutines.launch

/**
 * A whole crop screen: the cropper on the style's background, a top bar (cancel, done) and a bottom bar
 * (ratios, rotate, mirror). Both bars are slots — pass your own built from the app's design system; the
 * defaults in [ImageCropDefaults] draw from [screenStyle]. The labels are parameters of the defaults;
 * the kit ships no strings.
 */
@Composable
fun ImageCropScreen(
    state: ImageCropState,
    onCancel: () -> Unit,
    onCropped: (MediaFile) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
    style: ImageCropperStyle = MediaTheme.styles.cropper,
    screenStyle: ImageCropScreenStyle = MediaTheme.styles.cropScreen,
    topBar: @Composable (done: () -> Unit) -> Unit = { done ->
        ImageCropDefaults.TopBar(state = state, onCancel = onCancel, onDone = done, style = screenStyle)
    },
    bottomBar: @Composable () -> Unit = { ImageCropDefaults.BottomBar(state = state, style = screenStyle) },
) {
    val scope = rememberCoroutineScope()
    val done: () -> Unit = {
        if (!state.isProcessing) {
            scope.launch { state.crop().fold(onSuccess = onCropped, onFailure = onError) }
        }
    }
    Box(modifier = modifier.fillMaxSize().background(screenStyle.backgroundColor)) {
        ImageCropper(state = state, style = style, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            topBar(done)
            Box(modifier = Modifier.weight(1f))
            bottomBar()
        }
    }
}
