package io.thernal.mediakit.media.api.presentation

import androidx.compose.runtime.Composable
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.PickResult

/**
 * An image picker: gallery (up to [maxItems] images), camera, or documents. Pick with
 * `picker.pick(ImageSource.Gallery)`; [onResult] receives the outcome.
 */
@Composable
fun rememberImagePicker(
    maxItems: Int = 1,
    maxFileSizeBytes: Long? = null,
    onResult: (PickResult) -> Unit,
): ImagePicker {
    return LocalMediaPickerProvider.current.rememberImagePicker(
        options = ImagePickerOptions(maxItems = maxItems, maxFileSizeBytes = maxFileSizeBytes),
        onResult = onResult,
    )
}

/** A document picker for [mimeTypes]; [onResult] receives the outcome. */
@Composable
fun rememberFilePicker(
    mimeTypes: List<String> = listOf(MimeType.ALL),
    maxFileSizeBytes: Long? = null,
    onResult: (PickResult) -> Unit,
): FilePicker {
    return LocalMediaPickerProvider.current.rememberFilePicker(
        options = FilePickerOptions(mimeTypes = mimeTypes, maxFileSizeBytes = maxFileSizeBytes),
        onResult = onResult,
    )
}
