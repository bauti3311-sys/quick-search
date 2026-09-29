package com.tk.quicksearch.search.searchScreen.searchRoute

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.settings.settingsDetailScreen.AiApiKeyRequiredDialog
import com.tk.quicksearch.settings.settingsDetailScreen.AppSuggestionTabsDialog
import com.tk.quicksearch.settings.settingsDetailScreen.PriorityReorderDialog
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.applySettingsCommand
import com.tk.quicksearch.shared.featureFlags.FeatureFlags

/** Settings dialogs opened directly from app-setting search results. */
internal enum class AppSettingRouteDialog {
    TOP_MATCHES_PRIORITY,
    APP_SUGGESTION_TABS,
    API_KEY_REQUIRED,
}

@Composable
internal fun AppSettingRouteDialogs(
    settingActions: RouteSettingActions,
    viewModel: SearchViewModel,
    uiState: SearchUiState,
) {
    val activeDialog = settingActions.activeDialog
    val onDismiss = { activeDialog.value = null }
    when (activeDialog.value) {
        AppSettingRouteDialog.TOP_MATCHES_PRIORITY -> {
            val priorityItems =
                remember(uiState.topMatchesSectionOrder) {
                    uiState.topMatchesSectionOrder.filter(FeatureFlags::isSearchSectionEnabled)
                }
            PriorityReorderDialog(
                items = priorityItems,
                onItemsChange = { order ->
                    viewModel.applySettingsCommand(SettingsCommand.TopMatchesSectionOrder(order))
                },
                disabledSections = uiState.disabledTopMatchesSections,
                onItemEnabledChange = { section, enabled ->
                    viewModel.applySettingsCommand(SettingsCommand.TopMatchesSectionEnabled(section, enabled))
                },
                onDismiss = onDismiss,
            )
        }
        AppSettingRouteDialog.APP_SUGGESTION_TABS ->
            AppSuggestionTabsDialog(
                enabledTabs = uiState.enabledAppSuggestionTabs,
                onTabEnabledChange = { tab, enabled ->
                    viewModel.applySettingsCommand(SettingsCommand.AppSuggestionTabEnabled(tab, enabled))
                },
                onDismiss = onDismiss,
            )
        AppSettingRouteDialog.API_KEY_REQUIRED ->
            AiApiKeyRequiredDialog(
                onDismiss = onDismiss,
                onSetupKey = {
                    settingActions.onOpenAppSettingDestination(AppSettingsDestination.API_KEY_SETUP)
                },
            )
        null -> Unit
    }
}
