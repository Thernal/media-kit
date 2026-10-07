package io.thernal.mediakit.sample.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.createGraph
import io.thernal.mediakit.media.ui.theme.MediaTheme
import io.thernal.mediakit.sample.designsystem.SampleTheme
import io.thernal.mediakit.sample.designsystem.media.sampleMediaStyles

/**
 * The sample's root: installs the graph's composition locals and the design system with media-kit's styles
 * mapped from it, as an application's root does.
 */
@Composable
fun SampleApp() {
    val graph = remember { createGraph<SampleGraph>() }
    // Once, at the root: the copy the spread makes is not worth avoiding.
    @Suppress("SpreadOperator")
    CompositionLocalProvider(*graph.compositionLocals.toTypedArray()) {
        SampleTheme {
            MediaTheme(styles = sampleMediaStyles()) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        SampleNavigation()
                    }
                }
            }
        }
    }
}
