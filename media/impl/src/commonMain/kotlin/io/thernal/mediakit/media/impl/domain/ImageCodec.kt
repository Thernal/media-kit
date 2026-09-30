package io.thernal.mediakit.media.impl.domain

import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.Rejection

/** The platform half of preparing a picked image. Both calls run off the main thread. */
internal interface ImageCodec {
    /** HEIC/HEIF → JPEG, orientation applied; `null` when this device cannot decode it. */
    suspend fun transcodeToJpeg(bytes: ByteArray): ByteArray?

    /** A JPEG whose EXIF orientation is not "up", redrawn upright; anything else returned as is. */
    suspend fun normalizeOrientation(bytes: ByteArray): ByteArray
}

private val HEIC_TYPES = setOf(MimeType.Image.HEIC, MimeType.Image.HEIF)

/**
 * Every picked image passes through here, whatever its source: HEIC becomes JPEG, a rotated JPEG is
 * redrawn upright, anything the crop engine and Coil cannot decode on both platforms (SVG, RAW) is
 * refused now rather than failing on the crop screen, and the size limit applies to the final bytes.
 */
internal suspend fun prepareImage(
    file: MediaFile,
    maxFileSizeBytes: Long?,
    codec: ImageCodec,
): PickedItem {
    val prepared = when (file.mimeType.lowercase()) {
        in HEIC_TYPES -> {
            val jpeg = codec.transcodeToJpeg(file.bytes)
                ?: return PickedItem.Refused(Rejection.UnsupportedType(name = file.name, mimeType = file.mimeType))
            file.copy(
                name = file.name.substringBeforeLast('.') + ".jpg",
                bytes = jpeg,
                mimeType = MimeType.Image.JPEG,
                // The original URI still points at HEIC content; the transcoded bytes are the file now.
                uri = null,
            )
        }

        MimeType.Image.JPEG -> file.copy(bytes = codec.normalizeOrientation(file.bytes))

        else -> file
    }
    val rejection = checkFile(
        name = prepared.name,
        mimeType = prepared.mimeType,
        size = prepared.size,
        allowedMimeTypes = MimeType.Image.SUPPORTED,
        maxFileSizeBytes = maxFileSizeBytes,
    )
    return rejection?.let(PickedItem::Refused) ?: PickedItem.Accepted(prepared)
}
