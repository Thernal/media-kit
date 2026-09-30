package io.thernal.mediakit.sample.shared

import androidx.compose.runtime.ProvidedValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph

/** The graph an application has; here it only collects what `PermissionsWiring` contributes. */
@DependencyGraph(AppScope::class)
interface SampleGraph {
    val compositionLocals: Set<ProvidedValue<*>>
}
