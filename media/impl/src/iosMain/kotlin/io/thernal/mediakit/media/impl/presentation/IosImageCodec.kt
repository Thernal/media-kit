package io.thernal.mediakit.media.impl.presentation

import io.thernal.mediakit.media.impl.domain.ImageCodec
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation

private const val JPEG_QUALITY = 0.9

internal object IosImageCodec : ImageCodec {
    override suspend fun transcodeToJpeg(bytes: ByteArray): ByteArray? {
        return withContext(Dispatchers.Default) {
            UIImage.imageWithData(bytes.toNSData())?.uprightJpeg()
        }
    }

    override suspend fun normalizeOrientation(bytes: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) {
            val image = UIImage.imageWithData(bytes.toNSData()) ?: return@withContext bytes
            if (image.imageOrientation == UIImageOrientation.UIImageOrientationUp) {
                return@withContext bytes
            }
            image.uprightJpeg() ?: bytes
        }
    }
}

/** The image redrawn with its orientation applied, as JPEG at scale 1 — pixels, not points. */
@OptIn(ExperimentalForeignApi::class)
internal fun UIImage.uprightJpeg(quality: Double = JPEG_QUALITY): ByteArray? {
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
    val (width, height) = size.useContents { width * scale to height * scale }
    val renderer = UIGraphicsImageRenderer(size = platform.CoreGraphics.CGSizeMake(width, height), format = format)
    val upright = renderer.imageWithActions { _ ->
        drawInRect(CGRectMake(x = 0.0, y = 0.0, width = width, height = height))
    }
    return UIImageJPEGRepresentation(upright, quality)?.toByteArray()
}
