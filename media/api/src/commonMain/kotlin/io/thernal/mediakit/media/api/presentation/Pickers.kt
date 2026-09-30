package io.thernal.mediakit.media.api.presentation

import androidx.compose.runtime.Stable
import io.thernal.mediakit.media.api.domain.ImageSource

/** Opens an image picker; the result arrives in the `onResult` given to `rememberImagePicker`. */
@Stable
fun interface ImagePicker {
    fun pick(source: ImageSource)
}

/** Opens the system document picker; the result arrives in the `onResult` given to `rememberFilePicker`. */
@Stable
fun interface FilePicker {
    fun pick()
}
