package com.tk.quicksearch.search.searchScreen.searchRoute

import com.tk.quicksearch.search.appSettings.AppSettingInlineControls
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel

/** Binds the app-setting rows that edit their value inline to the current state and ViewModel setters. */
internal fun appSettingInlineControls(
    uiState: SearchUiState,
    viewModel: SearchViewModel,
): AppSettingInlineControls =
    AppSettingInlineControls(
        fontScaleMultiplier = uiState.fontScaleMultiplier,
        onFontScaleMultiplierChange = viewModel::setFontScaleMultiplier,
        appIconSizeStep = uiState.appIconSizeStep,
        onAppIconSizeStepChange = viewModel::setAppIconSizeStep,
        homeTextColorOverride = uiState.homeTextColorOverride,
        onHomeTextColorChange = viewModel::setHomeTextColorOverride,
        backgroundSource = uiState.backgroundSource,
        customImageUri = uiState.customImageUri,
        appThemeMode = uiState.appThemeMode,
        onAppThemeModeChange = viewModel::setAppThemeMode,
        topMatchesLimit = uiState.topMatchesLimit,
        onTopMatchesLimitChange = viewModel::setTopMatchesLimit,
    )
