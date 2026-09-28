package com.tk.quicksearch.shared.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors

@Composable
fun dialogTextFieldColors(
    unfocusedIndicatorColor: Color = MaterialTheme.colorScheme.outline,
): TextFieldColors =
    TextFieldDefaults.colors(
        focusedContainerColor = AppColors.Accent.copy(alpha = 0.14f),
        unfocusedContainerColor = AppColors.Accent.copy(alpha = 0.08f),
        disabledContainerColor = AppColors.Accent.copy(alpha = 0.05f),
        errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
        focusedIndicatorColor = AppColors.Accent,
        unfocusedIndicatorColor = unfocusedIndicatorColor,
        focusedLabelColor = AppColors.Accent,
        cursorColor = AppColors.Accent,
    )

/** Borderless, transparent fields for text inputs stacked as rows of a settings card. */
@Composable
fun cardTextFieldColors(): TextFieldColors =
    TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    )

/**
 * A borderless [TextField] for a row of a settings card. The label keeps a fixed gap above the
 * input so focusing never changes the field's height, and a hint taller than the field grows it
 * smoothly instead of in one frame.
 */
@Composable
fun CardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    // A single-line resting label is centered with its gap included, so nudge it back down by
    // half the gap until it floats. Offset is draw-only, so this never resizes the field.
    val restingLabelOffset by animateDpAsState(
        targetValue = if (singleLine && !focused && value.isEmpty()) CardTextFieldLabelGap / 2 else 0.dp,
        animationSpec = tween(durationMillis = CARD_TEXT_FIELD_LABEL_ANIMATION_MS),
        label = "cardTextFieldRestingLabelOffset",
    )
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.animateContentSize(),
        label = {
            Text(
                text = label,
                modifier = Modifier.offset(y = restingLabelOffset).padding(bottom = CardTextFieldLabelGap),
            )
        },
        placeholder = placeholder?.let {
            { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        colors = cardTextFieldColors(),
        interactionSource = interactionSource,
    )
}

private val CardTextFieldLabelGap = 6.dp

// Matches the Material 3 label float duration.
private const val CARD_TEXT_FIELD_LABEL_ANIMATION_MS = 150
