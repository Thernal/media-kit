package io.thernal.mediakit.media.ui.crop

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

/** The dimmed outside, the frame, its grid and handles, and the gesture that moves them. */
@Composable
internal fun CropOverlay(
    state: ImageCropState,
    style: ImageCropperStyle,
    modifier: Modifier,
) {
    val slop = with(LocalDensity.current) { style.touchSlop.toPx() }
    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                val frame = state.frameRect
                if (frame != Rect.Zero) {
                    drawScrim(frame = frame, color = style.scrim)
                    drawGrid(frame = frame, color = style.grid, width = style.frameWidth.toPx() / 2f)
                    drawRect(
                        color = style.frame,
                        topLeft = frame.topLeft,
                        size = frame.size,
                        style = Stroke(style.frameWidth.toPx()),
                    )
                    drawHandles(
                        frame = frame,
                        color = style.frame,
                        length = style.handleLength.toPx(),
                        width = style.handleWidth.toPx(),
                    )
                }
            }
            .pointerInput(state) { awaitEachGesture { trackCropGesture(state = state, slop = slop) } },
    )
}

private fun DrawScope.drawScrim(
    frame: Rect,
    color: Color,
) {
    drawRect(color = color, topLeft = Offset.Zero, size = Size(width = size.width, height = frame.top))
    drawRect(
        color = color,
        topLeft = Offset(x = 0f, y = frame.bottom),
        size = Size(width = size.width, height = size.height - frame.bottom),
    )
    drawRect(
        color = color,
        topLeft = Offset(x = 0f, y = frame.top),
        size = Size(width = frame.left, height = frame.height),
    )
    drawRect(
        color = color,
        topLeft = Offset(x = frame.right, y = frame.top),
        size = Size(width = size.width - frame.right, height = frame.height),
    )
}

private fun DrawScope.drawGrid(
    frame: Rect,
    color: Color,
    width: Float,
) {
    for (line in 1 until GRID_DIVISIONS) {
        val x = frame.left + frame.width * line / GRID_DIVISIONS
        val y = frame.top + frame.height * line / GRID_DIVISIONS
        drawLine(
            color = color,
            start = Offset(x = x, y = frame.top),
            end = Offset(x = x, y = frame.bottom),
            strokeWidth = width,
        )
        drawLine(
            color = color,
            start = Offset(x = frame.left, y = y),
            end = Offset(x = frame.right, y = y),
            strokeWidth = width,
        )
    }
}

private fun DrawScope.drawHandles(
    frame: Rect,
    color: Color,
    length: Float,
    width: Float,
) {
    listOf(
        frame.topLeft to Offset(x = 1f, y = 1f),
        frame.topRight to Offset(x = -1f, y = 1f),
        frame.bottomLeft to Offset(x = 1f, y = -1f),
        frame.bottomRight to Offset(x = -1f, y = -1f),
    ).forEach { (corner, direction) ->
        drawLine(
            color = color,
            start = corner,
            end = corner + Offset(x = length * direction.x, y = 0f),
            strokeWidth = width,
        )
        drawLine(
            color = color,
            start = corner,
            end = corner + Offset(x = 0f, y = length * direction.y),
            strokeWidth = width,
        )
    }
}

private const val GRID_DIVISIONS = 3
