package io.thernal.mediakit.media.impl.presentation

import androidx.compose.runtime.Composable
import io.thernal.mediakit.media.api.presentation.CameraAccess
import io.thernal.mediakit.media.api.presentation.CameraPermission
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * MINIMAL PERMISSION CODE — replace it with your app's own.
 *
 * It asks once and reports the answer: no rationale, no statuses, no way to settings. An app that
 * handles permissions itself (permissions-kit, say) binds its own `CameraPermission` instead — see
 * `media/api/README.md` → The camera permission — and this file is never used.
 *
 * The app's Info.plist needs NSCameraUsageDescription either way.
 */
internal object IosCameraPermission : CameraPermission {
    private val access = object : CameraAccess {
        override val isGranted: Boolean
            get() = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized

        override suspend fun request(): Boolean {
            if (isGranted) {
                return true
            }
            // Answers at once, without a prompt, when the user has already refused.
            return suspendCoroutine { continuation ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted -> continuation.resume(granted) }
            }
        }
    }

    @Composable
    override fun rememberCameraAccess(): CameraAccess {
        return access
    }
}
