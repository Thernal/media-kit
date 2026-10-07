package io.thernal.mediakit.sample.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The sample's palette: a dark canvas for photos, light text on it, one accent. */
@Immutable
data class SampleColors(
    /** What a photo is shown on: the crop screen's background. */
    val canvas: Color = Color(color = 0xFF0E0E12),
    val onCanvas: Color = Color(color = 0xFFF1F0F7),
    val line: Color = Color(color = 0xFF5A5A6E),
    val accent: Color = Color(color = 0xFFB38CFF),
    val onAccent: Color = Color(color = 0xFF14141A),
    /** What dims the image outside the crop frame. */
    val scrim: Color = Color(color = 0xB3000000),
)
