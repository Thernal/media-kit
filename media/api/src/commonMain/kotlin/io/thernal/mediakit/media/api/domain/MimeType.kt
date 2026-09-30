package io.thernal.mediakit.media.api.domain

/** The MIME types the kit names. A pattern such as `image/(star)` — written `ALL` in each family — matches the whole family. */
object MimeType {
    const val ALL = "*/*"

    object Image {
        const val ALL = "image/*"
        const val JPEG = "image/jpeg"
        const val PNG = "image/png"
        const val GIF = "image/gif"
        const val WEBP = "image/webp"
        const val HEIC = "image/heic"
        const val HEIF = "image/heif"

        /**
         * What the crop engine and Coil decode on both platforms. HEIC/HEIF are transcoded to JPEG at
         * pick time, so they never reach a caller; anything else an image picker returns (SVG, RAW) is
         * rejected there instead of failing later in the cropper.
         */
        val SUPPORTED: List<String> = listOf(JPEG, PNG, GIF, WEBP)
    }

    object Video {
        const val ALL = "video/*"
        const val MP4 = "video/mp4"
        const val QUICKTIME = "video/quicktime"
    }

    object Application {
        const val PDF = "application/pdf"
        const val OCTET_STREAM = "application/octet-stream"
    }

    /** Whether [mimeType] is one of [allowed]; an empty list, or one containing [ALL], allows everything. */
    fun matches(
        mimeType: String,
        allowed: List<String>,
    ): Boolean {
        if (allowed.isEmpty()) {
            return true
        }
        return allowed.any { pattern ->
            when {
                pattern == ALL -> true

                pattern.endsWith(
                    "/*",
                ) -> mimeType.substringBefore(delimiter = '/') == pattern.substringBefore(delimiter = '/')

                else -> mimeType.equals(other = pattern, ignoreCase = true)
            }
        }
    }
}
