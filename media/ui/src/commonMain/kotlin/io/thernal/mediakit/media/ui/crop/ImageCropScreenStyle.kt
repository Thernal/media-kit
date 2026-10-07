package io.thernal.mediakit.media.ui.crop

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * How `ImageCropScreen` and its default bars look: the background behind the image, the bars' text and
 * spacing, the ratio chips. Neutral defaults; map the app's tokens in `MediaStyles`.
 */
@Immutable
data class ImageCropScreenStyle(
    val backgroundColor: Color = Color.Black,
    val contentColor: Color = Color.White,
    val textStyle: TextStyle = TextStyle(fontSize = 16.sp),
    /** How visible a button is while it cannot be pressed (Done, while the crop runs). */
    val disabledAlpha: Float = DEFAULT_DISABLED_ALPHA,
    /** Around the top bar's row of buttons. */
    val topBarPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    /** Around the bottom bar, above and below its two rows. */
    val bottomBarPadding: PaddingValues = PaddingValues(vertical = 12.dp),
    /** Between the bottom bar's ratio chips and its buttons. */
    val bottomBarSpacing: Dp = 16.dp,
    /** On either side of the ratio chips' scrolling row. */
    val ratioRowPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    /** Around a text button's label, inside its touch target. */
    val buttonPadding: PaddingValues = PaddingValues(8.dp),
    val ratioChip: CropRatioChipStyle = CropRatioChipStyle(),
)

private const val DEFAULT_DISABLED_ALPHA = 0.6f
