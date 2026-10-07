package io.thernal.mediakit.media.testing

import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType

/** A small file standing in for a picked image; its bytes are not a real image. */
fun testImage(
    name: String = "image.jpg",
    mimeType: String = MimeType.Image.JPEG,
    size: Int = TEST_FILE_SIZE,
): MediaFile {
    return MediaFile(name = name, bytes = ByteArray(size) { it.toByte() }, mimeType = mimeType)
}

private const val TEST_FILE_SIZE = 16
