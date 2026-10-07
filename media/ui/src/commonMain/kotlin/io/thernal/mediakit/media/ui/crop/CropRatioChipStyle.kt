package io.thernal.mediakit.media.ui.crop

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The ratio chips of `ImageCropDefaults.BottomBar`: one per offered ratio, the selected one filled. */
@Immutable
data class CropRatioChipStyle(
    val shape: Shape = RoundedCornerShape(16.dp),
    val borderWidth: Dp = 1.dp,
    val borderColor: Color = Color.White,
    val contentColor: Color = Color.White,
    val selectedContainerColor: Color = Color.White,
    val selectedContentColor: Color = Color.Black,
    val contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    /** Between two chips. */
    val spacing: Dp = 8.dp,
)
