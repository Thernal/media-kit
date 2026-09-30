package io.thernal.mediakit.media.api.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.PickResult

/**
 * Opens the platform pickers and reads what they return. `impl` holds the platform code — the Android
 * activity-result launchers, the iOS view controllers — so a feature compiles against this contract
 * only; `wiring` installs the real one as [LocalMediaPickerProvider].
 */
interface MediaPickerProvider {
    @Composable
    fun rememberImagePicker(
        options: ImagePickerOptions,
        onResult: (PickResult) -> Unit,
    ): ImagePicker

    @Composable
    fun rememberFilePicker(
        options: FilePickerOptions,
        onResult: (PickResult) -> Unit,
    ): FilePicker
}

data class ImagePickerOptions(
    /** How many images the gallery lets the user choose; the camera and documents always give one. */
    val maxItems: Int = 1,
    /** Files larger than this — after HEIC transcoding and orientation — are rejected. */
    val maxFileSizeBytes: Long? = null,
) {
    init {
        require(maxItems >= 1) { "maxItems must be at least 1, was $maxItems" }
    }
}

data class FilePickerOptions(
    /** MIME types, or whole families such as [MimeType.Image.ALL], the picker offers and accepts. */
    val mimeTypes: List<String> = listOf(MimeType.ALL),
    val maxFileSizeBytes: Long? = null,
)

/**
 * The provider the composables below use. Without `wiring` — an isolated preview — every pick is
 * [PickResult.Cancelled] and nothing opens.
 */
val LocalMediaPickerProvider = staticCompositionLocalOf<MediaPickerProvider> { PreviewMediaPickerProvider }

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

private object PreviewMediaPickerProvider : MediaPickerProvider {
    @Composable
    override fun rememberImagePicker(
        options: ImagePickerOptions,
        onResult: (PickResult) -> Unit,
    ): ImagePicker {
        return ImagePicker { onResult(PickResult.Cancelled) }
    }

    @Composable
    override fun rememberFilePicker(
        options: FilePickerOptions,
        onResult: (PickResult) -> Unit,
    ): FilePicker {
        return FilePicker { onResult(PickResult.Cancelled) }
    }
}
