package io.thernal.mediakit.media.api.presentation

import androidx.compose.runtime.Stable

@Stable
interface CameraAccess {
    val isGranted: Boolean

    /** Asks for the permission if the platform still can; returns whether it is granted now. */
    suspend fun request(): Boolean
}
