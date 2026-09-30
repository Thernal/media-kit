package io.thernal.mediakit.media.ui.crop

import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.NormalizedCropRect

/**
 * Cuts [rect] out of [source] (its pixels as displayed — EXIF orientation applied), then turns the
 * piece [rotation] degrees counter-clockwise, flipping it first when [mirrored], scales it down to
 * [maxOutputSize] on its longest side if given, and encodes it as JPEG. Throws when the source cannot
 * be decoded.
 */
internal expect suspend fun cropImage(
    source: MediaFile,
    rect: NormalizedCropRect,
    rotation: Float,
    mirrored: Boolean,
    maxOutputSize: Int?,
    jpegQuality: Int,
): MediaFile

/** `photo.png` → `photo_cropped.jpg`. */
internal fun croppedName(sourceName: String): String {
    return sourceName.substringBeforeLast(delimiter = '.').ifBlank { "image" } + "_cropped.jpg"
}

/** The size to scale ([width], [height]) to so its longest side is at most [maxSize]; unchanged if it is. */
internal fun scaledToFit(
    width: Int,
    height: Int,
    maxSize: Int?,
): Pair<Int, Int> {
    val longest = maxOf(a = width, b = height)
    if (maxSize == null || longest <= maxSize) {
        return width to height
    }
    val factor = maxSize.toDouble() / longest
    return maxOf(a = 1, b = (width * factor).toInt()) to maxOf(a = 1, b = (height * factor).toInt())
}
