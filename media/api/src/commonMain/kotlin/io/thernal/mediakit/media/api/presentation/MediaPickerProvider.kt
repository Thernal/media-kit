package io.thernal.mediakit.media.api.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * The provider the composables below use. Without `wiring` — an isolated preview — every pick is
 * [PickResult.Cancelled] and nothing opens.
 */
val LocalMediaPickerProvider = staticCompositionLocalOf<MediaPickerProvider> { PreviewMediaPickerProvider }

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
