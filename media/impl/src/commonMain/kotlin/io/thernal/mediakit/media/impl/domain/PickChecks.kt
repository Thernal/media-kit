package io.thernal.mediakit.media.impl.domain

import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.Rejection

/**
 * The checks a file must pass before its bytes are worth reading: its type, and its size as the
 * platform reports it (`null` when it does not). Run again on the bytes, whose size is exact.
 */
internal fun checkFile(
    name: String,
    mimeType: String,
    size: Long?,
    allowedMimeTypes: List<String>,
    maxFileSizeBytes: Long?,
): Rejection? {
    return when {
        !MimeType.matches(mimeType = mimeType, allowed = allowedMimeTypes) ->
            Rejection.UnsupportedType(name = name, mimeType = mimeType)

        maxFileSizeBytes != null && size != null && size > maxFileSizeBytes ->
            Rejection.TooLarge(name = name, limitBytes = maxFileSizeBytes, actualBytes = size)

        else -> null
    }
}

/** A MIME type from a file name, for platforms that give only the name. */
internal fun mimeTypeForExtension(extension: String): String {
    return when (extension.lowercase()) {
        "jpg", "jpeg" -> MimeType.Image.JPEG
        "png" -> MimeType.Image.PNG
        "gif" -> MimeType.Image.GIF
        "webp" -> MimeType.Image.WEBP
        "heic" -> MimeType.Image.HEIC
        "heif" -> MimeType.Image.HEIF
        "pdf" -> MimeType.Application.PDF
        "mp4" -> MimeType.Video.MP4
        "mov" -> MimeType.Video.QUICKTIME
        else -> MimeType.Application.OCTET_STREAM
    }
}
