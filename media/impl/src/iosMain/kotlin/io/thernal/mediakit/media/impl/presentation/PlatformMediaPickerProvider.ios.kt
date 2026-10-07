package io.thernal.mediakit.media.impl.presentation

import io.thernal.mediakit.media.api.presentation.CameraPermission
import io.thernal.mediakit.media.api.presentation.MediaPickerProvider

actual fun platformMediaPickerProvider(): MediaPickerProvider {
    return IosMediaPickerProvider
}

actual fun defaultCameraPermission(): CameraPermission {
    return IosCameraPermission
}
