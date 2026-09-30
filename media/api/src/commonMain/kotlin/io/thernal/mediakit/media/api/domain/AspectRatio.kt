package io.thernal.mediakit.media.api.domain

import androidx.compose.runtime.Immutable

/** A crop frame's width to height, kept as the two numbers so it can be labelled `4:5`. */
@Immutable
data class AspectRatio(
    val width: Float,
    val height: Float,
) {
    val value: Float
        get() = width / height

    val label: String
        get() = "${width.toInt()}:${height.toInt()}"

    companion object {
        val R9_16 = AspectRatio(width = 9f, height = 16f)
        val R3_4 = AspectRatio(width = 3f, height = 4f)
        val R4_5 = AspectRatio(width = 4f, height = 5f)
        val SQUARE = AspectRatio(width = 1f, height = 1f)
        val R4_3 = AspectRatio(width = 4f, height = 3f)
        val R16_9 = AspectRatio(width = 16f, height = 9f)
    }
}
