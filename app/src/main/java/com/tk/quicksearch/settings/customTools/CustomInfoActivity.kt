package com.tk.quicksearch.settings.customTools

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.reminders.ReminderPermissions
import com.tk.quicksearch.search.data.CustomInfoItem
import com.tk.quicksearch.search.data.CustomInfoRepository
import com.tk.quicksearch.search.data.CustomInfoScheduler
import com.tk.quicksearch.search.data.fetchCustomInfoAnswer
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.settings.settingsDetailScreen.ReminderFormDialog
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailHeader
import com.tk.quicksearch.settings.shared.ModelFeatureSettingsCard
import com.tk.quicksearch.settings.shared.SettingsCheckboxPill
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.LlmTextModel
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomInfoActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    private fun finishWithSlide() {
        finish()
        overridePendingTransition(R.anim.custom_info_slide_in_left, R.anim.custom_info_slide_out_right)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguageManager.applySavedAppLanguage(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finishWithSlide()
        })
        val preferences = UserAppPreferences(applicationContext)
        setContent {
            val useDarkSystemBars = when (preferences.getAppThemeMode()) {
                com.tk.quicksearch.search.core.AppThemeMode.LIGHT -> false
                com.tk.quicksearch.search.core.AppThemeMode.DARK -> true
                com.tk.quicksearch.search.core.AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            SideEffect {
                val style = if (useDarkSystemBars) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            QuickSearchTheme(
                fontScaleMultiplier = preferences.getFontScaleMultiplier(),
                useSystemFont = preferences.shouldUseSystemFont(),
                appTheme = preferences.getAppTheme(),
                appThemeMode = preferences.getAppThemeMode(),
                backgroundSource = preferences.getBackgroundSource(),
                customImageUri = preferences.getCustomImageUri(),
                accentColorMode = preferences.getAccentColorMode(),
                customAccentColorArgb = preferences.getCustomAccentColorArgb(),
                deviceThemeEnabled = preferences.isDeviceThemeEnabled(),
            ) {
                SettingsScreenBackground(
                    appTheme = preferences.getAppTheme(),
                    overlayThemeIntensity = preferences.getOverlayThemeIntensity(),
                    deviceThemeEnabled = preferences.isDeviceThemeEnabled(),
                    amoledThemeEnabled = preferences.isAmoledThemeEnabled(),
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                ) {
                    CustomInfoEditor(preferences, onBack = ::finishWithSlide) { item ->
                        CustomInfoRepository(applicationContext).add(item)
                        CustomInfoScheduler.schedule(applicationContext, item)
                        finishWithSlide()
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomInfoEditor(
    preferences: UserAppPreferences,
    onBack: () -> Unit,
    onSave: (CustomInfoItem) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var providerId by remember { mutableStateOf(preferences.getAiSearchProviderId()) }
    var modelId by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var webSearch by remember { mutableStateOf(false) }
    var dueMillis by remember { mutableStateOf<Long?>(null) }
    var hasExplicitTime by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var sendNotification by remember { mutableStateOf(false) }
    var configuredIds by remember { mutableStateOf(emptySet<AiSearchLlmProviderId>()) }
    var modelsByProvider by remember { mutableStateOf(emptyMap<AiSearchLlmProviderId, List<LlmTextModel>>()) }
    val scope = rememberCoroutineScope()
    var showTestDialog by remember { mutableStateOf(false) }
    var testLoading by remember { mutableStateOf(false) }
    var testResponse by remember { mutableStateOf("") }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        sendNotification = granted
    }
    LaunchedEffect(Unit) {
        val ids = withContext(Dispatchers.IO) {
            preferences.getConfiguredLlmProviderIds().filter { !preferences.getLlmApiKey(it).isNullOrBlank() }.toSet()
        }
        configuredIds = ids
        if (providerId !in ids) providerId = ids.firstOrNull() ?: providerId
        val catalogs = ids.associateWith { id ->
            AiSearchLlmProviderRegistry.get(id, context).fallbackTextModels
        }
        modelsByProvider = catalogs
        modelId = preferences.getLlmModel(providerId).takeIf { selected ->
            catalogs[providerId]?.any { it.id == selected } == true
        } ?: catalogs[providerId]?.firstOrNull()?.id.orEmpty()
        thinking = preferences.isLlmThinkingEnabled(providerId)
        webSearch = preferences.isLlmGroundingEnabled(providerId)
        ids.forEach { id ->
            launch {
                val liveModels = withContext(Dispatchers.IO) {
                    val provider = AiSearchLlmProviderRegistry.get(id, context)
                    provider.fetchAvailableTextModels(preferences.getLlmApiKey(id).orEmpty(), context)
                        .getOrElse { provider.fallbackTextModels }
                }
                modelsByProvider = modelsByProvider + (id to liveModels)
                if (id == providerId && liveModels.none { it.id == modelId }) modelId = ""
            }
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        containerColor = Color.Transparent,
        topBar = {
            SettingsDetailHeader(
                title = stringResource(R.string.custom_info_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = DesignTokens.ContentHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.notes_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                )
                ModelFeatureSettingsCard(
                    selectedModelId = modelId,
                    selectedProviderId = providerId,
                    availableModels = modelsByProvider[providerId].orEmpty(),
                    availableModelsByProvider = modelsByProvider,
                    configuredProviderIds = configuredIds,
                    modelLabel = stringResource(R.string.settings_direct_search_model_label),
                    thinkingLabel = stringResource(R.string.settings_direct_search_thinking_label),
                    webSearchLabel = stringResource(R.string.settings_direct_search_grounding_label),
                    thinkingEnabled = thinking,
                    groundingEnabled = webSearch,
                    onModelSelected = { modelId = it },
                    onProviderModelSelected = { id, selected ->
                        providerId = id
                        modelId = selected
                        thinking = preferences.isLlmThinkingEnabled(id)
                        webSearch = preferences.isLlmGroundingEnabled(id)
                    },
                    onThinkingChange = { thinking = it },
                    onGroundingChange = { webSearch = it },
                    showThinkingCheckbox = supportsThinkingControl(providerId, modelId),
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.custom_info_prompt)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    minLines = 4,
                    maxLines = 8,
                    colors = dialogTextFieldColors(),
                )
                OutlinedButton(onClick = { showDateDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        dueMillis?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) }
                            ?: stringResource(R.string.custom_info_set_date_time),
                    )
                }
                SettingsCheckboxPill(
                    label = stringResource(R.string.custom_info_send_notification),
                    checked = sendNotification,
                    onCheckedChange = { checked ->
                        if (!checked) sendNotification = false
                        else if (ReminderPermissions.hasPostNotifications(context)) sendNotification = true
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        val testItem = CustomInfoItem(
                            id = 0,
                            title = title.trim(),
                            prompt = prompt.trim(),
                            providerId = providerId,
                            modelId = modelId,
                            webSearch = webSearch,
                            thinking = thinking && supportsThinkingControl(providerId, modelId),
                            dueMillis = dueMillis,
                            sendNotification = false,
                        )
                        showTestDialog = true
                        testLoading = true
                        testResponse = ""
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { fetchCustomInfoAnswer(context.applicationContext, testItem) }
                            testResponse = result.getOrElse { it.message ?: context.getString(R.string.direct_search_error_generic) }
                            testLoading = false
                        }
                    },
                    enabled = prompt.isNotBlank() && modelId.isNotBlank() && providerId in configuredIds && !testLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.custom_info_test)) }
            }
            Button(
                onClick = {
                    onSave(
                        CustomInfoItem(
                            id = (System.currentTimeMillis() % 1_000_000_000L).toInt(),
                            title = title.trim(),
                            prompt = prompt.trim(),
                            providerId = providerId,
                            modelId = modelId,
                            webSearch = webSearch,
                            thinking = thinking && supportsThinkingControl(providerId, modelId),
                            dueMillis = dueMillis,
                            sendNotification = sendNotification && ReminderPermissions.hasPostNotifications(context),
                        ),
                    )
                },
                enabled = title.isNotBlank() && prompt.isNotBlank() && modelId.isNotBlank() && providerId in configuredIds &&
                    dueMillis != null && dueMillis!! > System.currentTimeMillis(),
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.ContentHorizontalPadding).height(56.dp),
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        }
    }
    if (showDateDialog) {
        ReminderFormDialog(
            initialTitle = stringResource(R.string.custom_info_title),
            initialDateTimeMillis = dueMillis,
            initialAllDay = !hasExplicitTime,
            onDismiss = { showDateDialog = false },
            onConfirm = { _, millis, allDay ->
                hasExplicitTime = !allDay
                dueMillis = if (allDay) {
                    Calendar.getInstance().apply {
                        timeInMillis = millis
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else millis
                showDateDialog = false
            },
            titleResId = R.string.custom_info_set_date_time,
            confirmResId = R.string.dialog_save,
            extraActions = {},
            autoFocusTitle = false,
            showTitleInput = false,
            requireExplicitDateOrTime = true,
        )
    }
    if (showTestDialog) {
        AppAlertDialog(
            onDismissRequest = { showTestDialog = false },
            title = { Text(stringResource(R.string.custom_info_test_response)) },
            text = {
                if (testLoading) CircularProgressIndicator()
                else Text(testResponse, modifier = Modifier.verticalScroll(rememberScrollState()))
            },
            confirmButton = {
                TextButton(onClick = { showTestDialog = false }) { Text(stringResource(R.string.common_close)) }
            },
        )
    }
}
