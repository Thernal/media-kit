package io.thernal.mediakit.media.api.domain

import androidx.compose.runtime.Immutable

/** A crop in the source image's own pixels, each edge as a fraction of its width or height (0–1). */
@Immutable
data class NormalizedCropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    companion object {
        val FULL = NormalizedCropRect(left = 0f, top = 0f, right = 1f, bottom = 1f)
    }
}
