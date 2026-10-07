package io.thernal.mediakit.sample.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii. */
@Immutable
data class SampleShapes(
    val small: Dp = 6.dp,
    val pill: Dp = 50.dp,
)
