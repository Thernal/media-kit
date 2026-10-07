package io.thernal.mediakit.sample.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.thernal.mediakit.media.api.domain.AspectRatio
import io.thernal.mediakit.media.api.domain.CropOptions
import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.ui.crop.ImageCropScreen
import io.thernal.mediakit.media.ui.crop.rememberImageCropState
import io.thernal.mediakit.media.ui.zoom.ZoomableImage

/** Where the sample is: picking, cropping the chosen image, or looking at the result. */
private sealed interface Screen {
    data object Pick : Screen

    data class Crop(val file: MediaFile) : Screen

    data class Preview(val file: MediaFile) : Screen
}

/** The pick screen, and the crop screen over it once an image is chosen. */
@Composable
internal fun SampleNavigation() {
    var screen by remember { mutableStateOf<Screen>(Screen.Pick) }
    var picked by remember { mutableStateOf(emptyList<MediaFile>()) }
    var message by remember { mutableStateOf("Nothing picked yet") }
    when (val current = screen) {
        Screen.Pick -> PickScreen(
            picked = picked,
            message = message,
            onResult = { result ->
                message = result.describe()
                if (result is PickResult.Picked) {
                    picked = result.files
                }
            },
            onOpen = { screen = Screen.Crop(it) },
        )

        is Screen.Crop -> ImageCropScreen(
            state = rememberImageCropState(file = current.file, options = SAMPLE_CROP),
            onCancel = { screen = Screen.Pick },
            onCropped = { screen = Screen.Preview(it) },
            onError = { error ->
                message = "Crop failed: ${error.message.orEmpty()}"
                screen = Screen.Pick
            },
        )

        is Screen.Preview -> Box(modifier = Modifier.fillMaxSize()) {
            ZoomableImage(
                model = current.file.bytes,
                contentDescription = current.file.name,
                modifier = Modifier.fillMaxSize(),
            )
            Button(onClick = { screen = Screen.Pick }, modifier = Modifier.safeDrawingPadding().padding(16.dp)) {
                Text(text = "Back — ${current.file.width ?: 0}×${current.file.height ?: 0}")
            }
        }
    }
}

private val SAMPLE_CROP = CropOptions(initialRatio = AspectRatio.SQUARE, maxOutputSize = 2048)
