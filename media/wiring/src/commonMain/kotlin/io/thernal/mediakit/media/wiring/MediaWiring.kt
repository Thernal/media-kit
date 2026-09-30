package io.thernal.mediakit.media.wiring

import androidx.compose.runtime.ProvidedValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.mediakit.media.api.presentation.CameraPermission
import io.thernal.mediakit.media.api.presentation.LocalCameraPermission
import io.thernal.mediakit.media.api.presentation.LocalMediaPickerProvider
import io.thernal.mediakit.media.impl.presentation.defaultCameraPermission
import io.thernal.mediakit.media.impl.presentation.platformMediaPickerProvider

/**
 * Contributes the platform's pickers and the graph's [CameraPermission] as `ProvidedValue`s into the
 * app graph's `Set<ProvidedValue<*>>`, which the application installs once at its root.
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface MediaWiring {
    companion object {
        @Provides
        @IntoSet
        fun provideMediaPickerProvider(): ProvidedValue<*> {
            return LocalMediaPickerProvider provides platformMediaPickerProvider()
        }

        @Provides
        @IntoSet
        fun provideCameraPermissionLocal(cameraPermission: CameraPermission): ProvidedValue<*> {
            return LocalCameraPermission provides cameraPermission
        }
    }
}

/**
 * The kit's minimal camera permission (ask once, report the answer). An app with its own permission
 * handling replaces this container rather than editing it:
 *
 * ```
 * @BindingContainer
 * @ContributesTo(AppScope::class, replaces = [MediaCameraPermissionWiring::class])
 * interface AppCameraPermissionWiring {
 *     @Binds val PermissionsKitCameraPermission.bind: CameraPermission
 * }
 * ```
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface MediaCameraPermissionWiring {
    companion object {
        @Provides
        fun provideCameraPermission(): CameraPermission {
            return defaultCameraPermission()
        }
    }
}
