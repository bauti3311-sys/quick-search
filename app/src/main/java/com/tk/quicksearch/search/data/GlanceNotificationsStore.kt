package com.tk.quicksearch.search.data

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.AlarmClock
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.RemoteViews
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A running timer or stopwatch posted by a clock app, counted from [chronometerBase]. */
internal class TimerNotification(
    val key: String,
    val packageName: String,
    val isCountDown: Boolean,
    /** Wall-clock time the timer ends, or the stopwatch started. */
    val chronometerBase: Long,
    val contentIntent: PendingIntent?,
    /** The frozen time left (or elapsed) while paused; null while it runs. */
    val pausedMillis: Long? = null,
)

/** An ongoing notification with a determinate progress bar, such as a delivery or a download. */
internal class ProgressNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val progress: Int,
    val progressMax: Int,
    val postTime: Long,
    val contentIntent: PendingIntent?,
)

/** A missed call notification posted by the phone app; [caller] is its title, usually the name or number. */
internal class MissedCallNotification(
    val key: String,
    val packageName: String,
    val caller: String?,
    /** When the call came in, or when the notification was posted if the app does not say. */
    val callTime: Long,
    val contentIntent: PendingIntent?,
)


/**
 * The At a Glance view of the posted notifications, fed by
 * [com.tk.quicksearch.search.apps.notificationDots.NotificationDotsListenerService] so it shares
 * the notification access already granted for notification dots and media.
 */
internal object GlanceNotificationsStore {
    private val timersState = MutableStateFlow<List<TimerNotification>>(emptyList())
    private val progressState = MutableStateFlow<List<ProgressNotification>>(emptyList())
    private val missedCallsState = MutableStateFlow<List<MissedCallNotification>>(emptyList())
    private var clockPackages: Set<String>? = null

    /** Timers read from custom chronometer views, by notification key, reused until the notification changes. */
    private val remoteTimerCache = mutableMapOf<String, Pair<Long, TimerNotification?>>()

    val timers: StateFlow<List<TimerNotification>> = timersState.asStateFlow()
    val progress: StateFlow<List<ProgressNotification>> = progressState.asStateFlow()
    val missedCalls: StateFlow<List<MissedCallNotification>> = missedCallsState.asStateFlow()

    fun update(
        context: Context,
        notifications: Array<StatusBarNotification>?,
    ) {
        val clocks = clockPackages ?: resolveClockPackages(context).also { clockPackages = it }
        val posted =
            notifications.orEmpty().filter { sbn ->
                sbn.packageName != context.packageName &&
                    sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0
            }
        // Clock apps need not mark a running timer ongoing (Google Clock does not), so any of their
        // notifications with a chronometer counts.
        remoteTimerCache.keys.retainAll(posted.map { it.key }.toSet())
        val timerNotifications = posted.mapNotNull { it.toTimer(context, clocks) }
        val timerKeys = timerNotifications.map { it.key }.toSet()
        timersState.value = timerNotifications.sortedBy { it.chronometerBase }
        progressState.value =
            posted
                .filter { sbn ->
                    sbn.key !in timerKeys &&
                        sbn.notification.flags and
                        (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0
                }
                .mapNotNull { it.toProgress() }
                .sortedByDescending { it.postTime }
        val dialerPackage = defaultDialerPackage(context)
        missedCallsState.value =
            posted.mapNotNull { it.toMissedCall(dialerPackage) }.sortedByDescending { it.callTime }
    }

    /** Drops cached state when notification access is lost; clock apps are resolved again on reconnect. */
    fun clear() {
        clockPackages = null
        remoteTimerCache.clear()
        timersState.value = emptyList()
        progressState.value = emptyList()
        missedCallsState.value = emptyList()
    }

    private fun defaultDialerPackage(context: Context): String? =
        runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()

    /**
     * Phone apps mark missed calls with the missed call category (Android 10+), and older ones at
     * least post them to a channel named for it, so the default dialer's channel id is checked too.
     */
    private fun StatusBarNotification.toMissedCall(dialerPackage: String?): MissedCallNotification? {
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return null
        val isMissedCall =
            notification.category == CATEGORY_MISSED_CALL ||
                (
                    packageName == dialerPackage &&
                        notification.channelIdOrNull()?.contains("missed", ignoreCase = true) == true
                )
        if (!isMissedCall) return null
        return MissedCallNotification(
            key = key,
            packageName = packageName,
            caller = missedCallCaller(),
            callTime = notification.`when`.takeIf { it > 0L } ?: postTime,
            contentIntent = notification.contentIntent,
        )
    }

    /**
     * The caller's name or number. Phone apps put it in the title (Google) or the text (Samsung),
     * with a generic "Missed call" label in the other. The lock screen version leaves the caller
     * out, so a field that also appears there is the label, not the caller.
     */
    private fun StatusBarNotification.missedCallCaller(): String? {
        val extras = notification.extras ?: return null
        val publicExtras = notification.publicVersion?.extras
        val labels =
            listOfNotNull(
                publicExtras?.getCharSequence(Notification.EXTRA_TITLE),
                publicExtras?.getCharSequence(Notification.EXTRA_TEXT),
            ).map { it.toString().trim() }.toSet()
        return listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT)
            .mapNotNull { extras.getCharSequence(it)?.toString()?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull { it !in labels }
    }

    private fun Notification.channelIdOrNull(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) channelId else null


    /** [Notification.CATEGORY_MISSED_CALL], which is only defined from Android 10. */
    private const val CATEGORY_MISSED_CALL = "missed_call"

    /** Apps that handle the standard timer or alarm intents; only their chronometers count as timers. */
    private fun resolveClockPackages(context: Context): Set<String> {
        val packageManager = context.packageManager
        return listOf(AlarmClock.ACTION_SET_TIMER, AlarmClock.ACTION_SHOW_ALARMS)
            .flatMap { action ->
                runCatching { packageManager.queryIntentActivities(Intent(action), 0) }.getOrNull().orEmpty()
            }.mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }

    private fun StatusBarNotification.toTimer(
        context: Context,
        clockPackages: Set<String>,
    ): TimerNotification? {
        if (packageName !in clockPackages) return null
        val extras = notification.extras ?: return null
        if (!extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)) {
            val cached = remoteTimerCache[key]
            if (cached != null && cached.first == postTime) return cached.second
            return toRemoteViewTimer(context, extras).also { remoteTimerCache[key] = postTime to it }
        }
        if (notification.`when` <= 0L) return null
        return TimerNotification(
            key = key,
            packageName = packageName,
            isCountDown = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN),
            chronometerBase = notification.`when`,
            contentIntent = notification.contentIntent,
        )
    }

    /**
     * Many clock apps leave the standard chronometer extras unset and show the live count in a
     * chronometer view instead: Samsung Clock in an extra of its own (its `when` is only the post
     * time), Google Clock and other AOSP-based clocks in a custom notification layout. Inflating the
     * first view that has one gives the chronometer's base and direction. Runs on the listener's
     * main thread, as view inflation needs.
     */
    @Suppress("DEPRECATION")
    private fun StatusBarNotification.toRemoteViewTimer(
        context: Context,
        extras: Bundle,
    ): TimerNotification? {
        val tag = extras.getString(EXTRA_SAMSUNG_CHRONOMETER_TAG)
        val chronometer =
            listOfNotNull(
                extras.getParcelable<RemoteViews>(EXTRA_SAMSUNG_CHRONOMETER_VIEW),
                notification.contentView,
                notification.bigContentView,
                notification.headsUpContentView,
            ).firstNotNullOfOrNull { remoteViews ->
                runCatching {
                    val view = remoteViews.apply(context, FrameLayout(context))
                    (tag?.let { view.findViewWithTag<View>(it) } as? Chronometer) ?: view.findChronometer()
                }.getOrNull()
            } ?: return null
        val elapsedNow = SystemClock.elapsedRealtime()
        val isCountDown = chronometer.isCountDown
        val base = System.currentTimeMillis() - (elapsedNow - chronometer.base)
        return TimerNotification(
            key = key,
            packageName = packageName,
            isCountDown = isCountDown,
            chronometerBase = base,
            contentIntent = notification.contentIntent,
            pausedMillis =
                if (chronometer.isStarted() == false) {
                    (if (isCountDown) chronometer.base - elapsedNow else elapsedNow - chronometer.base)
                        .coerceAtLeast(0L)
                } else {
                    null
                },
        )
    }

    private fun View.findChronometer(): Chronometer? {
        if (this is Chronometer) return this
        if (this !is ViewGroup) return null
        for (index in 0 until childCount) {
            getChildAt(index).findChronometer()?.let { return it }
        }
        return null
    }

    /** Whether the chronometer is counting, or null when that cannot be read (it is not public API). */
    private fun Chronometer.isStarted(): Boolean? =
        runCatching {
            Chronometer::class.java.getDeclaredField("mStarted").apply { isAccessible = true }.getBoolean(this)
        }.getOrNull()

    private const val EXTRA_SAMSUNG_CHRONOMETER_VIEW = "android.ongoingActivityNoti.chronometerRemoteView"
    private const val EXTRA_SAMSUNG_CHRONOMETER_TAG = "android.ongoingActivityNoti.chronometerRemoteViewTag"

    private fun StatusBarNotification.toProgress(): ProgressNotification? {
        val extras = notification.extras ?: return null
        if (notification.category == Notification.CATEGORY_TRANSPORT) return null
        if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return null
        if (extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)) return null
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        if (max <= 0) return null
        return ProgressNotification(
            key = key,
            packageName = packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() },
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() },
            progress = extras.getInt(Notification.EXTRA_PROGRESS, 0).coerceIn(0, max),
            progressMax = max,
            postTime = postTime,
            contentIntent = notification.contentIntent,
        )
    }
}
