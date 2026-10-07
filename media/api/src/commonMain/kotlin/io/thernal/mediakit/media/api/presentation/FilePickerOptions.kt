package io.thernal.mediakit.media.api.presentation

import io.thernal.mediakit.media.api.domain.MimeType

data class FilePickerOptions(
    /** MIME types, or whole families such as [MimeType.Image.ALL], the picker offers and accepts. */
    val mimeTypes: List<String> = listOf(MimeType.ALL),
    val maxFileSizeBytes: Long? = null,
)
