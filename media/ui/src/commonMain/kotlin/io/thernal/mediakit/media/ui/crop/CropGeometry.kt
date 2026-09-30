package io.thernal.mediakit.media.ui.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import io.thernal.mediakit.media.api.domain.NormalizedCropRect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val QUARTER_TURN = 90
internal const val HALF_TURN = 180
internal const val FULL_TURN = 360

/** The smallest a frame side may become, in pixels on screen. */
internal const val MIN_FRAME_SIZE = 80f

/** A corner of the crop frame. */
enum class CropHandle { TopLeft, TopRight, BottomLeft, BottomRight }

/** Where an image of [imageSize] sits when fitted (ContentScale.Fit) into [containerSize]. */
internal fun fitRect(
    containerSize: Size,
    imageSize: Size,
): Rect {
    if (containerSize.width == 0f || containerSize.height == 0f || imageSize.width == 0f || imageSize.height == 0f) {
        return Rect.Zero
    }
    val containerRatio = containerSize.width / containerSize.height
    val imageRatio = imageSize.width / imageSize.height
    return if (imageRatio > containerRatio) {
        val height = containerSize.width / imageRatio
        Rect(
            left = 0f,
            top = (containerSize.height - height) / 2f,
            right = containerSize.width,
            bottom = (containerSize.height + height) / 2f,
        )
    } else {
        val width = containerSize.height * imageRatio
        Rect(
            left = (containerSize.width - width) / 2f,
            top = 0f,
            right = (containerSize.width + width) / 2f,
            bottom = containerSize.height,
        )
    }
}

/** The largest frame of [ratio] inside [imageRect], centred on it. */
internal fun centeredFrame(
    ratio: Float,
    imageRect: Rect,
): Rect {
    val width = minOf(a = imageRect.width, b = imageRect.height * ratio)
    val height = width / ratio
    val center = imageRect.center
    return Rect(
        left = center.x - width / 2f,
        top = center.y - height / 2f,
        right = center.x + width / 2f,
        bottom = center.y + height / 2f,
    )
}

/** The corner of [frame] within [threshold] of [position], the nearest if several. */
internal fun detectHandle(
    position: Offset,
    frame: Rect,
    threshold: Float,
): CropHandle? {
    fun distanceTo(corner: Offset): Float {
        val dx = position.x - corner.x
        val dy = position.y - corner.y
        return sqrt(dx * dx + dy * dy)
    }
    return listOf(
        CropHandle.TopLeft to distanceTo(frame.topLeft),
        CropHandle.TopRight to distanceTo(frame.topRight),
        CropHandle.BottomLeft to distanceTo(frame.bottomLeft),
        CropHandle.BottomRight to distanceTo(frame.bottomRight),
    ).minByOrNull { it.second }?.takeIf { it.second < threshold }?.first
}

/**
 * [rect] with its ratio brought within [minRatio]..[maxRatio] by changing its height, anchored on the
 * edge the [handle] does not move (the top when there is no handle), and kept inside [bounds].
 */
internal fun clampRatio(
    rect: Rect,
    bounds: Rect,
    handle: CropHandle?,
    minRatio: Float?,
    maxRatio: Float?,
): Rect {
    val ratio = rect.width / rect.height
    val clamped = ratio.coerceIn(minimumValue = minRatio ?: ratio, maximumValue = maxRatio ?: ratio)
    if (clamped == ratio) {
        return rect
    }
    val height = (rect.width / clamped).coerceAtLeast(MIN_FRAME_SIZE)
    return when (handle) {
        CropHandle.TopLeft, CropHandle.TopRight -> rect.copy(top = (rect.bottom - height).coerceAtLeast(bounds.top))

        CropHandle.BottomLeft, CropHandle.BottomRight, null ->
            rect.copy(bottom = (rect.top + height).coerceAtMost(bounds.bottom))
    }
}

/**
 * The frame on screen as a rectangle of the source image's pixels. The image is shown fitted into
 * [displayImageRect], zoomed by [imageScale] and moved by [imageOffset] around its centre, rotated
 * [rotation] degrees counter-clockwise and, if [mirrored], flipped horizontally first.
 */
internal fun toNormalizedCropRect(
    frameRect: Rect,
    displayImageRect: Rect,
    imageScale: Float,
    imageOffset: Offset,
    rotation: Float,
    mirrored: Boolean,
): NormalizedCropRect {
    if (displayImageRect == Rect.Zero) {
        return NormalizedCropRect.FULL
    }
    val center = displayImageRect.center + imageOffset
    val angle = rotation * (PI.toFloat() / HALF_TURN)
    val cosA = cos(angle)
    val sinA = sin(angle)
    // Corners relative to the image centre, turned back by the rotation (stored counter-clockwise).
    val corners = listOf(frameRect.topLeft, frameRect.topRight, frameRect.bottomLeft, frameRect.bottomRight)
        .map { it - center }
        .map { Offset(x = it.x * cosA - it.y * sinA, y = it.x * sinA + it.y * cosA) }
    val sides = Size(width = displayImageRect.width * imageScale, height = displayImageRect.height * imageScale)
    val imageWidth = if (isQuarterTurned(rotation)) {
        sides.height
    } else {
        sides.width
    }
    val imageHeight = if (isQuarterTurned(rotation)) {
        sides.width
    } else {
        sides.height
    }
    val normalized = corners.map { corner ->
        Offset(
            x = (corner.x + imageWidth / 2f) / imageWidth,
            y = (corner.y + imageHeight / 2f) / imageHeight,
        )
    }
    val minX = normalized.minOf { it.x }
    val maxX = normalized.maxOf { it.x }
    // The points are in the mirrored image's space; the source's is reflected horizontally.
    val (left, right) = if (mirrored) {
        1f - maxX to 1f - minX
    } else {
        minX to maxX
    }
    return NormalizedCropRect(
        left = left.coerceIn(0f, 1f),
        top = normalized.minOf { it.y }.coerceIn(0f, 1f),
        right = right.coerceIn(0f, 1f),
        bottom = normalized.maxOf { it.y }.coerceIn(0f, 1f),
    )
}

internal fun isQuarterTurned(rotation: Float): Boolean {
    return rotation.toInt() / QUARTER_TURN % 2 != 0
}
