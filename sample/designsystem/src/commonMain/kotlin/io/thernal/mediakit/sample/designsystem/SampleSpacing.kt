package io.thernal.mediakit.sample.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The spacing scale, and the widths of drawn lines. */
@Immutable
data class SampleSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val hairline: Dp = 1.dp,
    val stroke: Dp = 2.dp,
)
