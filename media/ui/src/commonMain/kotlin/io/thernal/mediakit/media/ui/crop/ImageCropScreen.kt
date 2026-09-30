package io.thernal.mediakit.media.ui.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.thernal.mediakit.media.api.domain.MediaFile
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val RATIO_MATCH_EPSILON = 0.01f
private const val INACTIVE_ALPHA = 0.6f

/**
 * A whole crop screen: the cropper on black, a top bar (cancel, done) and a bottom bar (ratios,
 * rotate, mirror). Both bars are slots — pass your own to use the app's design system; the defaults in
 * [ImageCropDefaults] need nothing but foundation. The labels are parameters of the defaults; the kit
 * ships no strings.
 */
@Composable
fun ImageCropScreen(
    state: ImageCropState,
    onCancel: () -> Unit,
    onCropped: (MediaFile) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
    style: ImageCropperStyle = ImageCropperStyle(),
    topBar: @Composable (done: () -> Unit) -> Unit = { done ->
        ImageCropDefaults.TopBar(state = state, onCancel = onCancel, onDone = done)
    },
    bottomBar: @Composable () -> Unit = { ImageCropDefaults.BottomBar(state = state) },
) {
    val scope = rememberCoroutineScope()
    val done: () -> Unit = {
        if (!state.isProcessing) {
            scope.launch { state.crop().fold(onSuccess = onCropped, onFailure = onError) }
        }
    }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        ImageCropper(state = state, style = style, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            topBar(done)
            Box(modifier = Modifier.weight(1f))
            bottomBar()
        }
    }
}

/** The default bars of [ImageCropScreen], usable on their own inside custom ones. */
object ImageCropDefaults {
    private val textStyle = TextStyle(color = Color.White, fontSize = 16.sp)

    @Composable
    fun TopBar(
        state: ImageCropState,
        onCancel: () -> Unit,
        onDone: () -> Unit,
        cancelLabel: String = "Cancel",
        doneLabel: String = "Done",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(text = cancelLabel, onClick = onCancel)
            TextButton(text = doneLabel, onClick = onDone, enabled = !state.isProcessing)
        }
    }

    @Composable
    fun BottomBar(
        state: ImageCropState,
        freeLabel: String = "Free",
        rotateLeftLabel: String = "Rotate left",
        mirrorLabel: String = "Mirror",
        rotateRightLabel: String = "Rotate right",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.ratios.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip(text = freeLabel, selected = !state.isLocked, onClick = state::unlockRatio)
                    state.ratios.forEach { ratio ->
                        Chip(
                            text = ratio.label,
                            selected = state.isLocked && abs(state.aspectRatio - ratio.value) < RATIO_MATCH_EPSILON,
                            onClick = { state.applyRatio(ratio) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(text = rotateLeftLabel, onClick = state::rotateLeft)
                TextButton(text = mirrorLabel, onClick = state::mirror)
                TextButton(text = rotateRightLabel, onClick = state::rotateRight)
            }
        }
    }

    @Composable
    private fun TextButton(
        text: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
    ) {
        val alpha = if (enabled) {
            1f
        } else {
            INACTIVE_ALPHA
        }
        BasicText(
            text = text,
            style = textStyle.copy(color = Color.White.copy(alpha = alpha)),
            modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(8.dp),
        )
    }

    @Composable
    private fun Chip(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        val shape = RoundedCornerShape(16.dp)
        val (content, fill) = if (selected) {
            Color.Black to Color.White
        } else {
            Color.White to Color.Transparent
        }
        BasicText(
            text = text,
            style = textStyle.copy(color = content),
            modifier = Modifier
                .background(color = fill, shape = shape)
                .border(width = 1.dp, color = Color.White, shape = shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
