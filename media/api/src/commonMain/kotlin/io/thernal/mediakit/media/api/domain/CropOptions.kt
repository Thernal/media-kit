package io.thernal.mediakit.media.api.domain

import androidx.compose.runtime.Immutable

/**
 * How an image may be cropped and what comes out.
 *
 * The defaults are neutral: a free frame, the usual ratios offered, no bounds, full resolution. An
 * app's own rules are one value — a post format, say:
 *
 * ```
 * val PostCrop = CropOptions(
 *     ratios = listOf(AspectRatio.R9_16, AspectRatio.R4_5, AspectRatio.SQUARE, AspectRatio.R16_9),
 *     initialRatio = AspectRatio.SQUARE,
 *     minRatio = AspectRatio.R9_16.value,
 *     maxRatio = AspectRatio.R16_9.value,
 * )
 * ```
 */
@Immutable
data class CropOptions(
    /** The ratios offered; empty offers none (only a free frame, or the locked one). */
    val ratios: List<AspectRatio> = listOf(
        AspectRatio.SQUARE,
        AspectRatio.R4_5,
        AspectRatio.R3_4,
        AspectRatio.R4_3,
        AspectRatio.R16_9,
    ),
    /** The frame's ratio when the cropper opens; `null` starts from the whole image. */
    val initialRatio: AspectRatio? = null,
    /** Keep [initialRatio] (or square, without one) and offer no other. */
    val isRatioLocked: Boolean = false,
    /** The narrowest a free frame may become (width / height); `null` for no bound. */
    val minRatio: Float? = null,
    /** The widest a free frame may become (width / height); `null` for no bound. */
    val maxRatio: Float? = null,
    /** The longest side of the result in pixels; larger crops are scaled down. `null` keeps full size. */
    val maxOutputSize: Int? = null,
    /** JPEG quality of the result, 0–100. */
    val jpegQuality: Int = DEFAULT_JPEG_QUALITY,
) {
    init {
        require(jpegQuality in 0..MAX_JPEG_QUALITY) { "jpegQuality must be 0–100, was $jpegQuality" }
        require(maxOutputSize == null || maxOutputSize > 0) { "maxOutputSize must be positive" }
    }

    private companion object {
        const val DEFAULT_JPEG_QUALITY = 90
        const val MAX_JPEG_QUALITY = 100
    }
}
