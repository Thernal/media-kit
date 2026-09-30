package io.thernal.mediakit.media.impl.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import io.thernal.mediakit.media.impl.domain.ImageCodec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

private const val JPEG_QUALITY = 90

internal class AndroidImageCodec(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ImageCodec {
    override suspend fun transcodeToJpeg(bytes: ByteArray): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }
        return withContext(dispatcher) {
            // A single-file decode with no shared state: any failure — a codec the device lacks, an
            // OutOfMemoryError on a huge source — becomes "cannot decode", never a crash.
            @Suppress("TooGenericExceptionCaught")
            try {
                // ImageDecoder applies the container's rotation itself; a software bitmap can be compressed.
                val bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(ByteBuffer.wrap(bytes)),
                ) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                bitmap.toJpeg()
            } catch (_: Throwable) {
                null
            }
        }
    }

    override suspend fun normalizeOrientation(bytes: ByteArray): ByteArray {
        return withContext(dispatcher) {
            val orientation = ByteArrayInputStream(bytes).use { stream ->
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
            val matrix = orientationMatrix(orientation) ?: return@withContext bytes
            // As above: an image that cannot be redrawn keeps its original orientation.
            @Suppress("TooGenericExceptionCaught")
            try {
                val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true).toJpeg()
            } catch (_: Throwable) {
                bytes
            }
        }
    }

    private fun Bitmap.toJpeg(): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return output.toByteArray()
    }
}

private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f

/** The transform that makes an image with this EXIF orientation upright; `null` when it already is. */
internal fun orientationMatrix(orientation: Int): Matrix? {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(QUARTER_TURN)

        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(HALF_TURN)

        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(THREE_QUARTER_TURN)

        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)

        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)

        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(QUARTER_TURN)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(THREE_QUARTER_TURN)
            matrix.postScale(-1f, 1f)
        }

        else -> return null
    }
    return matrix
}
