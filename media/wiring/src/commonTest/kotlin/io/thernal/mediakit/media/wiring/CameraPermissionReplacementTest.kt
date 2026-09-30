package io.thernal.mediakit.media.wiring

import androidx.compose.runtime.ProvidedValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import io.thernal.mediakit.media.api.presentation.CameraPermission
import io.thernal.mediakit.media.testing.FakeCameraPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/** What an app with its own permission code writes: a container replacing the kit's. */
@BindingContainer
@ContributesTo(AppScope::class, replaces = [MediaCameraPermissionWiring::class])
interface AppCameraPermissionWiring {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideCameraPermission(): CameraPermission {
            return FakeCameraPermission(isGranted = true)
        }
    }
}

@DependencyGraph(AppScope::class)
interface TestAppGraph {
    val cameraPermission: CameraPermission
    val compositionLocals: Set<ProvidedValue<*>>
}

class CameraPermissionReplacementTest {
    @Test
    fun `an app's container replaces the kit's minimal camera permission`() {
        val graph = createGraph<TestAppGraph>()

        val permission = graph.cameraPermission
        assertEquals(FakeCameraPermission::class, permission::class)
        assertSame(permission, graph.cameraPermission)
        assertEquals(2, graph.compositionLocals.size)
    }
}
