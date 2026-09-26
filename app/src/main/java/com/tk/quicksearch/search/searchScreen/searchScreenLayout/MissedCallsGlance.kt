package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import android.content.Context
import android.content.Intent
import android.provider.CallLog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneMissed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import com.tk.quicksearch.search.data.MissedCallNotification
import java.text.NumberFormat

/**
 * One summary row for the phone app's missed call notifications, [calls] newest first. It names the
 * callers and, for more than one call, counts them; tapping it opens the phone app's call history.
 */
@Composable
internal fun MissedCallsRow(
    calls: List<MissedCallNotification>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val callers = remember(calls) { calls.mapNotNull { it.caller }.distinct().joinToString(", ") }
    val countLabel =
        remember(calls.size) { calls.size.takeIf { it > 1 }?.let { NumberFormat.getIntegerInstance().format(it) } }
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = Icons.Rounded.PhoneMissed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = stringResource(if (calls.size == 1) R.string.home_missed_call else R.string.home_missed_calls),
        subtitle = callers.ifBlank { null },
        pillText = countLabel,
        onClick = { openCallHistory(context, calls.first()) },
        onDismiss = onDismiss,
    )
}

/**
 * Opens the call history of the app that posted [latest] when it has one (the phone app), and
 * otherwise the notification's own target, such as WhatsApp's calls.
 */
private fun openCallHistory(
    context: Context,
    latest: MissedCallNotification,
) {
    AppLockGate.runAfterUnlock(context, latest.packageName) {
        // Only the app that posted the call is asked for its call history, so a WhatsApp or other
        // app's missed call opens that app (through its notification) rather than the phone app.
        val callLog =
            Intent(Intent.ACTION_VIEW)
                .setType(CallLog.Calls.CONTENT_TYPE)
                .setPackage(latest.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = runCatching { context.startActivity(callLog) }.isSuccess
        if (!opened) launchNotificationTarget(context, latest.packageName, latest.contentIntent)
    }
}
