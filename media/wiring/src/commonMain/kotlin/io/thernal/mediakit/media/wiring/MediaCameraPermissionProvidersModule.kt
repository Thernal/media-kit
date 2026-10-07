package io.thernal.mediakit.media.wiring

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import io.thernal.mediakit.media.api.presentation.CameraPermission
import io.thernal.mediakit.media.impl.presentation.defaultCameraPermission

/**
 * The kit's minimal camera permission (ask once, report the answer). An app with its own permission
 * handling replaces this container rather than editing it:
 *
 * ```
 * @BindingContainer
 * @ContributesTo(AppScope::class, replaces = [MediaCameraPermissionProvidersModule::class])
 * interface AppCameraPermissionBindingsModule {
 *     @Binds val PermissionsKitCameraPermission.bind: CameraPermission
 * }
 * ```
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface MediaCameraPermissionProvidersModule {
    companion object {
        @Provides
        fun provideCameraPermission(): CameraPermission {
            return defaultCameraPermission()
        }
    }
}
