package io.thernal.mediakit.media.ui.zoom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val VIEW = Size(400f, 800f)

class ZoomableStateTest {
    @Test
    fun `double tap steps through the zoom levels`() {
        assertEquals(MID_ZOOM, nextZoomStep(MIN_ZOOM))
        assertEquals(MAX_ZOOM, nextZoomStep(MID_ZOOM))
        assertEquals(MIN_ZOOM, nextZoomStep(MAX_ZOOM))
    }

    @Test
    fun `double tap at the centre zooms without moving`() {
        val offset = doubleTapOffset(
            startScale = 1f,
            startOffset = Offset.Zero,
            targetScale = 2f,
            tap = Offset(200f, 400f),
            viewSize = VIEW,
        )

        assertEquals(Offset.Zero, offset)
    }

    @Test
    fun `double tap near an edge moves toward it within bounds`() {
        val offset = doubleTapOffset(
            startScale = 1f,
            startOffset = Offset.Zero,
            targetScale = 2f,
            tap = Offset(0f, 400f),
            viewSize = VIEW,
        )

        assertEquals(Offset(200f, 0f), offset)
    }

    @Test
    fun `pinching below one resets the offset`() {
        val state = ZoomableState()
        state.pinch(zoomChange = 2f, pan = Offset(50f, 50f), viewSize = VIEW)
        assertEquals(Offset(50f, 50f), state.offset)

        state.pinch(zoomChange = 0.1f, pan = Offset(50f, 50f), viewSize = VIEW)

        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `a one-finger pan moves a zoomed image faster than the finger and reports it`() {
        val state = ZoomableState()
        state.pinch(zoomChange = 2f, pan = Offset.Zero, viewSize = VIEW)

        assertTrue(state.panWithOneFinger(pan = Offset(10f, 0f), viewSize = VIEW))
        assertTrue(state.offset.x > 10f)
        assertFalse(state.panWithOneFinger(pan = Offset.Zero, viewSize = VIEW))
    }

    @Test
    fun `reset returns to one times and centred`() {
        val state = ZoomableState()
        state.pinch(zoomChange = 3f, pan = Offset(30f, 30f), viewSize = VIEW)

        state.reset()

        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }
}
