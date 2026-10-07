package io.thernal.mediakit.media.api.presentation

data class ImagePickerOptions(
    /** How many images the gallery lets the user choose; the camera and documents always give one. */
    val maxItems: Int = 1,
    /** Files larger than this — after HEIC transcoding and orientation — are rejected. */
    val maxFileSizeBytes: Long? = null,
) {
    init {
        require(maxItems >= 1) { "maxItems must be at least 1, was $maxItems" }
    }
}
