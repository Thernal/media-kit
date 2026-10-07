package io.thernal.mediakit.media.ui.crop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal class AnimatedPose(
    rotationState: State<Float>,
    baseScaleState: State<Float>,
    mirrorState: State<Float>,
) {
    val rotation by rotationState
    val baseScale by baseScaleState
    val mirror by mirrorState
}

@Composable
internal fun rememberAnimatedPose(state: ImageCropState): AnimatedPose {
    // Before the mirror the rotation is negated, so the visual pose is the same and nothing animates.
    val visualRotation = if (state.isMirrored) {
        -state.rotation
    } else {
        state.rotation
    }
    // Accumulated without the 360° wrap, so each quarter turn animates the short way round.
    var unwrapped by remember { mutableFloatStateOf(visualRotation) }
    LaunchedEffect(visualRotation) {
        val delta = (visualRotation - unwrapped) % FULL_TURN
        unwrapped += (delta + FULL_TURN + HALF_TURN) % FULL_TURN - HALF_TURN
    }
    val spec = tween<Float>(TURN_ANIMATION_MILLIS)
    val mirrorTarget = if (state.isMirrored) {
        -1f
    } else {
        1f
    }
    return AnimatedPose(
        rotationState = animateFloatAsState(targetValue = -unwrapped, animationSpec = spec, label = "cropRotation"),
        baseScaleState = animateFloatAsState(
            targetValue = state.baseScale,
            animationSpec = spec,
            label = "cropBaseScale",
        ),
        mirrorState = animateFloatAsState(targetValue = mirrorTarget, animationSpec = spec, label = "cropMirror"),
    )
}

private const val TURN_ANIMATION_MILLIS = 220
