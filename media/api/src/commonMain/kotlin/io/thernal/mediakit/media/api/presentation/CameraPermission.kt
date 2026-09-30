package io.thernal.mediakit.media.api.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The camera permission, as the image picker needs it: before opening the camera it reads
 * [CameraAccess.isGranted] and, if not, calls [CameraAccess.request].
 *
 * The kit's own implementation (`impl`, installed by `wiring`) is deliberately minimal: it asks once
 * and reports the answer, with no rationale and no way to settings. **An app with its own permission
 * handling replaces it** — with permissions-kit that is a one-file adapter over
 * `rememberPermissionState(AppPermission.Camera)`; see `media/api/README.md` → The camera permission.
 */
interface CameraPermission {
    @Composable
    fun rememberCameraAccess(): CameraAccess
}

@Stable
interface CameraAccess {
    val isGranted: Boolean

    /** Asks for the permission if the platform still can; returns whether it is granted now. */
    suspend fun request(): Boolean
}

/**
 * The implementation the image picker uses. Without `wiring` it reports granted — previews and tests
 * never prompt.
 */
val LocalCameraPermission = staticCompositionLocalOf<CameraPermission> { GrantedCameraPermission }

private object GrantedCameraPermission : CameraPermission {
    private val access = object : CameraAccess {
        override val isGranted: Boolean = true

        override suspend fun request(): Boolean {
            return true
        }
    }

    @Composable
    override fun rememberCameraAccess(): CameraAccess {
        return access
    }
}
