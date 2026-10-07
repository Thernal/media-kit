package io.thernal.mediakit.media.api.presentation

import androidx.compose.runtime.Stable

/** Opens the system document picker; the result arrives in the `onResult` given to `rememberFilePicker`. */
@Stable
fun interface FilePicker {
    fun pick()
}
