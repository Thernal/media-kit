package io.thernal.mediakit.sample.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.zacsweers.metro.createGraph
import io.thernal.mediakit.media.api.domain.AspectRatio
import io.thernal.mediakit.media.api.domain.CropOptions
import io.thernal.mediakit.media.api.domain.ImageSource
import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.api.domain.Rejection
import io.thernal.mediakit.media.api.presentation.rememberFilePicker
import io.thernal.mediakit.media.api.presentation.rememberImagePicker
import io.thernal.mediakit.media.ui.crop.ImageCropScreen
import io.thernal.mediakit.media.ui.crop.rememberImageCropState
import io.thernal.mediakit.media.ui.zoom.ZoomableImage

private const val MAX_IMAGES = 5
private const val KILOBYTE = 1024L
private const val MAX_BYTES = 20 * KILOBYTE * KILOBYTE

/** The sample's root: installs the graph's composition locals, as an application's root does. */
@Composable
fun SampleApp() {
    val graph = remember { createGraph<SampleGraph>() }
    // Once, at the root: the copy the spread makes is not worth avoiding.
    @Suppress("SpreadOperator")
    CompositionLocalProvider(*graph.compositionLocals.toTypedArray()) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                SampleNavigation()
            }
        }
    }
}

private sealed interface Screen {
    data object Pick : Screen

    data class Crop(val file: MediaFile) : Screen

    data class Preview(val file: MediaFile) : Screen
}

@Composable
private fun SampleNavigation() {
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

@Composable
private fun PickScreen(
    picked: List<MediaFile>,
    message: String,
    onResult: (PickResult) -> Unit,
    onOpen: (MediaFile) -> Unit,
) {
    val images = rememberImagePicker(maxItems = MAX_IMAGES, maxFileSizeBytes = MAX_BYTES, onResult = onResult)
    val files = rememberFilePicker(
        mimeTypes = listOf(MimeType.Application.PDF, MimeType.Image.ALL),
        maxFileSizeBytes = MAX_BYTES,
        onResult = onResult,
    )
    Column(
        modifier = Modifier.safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "media-kit", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { images.pick(ImageSource.Gallery) }) { Text(text = "Gallery (up to $MAX_IMAGES)") }
        Button(onClick = { images.pick(ImageSource.Camera) }) { Text(text = "Camera") }
        OutlinedButton(onClick = { images.pick(ImageSource.Documents) }) { Text(text = "Image from files") }
        OutlinedButton(onClick = files::pick) { Text(text = "PDF or image file") }
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            picked.filter { it.mimeType.startsWith("image/") }.forEach { file ->
                AsyncImage(
                    model = file.bytes,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp).clickable { onOpen(file) },
                )
            }
        }
        if (picked.any { it.mimeType.startsWith("image/") }) {
            Text(text = "Tap an image to crop it", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun PickResult.describe(): String {
    return when (this) {
        is PickResult.Picked -> buildString {
            append("Picked ${files.size}: ${files.joinToString { "${it.name} (${it.size / KILOBYTE} KB)" }}")
            if (rejected.isNotEmpty()) {
                append("\nRejected: ${rejected.joinToString { it.describe() }}")
            }
        }

        is PickResult.Rejected -> "Rejected: ${rejections.joinToString { it.describe() }}"

        PickResult.Cancelled -> "Cancelled"

        PickResult.PermissionDenied -> "Camera permission denied"

        PickResult.Unavailable -> "Not available here (no camera?)"
    }
}

private fun Rejection.describe(): String {
    return when (this) {
        is Rejection.TooLarge -> "$name is ${actualBytes / KILOBYTE} KB, over ${limitBytes / KILOBYTE} KB"
        is Rejection.UnsupportedType -> "$name is $mimeType"
        is Rejection.Unreadable -> "$name could not be read"
    }
}
