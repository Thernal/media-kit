package io.thernal.mediakit.media.ui.crop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import io.thernal.mediakit.media.api.domain.AspectRatio
import io.thernal.mediakit.media.api.domain.CropOptions
import io.thernal.mediakit.media.api.domain.MediaFile
import kotlinx.coroutines.CancellationException

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

/** A cropper for [file] under [options]; survives recomposition, not configuration changes. */
@Composable
fun rememberImageCropState(
    file: MediaFile,
    options: CropOptions = CropOptions(),
): ImageCropState {
    return remember(key1 = file, key2 = options) { ImageCropState(file = file, options = options) }
}

/**
 * Everything the crop UI shows and changes: the frame, its ratio, the image's zoom, offset, rotation
 * and mirror. The UI calls the operations; an app's own controls can call them too — rotate, mirror,
 * apply a ratio — and [crop] produces the result.
 *
 * Geometry is in pixels of the cropper's own layout; [onLayout] is called by `ImageCropper` once the
 * container and the image's size are known, and again when they change.
 */
@Stable
class ImageCropState(
    val file: MediaFile,
    val options: CropOptions = CropOptions(),
) {
    /** The frame on screen. */
    var frameRect by mutableStateOf(Rect.Zero)
        private set

    /** Where the image sits on screen, fitted and rotated, before zoom. */
    var displayImageRect by mutableStateOf(Rect.Zero)
        private set

    /** The frame keeps [lockedRatio] while resized. */
    var isLocked by mutableStateOf(false)
        private set

    var lockedRatio by mutableStateOf(1f)
        private set

    /** The frame's current width / height. */
    var aspectRatio by mutableStateOf(1f)
        private set

    var imageScale by mutableStateOf(MIN_SCALE)
        private set

    /** Re-fits the image after a quarter turn swaps its sides. */
    var baseScale by mutableStateOf(1f)
        private set

    var imageOffset by mutableStateOf(Offset.Zero)
        private set

    /** Counter-clockwise degrees, a multiple of 90. */
    var rotation by mutableStateOf(0f)
        private set

    var isMirrored by mutableStateOf(false)
        private set

    /** True while [crop] runs. */
    var isProcessing by mutableStateOf(false)
        private set

    /** The ratios to offer: none when the options lock one. */
    val ratios: List<AspectRatio>
        get() = if (options.isRatioLocked) {
            emptyList()
        } else {
            options.ratios
        }

    private var containerSize = Size.Zero
    private var intrinsicSize = Size.Zero

    fun onLayout(
        containerSize: Size,
        intrinsicSize: Size,
    ) {
        val isUnchanged = containerSize == this.containerSize &&
            intrinsicSize == this.intrinsicSize &&
            displayImageRect != Rect.Zero
        if (isUnchanged) {
            return
        }
        this.containerSize = containerSize
        this.intrinsicSize = intrinsicSize
        relayout()
    }

    /** Frames [ratio] in the middle of the image and keeps it while resizing. Ignored when the options lock the ratio. */
    fun applyRatio(ratio: AspectRatio) {
        if (options.isRatioLocked || displayImageRect == Rect.Zero) {
            return
        }
        val frame = clamped(rect = centeredFrame(ratio = ratio.value, imageRect = displayImageRect), handle = null)
        frameRect = frame
        aspectRatio = frame.width / frame.height
        lockedRatio = aspectRatio
        isLocked = true
    }

    /** Lets the frame take any ratio again. Ignored when the options lock the ratio. */
    fun unlockRatio() {
        if (!options.isRatioLocked) {
            isLocked = false
        }
    }

    fun moveFrame(delta: Offset) {
        val frame = frameRect
        val bounds = displayImageRect
        val left = (frame.left + delta.x).coerceIn(
            minimumValue = bounds.left,
            maximumValue = maxOf(a = bounds.left, b = bounds.right - frame.width),
        )
        val top = (frame.top + delta.y).coerceIn(
            minimumValue = bounds.top,
            maximumValue = maxOf(a = bounds.top, b = bounds.bottom - frame.height),
        )
        frameRect = Rect(offset = Offset(x = left, y = top), size = frame.size)
    }

    fun resizeFrame(
        handle: CropHandle,
        delta: Offset,
    ) {
        val candidate = if (isLocked) {
            resizedLocked(
                frame = frameRect,
                handle = handle,
                deltaX = delta.x,
                bounds = displayImageRect,
                ratio = lockedRatio,
            )
        } else {
            resizedFree(frame = frameRect, handle = handle, delta = delta, bounds = displayImageRect)
        }
        if (candidate.width < MIN_FRAME_SIZE || candidate.height < MIN_FRAME_SIZE) {
            return
        }
        val frame = clamped(rect = candidate, handle = handle)
        frameRect = frame
        aspectRatio = frame.width / frame.height
    }

    /** Pinch: scales by [zoomDelta] (within 1–5×) and moves by [panDelta]. */
    fun zoom(
        zoomDelta: Float,
        panDelta: Offset,
    ) {
        imageScale = (imageScale * zoomDelta).coerceIn(minimumValue = MIN_SCALE, maximumValue = MAX_SCALE)
        imageOffset = boundedOffset(imageOffset + panDelta)
    }

    /** Drag outside the frame: moves a zoomed image; does nothing at 1×. */
    fun pan(delta: Offset) {
        if (imageScale > MIN_SCALE) {
            imageOffset = boundedOffset(imageOffset + delta)
        }
    }

    fun rotateLeft() {
        rotate((rotation + QUARTER_TURN) % FULL_TURN)
    }

    fun rotateRight() {
        rotate((rotation - QUARTER_TURN) % FULL_TURN)
    }

    /**
     * A horizontal flip on screen. Its equivalent is the negated rotation with a flip before it, and the
     * offset flips with the content; the frame and the fitted image do not move.
     */
    fun mirror() {
        isMirrored = !isMirrored
        rotation = -rotation
        imageOffset = Offset(x = -imageOffset.x, y = imageOffset.y)
    }

    /** The frame's content as a JPEG [MediaFile], or the failure — a source the platform cannot decode. */
    suspend fun crop(): Result<MediaFile> {
        isProcessing = true
        return try {
            val rect = toNormalizedCropRect(
                frameRect = frameRect,
                displayImageRect = displayImageRect,
                imageScale = imageScale,
                imageOffset = imageOffset,
                rotation = rotation,
                mirrored = isMirrored,
            )
            Result.success(
                cropImage(
                    source = file,
                    rect = rect,
                    rotation = rotation,
                    mirrored = isMirrored,
                    maxOutputSize = options.maxOutputSize,
                    jpegQuality = options.jpegQuality,
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (
            // A single-image decode with no shared state: whatever the platform throws is this crop's failure.
            @Suppress("TooGenericExceptionCaught") failure: Exception,
        ) {
            Result.failure(failure)
        } finally {
            isProcessing = false
        }
    }

    private fun rotate(degrees: Float) {
        rotation = degrees
        imageScale = MIN_SCALE
        imageOffset = Offset.Zero
        relayout()
    }

    private fun relayout() {
        val isSwapped = isQuarterTurned(rotation)
        val rotatedSize = if (isSwapped) {
            Size(width = intrinsicSize.height, height = intrinsicSize.width)
        } else {
            intrinsicSize
        }
        val imageRect = fitRect(containerSize = containerSize, imageSize = rotatedSize)
        if (imageRect == Rect.Zero) {
            return
        }
        // The image is laid out unrotated; after a quarter turn this scale fits the turned image again.
        val unrotated = fitRect(containerSize = containerSize, imageSize = intrinsicSize)
        baseScale = if (isSwapped && unrotated.height > 0f) {
            imageRect.width / unrotated.height
        } else {
            1f
        }
        val isFirstLayout = displayImageRect == Rect.Zero
        displayImageRect = imageRect
        if (isFirstLayout) {
            val initial = options.initialRatio ?: AspectRatio.SQUARE.takeIf { options.isRatioLocked }
            isLocked = initial != null
            lockedRatio = initial?.value ?: 1f
        }
        val raw = if (isLocked) {
            centeredFrame(ratio = lockedRatio, imageRect = imageRect)
        } else {
            imageRect
        }
        val frame = clamped(rect = raw, handle = null)
        frameRect = frame
        aspectRatio = frame.width / frame.height
        if (isLocked) {
            lockedRatio = aspectRatio
        }
    }

    private fun clamped(
        rect: Rect,
        handle: CropHandle?,
    ): Rect {
        return clampRatio(
            rect = rect,
            bounds = displayImageRect,
            handle = handle,
            minRatio = options.minRatio,
            maxRatio = options.maxRatio,
        )
    }

    private fun boundedOffset(offset: Offset): Offset {
        val maxX = displayImageRect.width * (imageScale - 1f) / 2f
        val maxY = displayImageRect.height * (imageScale - 1f) / 2f
        return Offset(
            x = offset.x.coerceIn(minimumValue = -maxX, maximumValue = maxX),
            y = offset.y.coerceIn(minimumValue = -maxY, maximumValue = maxY),
        )
    }
}
