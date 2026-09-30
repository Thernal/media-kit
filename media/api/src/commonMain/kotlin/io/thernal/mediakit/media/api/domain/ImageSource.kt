package io.thernal.mediakit.media.api.domain

/** Where an image picker takes its image from. */
enum class ImageSource {
    /** The system photo picker — no permission on either platform. */
    Gallery,

    /** A new photo. Asks for the camera permission first (`CameraPermission`). */
    Camera,

    /** The system document picker, limited to images — files outside the photo library. */
    Documents,
}
