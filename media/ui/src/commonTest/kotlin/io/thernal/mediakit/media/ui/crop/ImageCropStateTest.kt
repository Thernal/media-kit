package io.thernal.mediakit.media.ui.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import io.thernal.mediakit.media.api.domain.AspectRatio
import io.thernal.mediakit.media.api.domain.CropOptions
import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val SQUARE_400 = Size(400f, 400f)
private const val TOLERANCE = 0.01f

private fun state(
    options: CropOptions = CropOptions(),
    container: Size = SQUARE_400,
    image: Size = SQUARE_400,
): ImageCropState {
    val state = ImageCropState(
        file = MediaFile(name = "a.jpg", bytes = ByteArray(1), mimeType = MimeType.Image.JPEG),
        options = options,
    )
    state.onLayout(containerSize = container, intrinsicSize = image)
    return state
}

private fun assertRect(
    expected: Rect,
    actual: Rect,
) {
    listOf(
        expected.left to actual.left,
        expected.top to actual.top,
        expected.right to actual.right,
        expected.bottom to actual.bottom,
    ).forEach { (e, a) -> assertEquals(e, a, TOLERANCE, "expected $expected, was $actual") }
}

class ImageCropStateTest {
    @Test
    fun `without an initial ratio the frame is the whole image and free`() {
        val state = state(image = Size(800f, 400f))

        assertRect(Rect(0f, 100f, 400f, 300f), state.frameRect)
        assertFalse(state.isLocked)
        assertEquals(2f, state.aspectRatio, TOLERANCE)
    }

    @Test
    fun `an initial ratio is centred in the image and locked`() {
        val state = state(options = CropOptions(initialRatio = AspectRatio.SQUARE), image = Size(800f, 400f))

        assertRect(Rect(100f, 100f, 300f, 300f), state.frameRect)
        assertTrue(state.isLocked)
    }

    @Test
    fun `a locked ratio without an initial one is square and offers no ratios`() {
        val state = state(options = CropOptions(isRatioLocked = true), image = Size(800f, 400f))

        assertTrue(state.isLocked)
        assertEquals(1f, state.aspectRatio, TOLERANCE)
        assertEquals(emptyList(), state.ratios)
    }

    @Test
    fun `the same layout again changes nothing`() {
        val state = state()
        state.moveFrame(Offset(10f, 10f))
        val frame = state.frameRect

        state.onLayout(containerSize = SQUARE_400, intrinsicSize = SQUARE_400)

        assertEquals(frame, state.frameRect)
    }

    @Test
    fun `applying a ratio frames it in the middle and locks it`() {
        val state = state()

        state.applyRatio(AspectRatio.R16_9)

        assertRect(Rect(0f, 87.5f, 400f, 312.5f), state.frameRect)
        assertTrue(state.isLocked)
        assertEquals(AspectRatio.R16_9.value, state.lockedRatio, TOLERANCE)
    }

    @Test
    fun `ratio changes are ignored when the options lock it`() {
        val state = state(options = CropOptions(isRatioLocked = true))
        val frame = state.frameRect

        state.applyRatio(AspectRatio.R16_9)
        state.unlockRatio()

        assertEquals(frame, state.frameRect)
        assertTrue(state.isLocked)
    }

    @Test
    fun `unlocking lets the frame take any ratio again`() {
        val state = state()
        state.applyRatio(AspectRatio.SQUARE)

        state.unlockRatio()

        assertFalse(state.isLocked)
    }

    @Test
    fun `moving keeps the frame inside the image`() {
        val state = state(options = CropOptions(initialRatio = AspectRatio.SQUARE), image = Size(800f, 400f))

        state.moveFrame(Offset(50f, 0f))
        assertRect(Rect(150f, 100f, 350f, 300f), state.frameRect)

        state.moveFrame(Offset(500f, 500f))
        assertRect(Rect(200f, 100f, 400f, 300f), state.frameRect)
    }

    @Test
    fun `a resize stops at the minimum size`() {
        val state = state()

        state.resizeFrame(CropHandle.BottomRight, Offset(-350f, -350f))

        assertRect(Rect(0f, 0f, MIN_FRAME_SIZE, MIN_FRAME_SIZE), state.frameRect)
    }

    @Test
    fun `a free resize moves only the dragged corner`() {
        val state = state()

        state.resizeFrame(CropHandle.TopLeft, Offset(40f, 100f))

        assertRect(Rect(40f, 100f, 400f, 400f), state.frameRect)
        assertEquals(360f / 300f, state.aspectRatio, TOLERANCE)
    }

    @Test
    fun `a locked resize keeps the ratio`() {
        val state = state(options = CropOptions(initialRatio = AspectRatio.SQUARE))

        state.resizeFrame(CropHandle.BottomRight, Offset(-100f, 0f))

        assertRect(Rect(0f, 0f, 300f, 300f), state.frameRect)
        assertEquals(1f, state.aspectRatio, TOLERANCE)
    }

    @Test
    fun `a free frame stays within the options' ratio bounds`() {
        val state = state(options = CropOptions(minRatio = 0.8f, maxRatio = 1.25f))

        state.resizeFrame(CropHandle.BottomRight, Offset(0f, -300f))

        assertEquals(1.25f, state.aspectRatio, TOLERANCE)
    }

    @Test
    fun `zoom stays between one and five times and the offset within the zoomed image`() {
        val state = state()

        state.zoom(zoomDelta = 10f, panDelta = Offset(5000f, 0f))
        assertEquals(5f, state.imageScale)
        assertEquals(Offset(800f, 0f), state.imageOffset)

        state.zoom(zoomDelta = 0.01f, panDelta = Offset.Zero)
        assertEquals(1f, state.imageScale)
    }

    @Test
    fun `panning needs a zoomed image`() {
        val state = state()
        state.pan(Offset(30f, 30f))
        assertEquals(Offset.Zero, state.imageOffset)

        state.zoom(zoomDelta = 2f, panDelta = Offset.Zero)
        state.pan(Offset(30f, -30f))

        assertEquals(Offset(30f, -30f), state.imageOffset)
    }

    @Test
    fun `a quarter turn refits the image into the swapped rect`() {
        val state = state(container = Size(300f, 400f), image = Size(300f, 200f))
        assertRect(Rect(0f, 100f, 300f, 300f), state.displayImageRect)

        state.rotateLeft()

        assertEquals(90f, state.rotation)
        assertRect(Rect(16.67f, 0f, 283.33f, 400f), state.displayImageRect)
        assertEquals(266.67f / 200f, state.baseScale, TOLERANCE)
    }

    @Test
    fun `a turn resets zoom and keeps a locked ratio`() {
        val state = state(
            options = CropOptions(initialRatio = AspectRatio.R4_5),
            container = Size(300f, 400f),
            image = Size(300f, 200f),
        )
        state.zoom(zoomDelta = 2f, panDelta = Offset(10f, 10f))

        state.rotateRight()

        assertEquals(-90f, state.rotation)
        assertEquals(1f, state.imageScale)
        assertEquals(Offset.Zero, state.imageOffset)
        assertEquals(AspectRatio.R4_5.value, state.aspectRatio, TOLERANCE)
    }

    @Test
    fun `turning right then left restores the layout`() {
        val state = state(container = Size(300f, 400f), image = Size(300f, 200f))
        val display = state.displayImageRect
        val frame = state.frameRect

        state.rotateRight()
        state.rotateLeft()

        assertEquals(0f, state.rotation)
        assertRect(display, state.displayImageRect)
        assertRect(frame, state.frameRect)
    }

    @Test
    fun `mirroring negates the rotation and flips the offset but keeps the geometry`() {
        val state = state()
        state.rotateLeft()
        state.zoom(zoomDelta = 2f, panDelta = Offset(40f, 20f))
        val display = state.displayImageRect
        val frame = state.frameRect

        state.mirror()

        assertTrue(state.isMirrored)
        assertEquals(-90f, state.rotation)
        assertEquals(Offset(-40f, 20f), state.imageOffset)
        assertEquals(2f, state.imageScale)
        assertEquals(display, state.displayImageRect)
        assertEquals(frame, state.frameRect)
    }

    @Test
    fun `mirroring twice restores the state`() {
        val state = state()
        state.rotateLeft()

        state.mirror()
        state.mirror()

        assertFalse(state.isMirrored)
        assertEquals(90f, state.rotation)
    }
}
