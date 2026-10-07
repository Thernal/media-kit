package io.thernal.mediakit.media.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.api.presentation.FilePicker
import io.thernal.mediakit.media.api.presentation.FilePickerOptions
import io.thernal.mediakit.media.api.presentation.ImagePicker
import io.thernal.mediakit.media.api.presentation.ImagePickerOptions
import io.thernal.mediakit.media.api.presentation.MediaPickerProvider

/**
 * Pickers a test or preview drives: every pick is recorded in [requests] and answered at once by
 * [answer] — by default [PickResult.Cancelled].
 *
 * ```
 * val pickers = FakeMediaPickerProvider(answer = { PickResult.Picked(listOf(testImage())) })
 * CompositionLocalProvider(LocalMediaPickerProvider provides pickers) { AvatarScreen() }
 * ```
 */
class FakeMediaPickerProvider(
    var answer: (PickRequest) -> PickResult = { PickResult.Cancelled },
) : MediaPickerProvider {
    private val recorded = mutableListOf<PickRequest>()

    val requests: List<PickRequest>
        get() = recorded.toList()

    /** The image picker a composition would get, for a test without one. */
    fun imagePicker(
        options: ImagePickerOptions = ImagePickerOptions(),
        onResult: (PickResult) -> Unit,
    ): ImagePicker {
        return ImagePicker { source ->
            respond(
                request = PickRequest.Image(source = source, options = options),
                onResult = onResult,
            )
        }
    }

    /** The file picker a composition would get, for a test without one. */
    fun filePicker(
        options: FilePickerOptions = FilePickerOptions(),
        onResult: (PickResult) -> Unit,
    ): FilePicker {
        return FilePicker { respond(request = PickRequest.File(options), onResult = onResult) }
    }

    @Composable
    override fun rememberImagePicker(
        options: ImagePickerOptions,
        onResult: (PickResult) -> Unit,
    ): ImagePicker {
        val deliver by rememberUpdatedState(onResult)
        return remember(options) { imagePicker(options = options, onResult = { deliver(it) }) }
    }

    @Composable
    override fun rememberFilePicker(
        options: FilePickerOptions,
        onResult: (PickResult) -> Unit,
    ): FilePicker {
        val deliver by rememberUpdatedState(onResult)
        return remember(options) { filePicker(options = options, onResult = { deliver(it) }) }
    }

    private fun respond(
        request: PickRequest,
        onResult: (PickResult) -> Unit,
    ) {
        recorded += request
        onResult(answer(request))
    }
}
