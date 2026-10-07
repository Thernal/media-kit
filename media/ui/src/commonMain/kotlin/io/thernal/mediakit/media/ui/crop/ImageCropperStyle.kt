package io.thernal.mediakit.media.ui.crop

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The cropper's drawing: the dimmed outside, the frame, its rule-of-thirds grid and corner handles. */
@Immutable
data class ImageCropperStyle(
    val scrim: Color = Color.Black.copy(alpha = SCRIM_ALPHA),
    val frame: Color = Color.White,
    val grid: Color = Color.White.copy(alpha = GRID_ALPHA),
    val frameWidth: Dp = 2.dp,
    val handleLength: Dp = 20.dp,
    val handleWidth: Dp = 4.dp,
    /** How near a corner a touch must start to resize rather than move. */
    val touchSlop: Dp = 40.dp,
)

private const val SCRIM_ALPHA = 0.6f
private const val GRID_ALPHA = 0.3f
