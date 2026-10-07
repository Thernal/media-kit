package io.thernal.mediakit.media.ui.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.NormalizedCropRect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal actual suspend fun cropImage(
    source: MediaFile,
    rect: NormalizedCropRect,
    rotation: Float,
    mirrored: Boolean,
    maxOutputSize: Int?,
    jpegQuality: Int,
): MediaFile {
    return cropOn(
        source = source,
        rect = rect,
        rotation = rotation,
        mirrored = mirrored,
        maxOutputSize = maxOutputSize,
        jpegQuality = jpegQuality,
    )
}

@Suppress("LongParameterList")
private suspend fun cropOn(
    source: MediaFile,
    rect: NormalizedCropRect,
    rotation: Float,
    mirrored: Boolean,
    maxOutputSize: Int?,
    jpegQuality: Int,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): MediaFile {
    return withContext(dispatcher) {
        val decoded = BitmapFactory.decodeByteArray(source.bytes, 0, source.bytes.size)
            ?: error("Cannot decode ${source.name}")
        val upright = decoded.upright(exifOrientation(source.bytes))
        val result = upright.cropped(rect = rect, rotation = rotation, mirrored = mirrored).fitted(maxOutputSize)
        val bytes = result.jpeg(jpegQuality)
        MediaFile(
            name = croppedName(source.name),
            bytes = bytes,
            mimeType = MimeType.Image.JPEG,
            width = result.width,
            height = result.height,
        )
    }
}

private fun exifOrientation(bytes: ByteArray): Int {
    return ByteArrayInputStream(bytes).use { stream ->
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
}

private const val QUARTER = 90f
private const val HALF = 180f
private const val THREE_QUARTERS = 270f

/** The bitmap as displayed: BitmapFactory ignores EXIF orientation, Coil and the pickers do not. */
private fun Bitmap.upright(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(QUARTER)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(HALF)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(THREE_QUARTERS)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply { postRotate(QUARTER) }.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply { postRotate(THREE_QUARTERS) }.postScale(-1f, 1f)
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/** The [rect] of this bitmap, turned by [rotation] degrees counter-clockwise and flipped if [mirrored]. */
private fun Bitmap.cropped(
    rect: NormalizedCropRect,
    rotation: Float,
    mirrored: Boolean,
): Bitmap {
    val x = (rect.left * width).toInt().coerceIn(0, width - 1)
    val y = (rect.top * height).toInt().coerceIn(0, height - 1)
    val cropWidth = (rect.width * width).toInt().coerceIn(1, width - x)
    val cropHeight = (rect.height * height).toInt().coerceIn(1, height - y)
    val matrix = Matrix().apply {
        if (mirrored) {
            preScale(-1f, 1f)
        }
        // Android rotates clockwise; the rotation is counter-clockwise.
        postRotate(-rotation)
    }
    return Bitmap.createBitmap(this, x, y, cropWidth, cropHeight, matrix, true)
}

/** This bitmap, scaled down so its longest side is at most [maxSize]; unchanged without one. */
private fun Bitmap.fitted(maxSize: Int?): Bitmap {
    val (outWidth, outHeight) = scaledToFit(width = width, height = height, maxSize = maxSize)
    return if (outWidth == width) {
        this
    } else {
        Bitmap.createScaledBitmap(this, outWidth, outHeight, true)
    }
}

private fun Bitmap.jpeg(quality: Int): ByteArray {
    return ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }
}
