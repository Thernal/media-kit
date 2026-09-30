package io.thernal.mediakit.media.ui.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.thernal.mediakit.media.api.domain.NormalizedCropRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val DISPLAY = Rect(0f, 0f, 400f, 400f)
private const val TOLERANCE = 0.001f

private fun assertCrop(
    expected: NormalizedCropRect,
    actual: NormalizedCropRect,
) {
    listOf(
        expected.left to actual.left,
        expected.top to actual.top,
        expected.right to actual.right,
        expected.bottom to actual.bottom,
    ).forEach { (e, a) -> assertEquals(e, a, TOLERANCE, "expected $expected, was $actual") }
}

private fun crop(
    frame: Rect,
    scale: Float = 1f,
    offset: Offset = Offset.Zero,
    rotation: Float = 0f,
    mirrored: Boolean = false,
): NormalizedCropRect {
    return toNormalizedCropRect(
        frameRect = frame,
        displayImageRect = DISPLAY,
        imageScale = scale,
        imageOffset = offset,
        rotation = rotation,
        mirrored = mirrored,
    )
}

class CropGeometryTest {
    @Test
    fun `the whole image is the whole source`() {
        assertCrop(NormalizedCropRect.FULL, crop(DISPLAY))
    }

    @Test
    fun `the left half is the left half`() {
        assertCrop(NormalizedCropRect(0f, 0f, 0.5f, 1f), crop(Rect(0f, 0f, 200f, 400f)))
    }

    @Test
    fun `mirrored the left half on screen is the source's right half`() {
        assertCrop(NormalizedCropRect(0.5f, 0f, 1f, 1f), crop(Rect(0f, 0f, 200f, 400f), mirrored = true))
    }

    @Test
    fun `at twice the zoom the whole view is the middle quarter`() {
        assertCrop(NormalizedCropRect(0.25f, 0.25f, 0.75f, 0.75f), crop(DISPLAY, scale = 2f))
    }

    @Test
    fun `a zoomed image moved right shows more of its left side`() {
        assertCrop(NormalizedCropRect(0f, 0.25f, 0.5f, 0.75f), crop(DISPLAY, scale = 2f, offset = Offset(200f, 0f)))
    }

    @Test
    fun `turned a quarter counter-clockwise the top half on screen is the source's right half`() {
        assertCrop(NormalizedCropRect(0.5f, 0f, 1f, 1f), crop(Rect(0f, 0f, 400f, 200f), rotation = 90f))
    }

    @Test
    fun `nothing laid out yet crops nothing away`() {
        val rect = toNormalizedCropRect(
            frameRect = Rect(0f, 0f, 10f, 10f),
            displayImageRect = Rect.Zero,
            imageScale = 1f,
            imageOffset = Offset.Zero,
            rotation = 0f,
            mirrored = false,
        )

        assertEquals(NormalizedCropRect.FULL, rect)
    }

    @Test
    fun `the nearest corner within the threshold is found`() {
        val frame = Rect(0f, 0f, 100f, 100f)

        assertEquals(CropHandle.BottomRight, detectHandle(position = Offset(95f, 90f), frame = frame, threshold = 20f))
        assertNull(detectHandle(position = Offset(50f, 50f), frame = frame, threshold = 20f))
    }

    @Test
    fun `the output keeps its proportions under the size limit`() {
        assertEquals(1000 to 500, scaledToFit(width = 4000, height = 2000, maxSize = 1000))
        assertEquals(400 to 300, scaledToFit(width = 400, height = 300, maxSize = 1000))
        assertEquals(4000 to 2000, scaledToFit(width = 4000, height = 2000, maxSize = null))
    }

    @Test
    fun `the cropped file is named after its source`() {
        assertEquals("photo_cropped.jpg", croppedName("photo.png"))
        assertEquals("image_cropped.jpg", croppedName(".hidden"))
    }
}
