package io.thernal.mediakit.sample.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.thernal.mediakit.media.api.domain.ImageSource
import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.api.presentation.rememberFilePicker
import io.thernal.mediakit.media.api.presentation.rememberImagePicker

/** The pickers, the last result and the picked images. */
@Composable
internal fun PickScreen(
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

private const val MAX_IMAGES = 5
private const val MAX_BYTES = 20 * KILOBYTE * KILOBYTE
