package io.thernal.mediakit.media.testing

import io.thernal.mediakit.media.api.domain.ImageSource
import io.thernal.mediakit.media.api.presentation.FilePickerOptions
import io.thernal.mediakit.media.api.presentation.ImagePickerOptions

/** One call a feature made on a picker. */
sealed interface PickRequest {
    data class Image(
        val source: ImageSource,
        val options: ImagePickerOptions,
    ) : PickRequest

    data class File(val options: FilePickerOptions) : PickRequest
}
