package io.thernal.mediakit.media.ui.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.thernal.mediakit.media.ui.theme.MediaTheme
import kotlin.math.abs

/** The default bars of [ImageCropScreen], usable on their own inside custom ones. They draw from an [ImageCropScreenStyle]. */
object ImageCropDefaults {
    @Composable
    fun TopBar(
        state: ImageCropState,
        onCancel: () -> Unit,
        onDone: () -> Unit,
        cancelLabel: String = "Cancel",
        doneLabel: String = "Done",
        style: ImageCropScreenStyle = MediaTheme.styles.cropScreen,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(style.topBarPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(text = cancelLabel, onClick = onCancel, style = style)
            TextButton(text = doneLabel, onClick = onDone, style = style, enabled = !state.isProcessing)
        }
    }

    @Composable
    fun BottomBar(
        state: ImageCropState,
        freeLabel: String = "Free",
        rotateLeftLabel: String = "Rotate left",
        mirrorLabel: String = "Mirror",
        rotateRightLabel: String = "Rotate right",
        style: ImageCropScreenStyle = MediaTheme.styles.cropScreen,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(style.bottomBarPadding),
            verticalArrangement = Arrangement.spacedBy(style.bottomBarSpacing),
        ) {
            if (state.ratios.isNotEmpty()) {
                RatioChips(state = state, freeLabel = freeLabel, style = style)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(text = rotateLeftLabel, onClick = state::rotateLeft, style = style)
                TextButton(text = mirrorLabel, onClick = state::mirror, style = style)
                TextButton(text = rotateRightLabel, onClick = state::rotateRight, style = style)
            }
        }
    }

    @Composable
    private fun RatioChips(
        state: ImageCropState,
        freeLabel: String,
        style: ImageCropScreenStyle,
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(style.ratioRowPadding),
            horizontalArrangement = Arrangement.spacedBy(style.ratioChip.spacing),
        ) {
            Chip(text = freeLabel, isSelected = !state.isLocked, onClick = state::unlockRatio, style = style)
            state.ratios.forEach { ratio ->
                Chip(
                    text = ratio.label,
                    isSelected = state.isLocked && abs(state.aspectRatio - ratio.value) < RATIO_MATCH_EPSILON,
                    onClick = { state.applyRatio(ratio) },
                    style = style,
                )
            }
        }
    }

    @Composable
    private fun TextButton(
        text: String,
        onClick: () -> Unit,
        style: ImageCropScreenStyle,
        enabled: Boolean = true,
    ) {
        val alpha = if (enabled) {
            1f
        } else {
            style.disabledAlpha
        }
        BasicText(
            text = text,
            style = style.textStyle.copy(color = style.contentColor.copy(alpha = alpha)),
            modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(style.buttonPadding),
        )
    }

    @Composable
    private fun Chip(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        style: ImageCropScreenStyle,
    ) {
        val chip = style.ratioChip
        val (content, fill) = if (isSelected) {
            chip.selectedContentColor to chip.selectedContainerColor
        } else {
            chip.contentColor to Color.Transparent
        }
        BasicText(
            text = text,
            style = style.textStyle.copy(color = content),
            modifier = Modifier
                .background(color = fill, shape = chip.shape)
                .border(width = chip.borderWidth, color = chip.borderColor, shape = chip.shape)
                .clickable(onClick = onClick)
                .padding(chip.contentPadding),
        )
    }
}

private const val RATIO_MATCH_EPSILON = 0.01f
