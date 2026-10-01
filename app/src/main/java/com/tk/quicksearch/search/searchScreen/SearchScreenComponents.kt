package com.tk.quicksearch.search.searchScreen

// Re-export all components from their respective files for backward compatibility

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.shared.ui.theme.DesignTokens

// Import the modifier functions
import com.tk.quicksearch.search.searchScreen.components.predictedSubmitHighlight as componentsPredictedSubmitHighlight
import com.tk.quicksearch.search.searchScreen.components.predictedSubmitCardBorder as componentsPredictedSubmitCardBorder

// Modifiers
internal fun Modifier.predictedSubmitHighlight(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
    opaqueCardTopResultBorder: Boolean = false,
): Modifier = componentsPredictedSubmitHighlight(isPredicted, shape, opaqueCardTopResultBorder)

internal fun Modifier.predictedSubmitCardBorder(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
): Modifier = componentsPredictedSubmitCardBorder(isPredicted, shape)

// Cards
@Composable
internal fun PermissionDisabledCard(
    title: String,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
) = com.tk.quicksearch.search.searchScreen.components.PermissionDisabledCard(
    title = title,
    message = message,
    actionLabel = actionLabel,
    onActionClick = onActionClick,
)

@Composable
internal fun UsagePermissionCard(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
) = com.tk.quicksearch.search.searchScreen.components.UsagePermissionCard(
    modifier = modifier,
    onRequestPermission = onRequestPermission,
    onDismiss = onDismiss,
)

// Banners
@Composable
internal fun InfoBanner(message: String) =
    com.tk.quicksearch.search.searchScreen.components.InfoBanner(message)

// Search Field - SPOTLIGHT MACOS REDESIGN (Pill + 5 Circular Buttons)
@Composable
internal fun PersistentSearchBar(
    query: String,
    selectRetainedQuery: Boolean,
    onSelectRetainedQueryHandled: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsIcon: Boolean = true,
    dismissKeyboardBeforeSettingsClick: Boolean = false,
    enabledTargets: List<com.tk.quicksearch.search.core.SearchTarget>,
    shortcutCodes: Map<String, String> = emptyMap(),
    shortcutEnabled: Map<String, Boolean> = emptyMap(),
    triggerWords: Collection<String> = emptyList(),
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    onSearchAction: () -> Boolean,
    onMoveTopResultSelectionUp: (() -> Boolean)? = null,
    onMoveTopResultSelectionDown: (() -> Boolean)? = null,
    shouldUseNumberKeyboard: Boolean,
    detectedShortcutTarget: com.tk.quicksearch.search.core.SearchTarget? = null,
    detectedAliasSearchSection: com.tk.quicksearch.search.core.SearchSection? = null,
    isCurrencyConverterAliasMode: Boolean = false,
    isWorldClockAliasMode: Boolean = false,
    isDictionaryAliasMode: Boolean = false,
    isWeatherAliasMode: Boolean = false,
    detectedCustomToolId: String? = null,
    detectedTaskerIntentId: String? = null,
    activeToolType: SearchToolType? = null,
    isCalculatorMode: Boolean = false,
    placeholderText: String,
    showWelcomeAnimation: Boolean = false,
    showWallpaperBackground: Boolean = false,
    opaqueBackground: Boolean = false,
    forceRestingOutline: Boolean = false,
    autoFocusOnStart: Boolean = false,
    releaseFocusOnLeave: Boolean = false,
    restoreKeyboardOnEnter: Boolean = false,
    onRestoreKeyboardHandled: () -> Unit = {},
    startupSurfaceReady: Boolean = true,
    onClearDetectedShortcut: () -> Unit = {},
    onSectionSelected: (com.tk.quicksearch.search.core.SearchSection) -> Unit = {},
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    onPressWhileKeyboardClosed: () -> Unit = {},
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    transparentBackground: Boolean = false,
    cornerRadius: androidx.compose.ui.unit.Dp = com.tk.quicksearch.shared.ui.theme.DesignTokens.Spacing28,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cápsula central (Pill) con bordes redondeados estilo Spotlight
        Box(
            modifier = Modifier.weight(1f)
        ) {
            com.tk.quicksearch.search.searchScreen.components.PersistentSearchBar(
                query = query,
                selectRetainedQuery = selectRetainedQuery,
                onSelectRetainedQueryHandled = onSelectRetainedQueryHandled,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
                onSettingsClick = onSettingsClick,
                showSettingsIcon = false, // Lo movemos al botón circular de la derecha
                dismissKeyboardBeforeSettingsClick = dismissKeyboardBeforeSettingsClick,
                enabledTargets = enabledTargets,
                shortcutCodes = shortcutCodes,
                shortcutEnabled = shortcutEnabled,
                triggerWords = triggerWords,
                isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled,
                onSearchAction = onSearchAction,
                onMoveTopResultSelectionUp = onMoveTopResultSelectionUp,
                onMoveTopResultSelectionDown = onMoveTopResultSelectionDown,
                shouldUseNumberKeyboard = shouldUseNumberKeyboard,
                detectedShortcutTarget = detectedShortcutTarget,
                detectedAliasSearchSection = detectedAliasSearchSection,
                isCurrencyConverterAliasMode = isCurrencyConverterAliasMode,
                isWorldClockAliasMode = isWorldClockAliasMode,
                isDictionaryAliasMode = isDictionaryAliasMode,
                isWeatherAliasMode = isWeatherAliasMode,
                detectedCustomToolId = detectedCustomToolId,
                detectedTaskerIntentId = detectedTaskerIntentId,
                activeToolType = activeToolType,
                isCalculatorMode = isCalculatorMode,
                placeholderText = "Spotlight Search",
                showWelcomeAnimation = showWelcomeAnimation,
                showWallpaperBackground = showWallpaperBackground,
                opaqueBackground = false,
                forceRestingOutline = true,
                autoFocusOnStart = autoFocusOnStart,
                releaseFocusOnLeave = releaseFocusOnLeave,
                restoreKeyboardOnEnter = restoreKeyboardOnEnter,
                onRestoreKeyboardHandled = onRestoreKeyboardHandled,
                startupSurfaceReady = startupSurfaceReady,
                onClearDetectedShortcut = onClearDetectedShortcut,
                onSectionSelected = onSectionSelected,
                onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
                onPressWhileKeyboardClosed = onPressWhileKeyboardClosed,
                focusRequester = focusRequester,
                transparentBackground = true,
                cornerRadius = 50.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Fila de los 5 botones circulares adyacentes de tu HTML
        SpotlightSideButtons(
            onSettingsClick = onSettingsClick,
            onQueryChange = onQueryChange
        )
    }
}

@Composable
private fun SpotlightSideButtons(
    onSettingsClick: () -> Unit,
    onQueryChange: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Apps [ A ]
        SpotlightCirclePill(
            text = "A",
            isAccent = false,
            onClick = { onQueryChange("apps ") }
        )

        // 2. Archivos [ 📁 ]
        SpotlightCirclePill(
            text = "📁",
            isAccent = false,
            onClick = { onQueryChange("files ") }
        )

        // 3. Acciones / Layers [ 🥞 ]
        SpotlightCirclePill(
            text = "🥞",
            isAccent = false,
            onClick = { onQueryChange("actions ") }
        )

        // 4. Portapapeles [ 📄 ]
        SpotlightCirclePill(
            text = "📄",
            isAccent = false,
            onClick = { onQueryChange("") }
        )

        // 5. Ajustes / Paleta [ ⚙️ ]
        SpotlightCirclePill(
            text = "⚙️",
            isAccent = true,
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun SpotlightCirclePill(
    text: String,
    isAccent: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isAccent) Color(0xFF007AFF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f)
    val borderColor = if (isAccent) Color(0xFF007AFF) else Color.White.copy(alpha = 0.22f)

    Box(
        modifier = Modifier
            .size(46.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// Pills
@Composable
internal fun KeyboardSwitchPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.KeyboardSwitchPill(
    text = text,
    onClick = onClick,
    modifier = modifier,
)

@Composable
internal fun PhoneCallPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.PhoneCallPill(
    onClick = onClick,
    modifier = modifier,
)

@Composable
internal fun SetAlarmPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.SetAlarmPill(
    onClick = onClick,
    modifier = modifier,
)

@Composable
internal fun StartTimerPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.StartTimerPill(
    onClick = onClick,
    modifier = modifier,
)

@Composable
internal fun CreateReminderPill(
    onClick: () -> Unit,
    useShortLabel: Boolean = false,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.CreateReminderPill(
    onClick = onClick,
    useShortLabel = useShortLabel,
    modifier = modifier,
)

@Composable
internal fun OpenKeyboardAction(
    text: String,
    onClick: () -> Unit,
    onVoiceClick: () -> Unit,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.OpenKeyboardAction(
    text = text,
    onClick = onClick,
    onVoiceClick = onVoiceClick,
    showWallpaperBackground = showWallpaperBackground,
    modifier = modifier,
)

@Composable
internal fun OverlayExpandPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.OverlayExpandPill(
    text = text,
    onClick = onClick,
    modifier = modifier,
)

@Composable
internal fun NumberKeyboardOperatorPills(
    onOperatorClick: (String) -> Unit,
    isOverlayPresentation: Boolean = false,
    extendToScreenEdges: Boolean = true,
    showWallpaperBackground: Boolean = false,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.NumberKeyboardOperatorPills(
    onOperatorClick = onOperatorClick,
    isOverlayPresentation = isOverlayPresentation,
    extendToScreenEdges = extendToScreenEdges,
    showWallpaperBackground = showWallpaperBackground,
    modifier = modifier,
)

// Empty State
@Composable
internal fun EmptyState() =
    com.tk.quicksearch.search.searchScreen.components.EmptyState()
