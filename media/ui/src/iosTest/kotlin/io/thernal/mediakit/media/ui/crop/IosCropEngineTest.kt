package io.thernal.mediakit.media.ui.crop

import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.NormalizedCropRect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.test.runTest
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIRectFill
import platform.posix.memcpy
import kotlin.test.Test
import kotlin.test.assertEquals

/** A real PNG, 40×20: red left half, blue right half. */
@OptIn(ExperimentalForeignApi::class)
private fun testPng(): MediaFile {
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
    val image = UIGraphicsImageRenderer(size = CGSizeMake(40.0, 20.0), format = format).imageWithActions { _ ->
        UIColor.redColor.setFill()
        UIRectFill(CGRectMake(0.0, 0.0, 20.0, 20.0))
        UIColor.blueColor.setFill()
        UIRectFill(CGRectMake(20.0, 0.0, 20.0, 20.0))
    }
    val data = UIImagePNGRepresentation(image)!!
    val bytes = ByteArray(data.length.toInt())
    bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return MediaFile(name = "test.png", bytes = bytes, mimeType = MimeType.Image.PNG)
}

class IosCropEngineTest {
    @Test
    fun `the left half comes out square and as JPEG`() {
        runTest {
            val result = cropImage(testPng(), NormalizedCropRect(0f, 0f, 0.5f, 1f), 0f, false, null, 90)

            assertEquals(20, result.width)
            assertEquals(20, result.height)
            assertEquals(MimeType.Image.JPEG, result.mimeType)
            assertEquals("test_cropped.jpg", result.name)
        }
    }

    @Test
    fun `a quarter turn swaps the sides`() {
        runTest {
            val result = cropImage(testPng(), NormalizedCropRect.FULL, 90f, true, null, 90)

            assertEquals(20, result.width)
            assertEquals(40, result.height)
        }
    }

    @Test
    fun `the result is scaled down to the size limit`() {
        runTest {
            val result = cropImage(testPng(), NormalizedCropRect.FULL, 0f, false, 10, 90)

            assertEquals(10, result.width)
            assertEquals(5, result.height)
        }
    }
}
