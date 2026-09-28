package com.tk.quicksearch.searchEngines.inline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

/** Engine count at or below which the strip shows evenly spaced pills instead of scrolling icons. */
internal const val ENGINE_PILL_MAX_COUNT = 6

/** Engine count at or below which the pills also show the engine name. */
private const val LABELED_ENGINE_PILL_MAX_COUNT = 2

private val PILL_SHAPE = RoundedCornerShape(50)
private val PILL_ICON_SIZE = 16.dp
private val ICON_ONLY_PILL_ICON_SIZE = 18.dp
private val PILL_HORIZONTAL_PADDING = DesignTokens.SpacingMedium
private val PILL_VERTICAL_PADDING = DesignTokens.SpacingSmall

/**
 * A single row of outlined pills, one per engine, sharing the strip width equally. Pills carry the
 * engine name only while there are few enough of them for it to fit.
 */
@Composable
internal fun LabeledEnginePillRow(
    query: String,
    enabledEngines: List<SearchTarget>,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    predictedTarget: PredictedSubmitTarget?,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
) {
    val predictedTargetId = (predictedTarget as? PredictedSubmitTarget.SearchTarget)?.targetId
    val showLabels = enabledEngines.size <= LABELED_ENGINE_PILL_MAX_COUNT
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        enabledEngines.forEach { engine ->
            key(engine.getId()) {
                LabeledEnginePill(
                    engine = engine,
                    query = query,
                    onSearchEngineClick = onSearchEngineClick,
                    onSearchEngineLongPress = onSearchEngineLongPress,
                    isPredicted = predictedTargetId == engine.getId(),
                    showLabel = showLabels,
                    appIconShape = appIconShape,
                    iconPackPackage = iconPackPackage,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Icon and name for a strip with only one engine. Drawn without a pill because the whole strip is
 * the tap target (see `SearchEngineContent`).
 */
@Composable
internal fun SingleEngineLabel(
    engine: SearchTarget,
    query: String,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchTargetIcon(
            target = engine,
            iconSize = PILL_ICON_SIZE,
            style = IconRenderStyle.ADVANCED,
            appIconShape = appIconShape,
            iconPackPackage = iconPackPackage,
        )
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
        Text(
            text =
                stringResource(
                    if (isLikelyWebUrl(query)) R.string.open_with_engine else R.string.search_on_engine,
                    engine.getDisplayName(),
                ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LabeledEnginePill(
    engine: SearchTarget,
    query: String,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    isPredicted: Boolean,
    showLabel: Boolean,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Surface(
        shape = PILL_SHAPE,
        color = if (isPredicted) AppColors.InlineEngineHighlightBackground else Color.Transparent,
        border =
            BorderStroke(
                DesignTokens.BorderWidth,
                if (isPredicted) AppColors.InlineEngineHighlightBorder else AppColors.InlineEnginePillBorder,
            ),
        modifier =
            modifier.combinedClickable(
                onClick = {
                    hapticConfirm(view)()
                    onSearchEngineClick(query, engine)
                },
                onLongClick = onSearchEngineLongPress,
            ),
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = if (showLabel) PILL_HORIZONTAL_PADDING else DesignTokens.SpacingXSmall,
                    vertical = PILL_VERTICAL_PADDING,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchTargetIcon(
                target = engine,
                iconSize = if (showLabel) PILL_ICON_SIZE else ICON_ONLY_PILL_ICON_SIZE,
                style = IconRenderStyle.ADVANCED,
                appIconShape = appIconShape,
                iconPackPackage = iconPackPackage,
            )
            if (showLabel) {
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
                Text(
                    text = engine.getDisplayName(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
