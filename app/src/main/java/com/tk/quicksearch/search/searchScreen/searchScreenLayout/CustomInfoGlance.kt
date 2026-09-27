package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.CustomInfoItem
import com.tk.quicksearch.search.data.CustomInfoRepository
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class CustomInfoGlance(val items: List<CustomInfoItem>)

@Composable
internal fun rememberCustomInfoGlance(enabled: Boolean): CustomInfoGlance {
    val context = LocalContext.current
    val repository = remember(context) { CustomInfoRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val change by CustomInfoRepository.changes.collectAsState()
    var resumeCount by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(emptyList<CustomInfoItem>()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeCount++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(enabled, change, resumeCount) {
        if (enabled) {
            items = withContext(Dispatchers.IO) {
                repository.all().filter { it.showOnHome && it.status == CustomInfoItem.COMPLETE && it.answer.isNotBlank() }
            }
        } else {
            items = emptyList()
        }
    }
    return CustomInfoGlance(items)
}

@Composable
internal fun CustomInfoRow(item: CustomInfoItem) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 7.dp, top = DesignTokens.SpacingMedium, bottom = DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            modifier = Modifier.weight(1f).padding(start = DesignTokens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            Text(item.answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = {
            CustomInfoRepository(context).update(item.id) { it.copy(showOnHome = false) }
        }, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
