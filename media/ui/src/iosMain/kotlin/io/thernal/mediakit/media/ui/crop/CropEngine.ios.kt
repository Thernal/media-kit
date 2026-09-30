package io.thernal.mediakit.media.ui.crop

import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.NormalizedCropRect
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation
import platform.posix.memcpy

private const val PERCENT = 100.0

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun cropImage(
    source: MediaFile,
    rect: NormalizedCropRect,
    rotation: Float,
    mirrored: Boolean,
    maxOutputSize: Int?,
    jpegQuality: Int,
): MediaFile {
    return withContext(Dispatchers.Default) {
        val image = UIImage.imageWithData(source.bytes.toNSData()) ?: error("Cannot decode ${source.name}")
        val upright = image.redrawn(orientation = UIImageOrientation.UIImageOrientationUp, maxSize = null)
            .CGImage ?: error("No pixels in ${source.name}")
        val sourceWidth = CGImageGetWidth(upright).toDouble()
        val sourceHeight = CGImageGetHeight(upright).toDouble()
        val piece = CGImageCreateWithImageInRect(
            upright,
            CGRectMake(
                x = rect.left * sourceWidth,
                y = rect.top * sourceHeight,
                width = rect.width * sourceWidth,
                height = rect.height * sourceHeight,
            ),
        ) ?: error("Crop failed for ${source.name}")
        val turned = UIImage.imageWithCGImage(piece, scale = 1.0, orientation = turnOrientation(rotation, mirrored))
        val result = turned.redrawn(orientation = null, maxSize = maxOutputSize)
        val jpeg = UIImageJPEGRepresentation(result, jpegQuality / PERCENT) ?: error("JPEG encoding failed")
        val (outWidth, outHeight) = result.size.useContents { width.toInt() to height.toInt() }
        MediaFile(
            name = croppedName(source.name),
            bytes = jpeg.toByteArray(),
            mimeType = MimeType.Image.JPEG,
            width = outWidth,
            height = outHeight,
        )
    }
}

/**
 * The image drawn with its orientation applied, at scale 1 and at most [maxSize] on its longest side.
 * [orientation] `Up` shortcuts an image that is already upright.
 */
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.redrawn(
    orientation: UIImageOrientation?,
    maxSize: Int?,
): UIImage {
    if (orientation != null && imageOrientation == orientation && scale == 1.0) {
        return this
    }
    val (pixelWidth, pixelHeight) = size.useContents { (width * scale).toInt() to (height * scale).toInt() }
    val (width, height) = scaledToFit(width = pixelWidth, height = pixelHeight, maxSize = maxSize)
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(width.toDouble(), height.toDouble()), format = format)
    return renderer.imageWithActions { _ ->
        drawInRect(CGRectMake(x = 0.0, y = 0.0, width = width.toDouble(), height = height.toDouble()))
    }
}

/**
 * The UIImage orientation that shows the piece turned [rotation] degrees counter-clockwise, flipped
 * horizontally first when [mirrored] — EXIF's composition order.
 */
internal fun turnOrientation(
    rotation: Float,
    mirrored: Boolean,
): UIImageOrientation {
    val degrees = (rotation.toInt() % FULL_TURN + FULL_TURN) % FULL_TURN
    val (plain, flipped) = when (degrees) {
        QUARTER_TURN -> UIImageOrientation.UIImageOrientationLeft to UIImageOrientation.UIImageOrientationLeftMirrored

        HALF_TURN -> UIImageOrientation.UIImageOrientationDown to UIImageOrientation.UIImageOrientationDownMirrored

        FULL_TURN - QUARTER_TURN ->
            UIImageOrientation.UIImageOrientationRight to UIImageOrientation.UIImageOrientationRightMirrored

        else -> UIImageOrientation.UIImageOrientationUp to UIImageOrientation.UIImageOrientationUpMirrored
    }
    return if (mirrored) {
        flipped
    } else {
        plain
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
    }
    return bytes
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) {
        return NSData()
    }
    return usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
}
