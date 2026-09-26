package com.tk.quicksearch.search.data

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.AlarmClock
import android.service.notification.StatusBarNotification
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

/**
 * The At a Glance view of the posted notifications, fed by
 * [com.tk.quicksearch.search.apps.notificationDots.NotificationDotsListenerService] so it shares
 * the notification access already granted for notification dots and media.
 */
internal object GlanceNotificationsStore {
    private val timersState = MutableStateFlow<List<TimerNotification>>(emptyList())
    private val progressState = MutableStateFlow<List<ProgressNotification>>(emptyList())
    private var clockPackages: Set<String>? = null

    /** Timers read from custom chronometer views, by notification key, reused until the notification changes. */
    private val remoteTimerCache = mutableMapOf<String, Pair<Long, TimerNotification?>>()

    val timers: StateFlow<List<TimerNotification>> = timersState.asStateFlow()
    val progress: StateFlow<List<ProgressNotification>> = progressState.asStateFlow()

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
    }

    /** Drops cached state when notification access is lost; clock apps are resolved again on reconnect. */
    fun clear() {
        clockPackages = null
        remoteTimerCache.clear()
        timersState.value = emptyList()
        progressState.value = emptyList()
    }

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
