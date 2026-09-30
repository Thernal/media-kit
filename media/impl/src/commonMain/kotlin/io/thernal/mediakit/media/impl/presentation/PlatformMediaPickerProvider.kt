package io.thernal.mediakit.media.impl.presentation

import io.thernal.mediakit.media.api.presentation.CameraPermission
import io.thernal.mediakit.media.api.presentation.MediaPickerProvider

/** The pickers for the platform this is compiled for; `wiring` installs them. */
expect fun platformMediaPickerProvider(): MediaPickerProvider

/**
 * The kit's minimal camera permission — ask once, report the answer. `wiring` installs it unless the
 * app replaces it with its own permission code (see `CameraPermission`).
 */
expect fun defaultCameraPermission(): CameraPermission
