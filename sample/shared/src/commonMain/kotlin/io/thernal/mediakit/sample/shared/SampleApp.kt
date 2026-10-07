package io.thernal.mediakit.sample.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.createGraph

/** The sample's root: installs the graph's composition locals, as an application's root does. */
@Composable
fun SampleApp() {
    val graph = remember { createGraph<SampleGraph>() }
    // Once, at the root: the copy the spread makes is not worth avoiding.
    @Suppress("SpreadOperator")
    CompositionLocalProvider(*graph.compositionLocals.toTypedArray()) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                SampleNavigation()
            }
        }
    }
}
