package io.thernal.mediakit.media.testing

import androidx.compose.runtime.Composable
import io.thernal.mediakit.media.api.presentation.CameraAccess
import io.thernal.mediakit.media.api.presentation.CameraPermission

/** A camera permission a test sets: [isGranted] now, [willGrantOnRequest] when asked; [requests] counts asks. */
class FakeCameraPermission(
    var isGranted: Boolean = false,
    var willGrantOnRequest: Boolean = true,
) : CameraPermission {
    var requests: Int = 0
        private set

    val access: CameraAccess = object : CameraAccess {
        override val isGranted: Boolean
            get() = this@FakeCameraPermission.isGranted

        override suspend fun request(): Boolean {
            requests++
            if (!this@FakeCameraPermission.isGranted) {
                this@FakeCameraPermission.isGranted = willGrantOnRequest
            }
            return this@FakeCameraPermission.isGranted
        }
    }

    @Composable
    override fun rememberCameraAccess(): CameraAccess {
        return access
    }
}
