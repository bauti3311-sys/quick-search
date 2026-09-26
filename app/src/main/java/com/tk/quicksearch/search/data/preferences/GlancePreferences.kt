package com.tk.quicksearch.search.data.preferences

import android.content.Context

/**
 * Stores the home At a Glance toggles for contact birthdays, low storage, running timers and live
 * progress notifications, plus the birthdays dismissed for the current day and the low storage
 * row's dismissal.
 */
class GlancePreferences(context: Context) : BasePreferences(context) {
    fun isShowBirthdaysEnabled(): Boolean = getBooleanPref(KEY_SHOW_BIRTHDAYS, true)

    fun setShowBirthdaysEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_BIRTHDAYS, enabled)

    fun isShowLowStorageEnabled(): Boolean = getBooleanPref(KEY_SHOW_LOW_STORAGE, true)

    fun setShowLowStorageEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_LOW_STORAGE, enabled)

    fun isShowTimersEnabled(): Boolean = getBooleanPref(KEY_SHOW_TIMERS, true)

    fun setShowTimersEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_TIMERS, enabled)

    fun isShowProgressNotificationsEnabled(): Boolean = getBooleanPref(KEY_SHOW_PROGRESS_NOTIFICATIONS, true)

    fun setShowProgressNotificationsEnabled(enabled: Boolean) =
        setBooleanPref(KEY_SHOW_PROGRESS_NOTIFICATIONS, enabled)

    /** Contact ids dismissed on [day] (an ISO date); dismissals from earlier days are ignored. */
    fun getDismissedBirthdays(day: String): Set<Long> =
        if (prefs.getString(KEY_DISMISSED_BIRTHDAYS_DAY, null) == day) {
            getStringSet(KEY_DISMISSED_BIRTHDAYS).mapNotNull { it.toLongOrNull() }.toSet()
        } else {
            emptySet()
        }

    fun dismissBirthday(day: String, contactId: Long) {
        val dismissed = getDismissedBirthdays(day) + contactId
        prefs.edit()
            .putString(KEY_DISMISSED_BIRTHDAYS_DAY, day)
            .putStringSet(KEY_DISMISSED_BIRTHDAYS, dismissed.map { it.toString() }.toSet())
            .apply()
    }

    /**
     * Free space, as a percent of the total, when the low storage row was dismissed (raised if space
     * is freed since), or null when it is not dismissed.
     */
    fun getLowStorageDismissedFreePercent(): Float? =
        if (prefs.contains(KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT)) {
            prefs.getFloat(KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT, 0f)
        } else {
            null
        }

    fun setLowStorageDismissedFreePercent(freePercent: Float?) {
        prefs.edit().apply {
            if (freePercent == null) {
                remove(KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT)
            } else {
                putFloat(KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT, freePercent)
            }
        }.apply()
    }

    companion object {
        /** The low storage row appears once free space drops to this share of the total. */
        const val LOW_STORAGE_THRESHOLD_PERCENT = 10

        private const val KEY_SHOW_BIRTHDAYS = "home_show_birthdays"
        private const val KEY_SHOW_LOW_STORAGE = "home_show_low_storage"
        private const val KEY_SHOW_TIMERS = "home_show_timers"
        private const val KEY_SHOW_PROGRESS_NOTIFICATIONS = "home_show_progress_notifications"
        private const val KEY_DISMISSED_BIRTHDAYS_DAY = "home_dismissed_birthdays_day"
        private const val KEY_DISMISSED_BIRTHDAYS = "home_dismissed_birthdays"
        private const val KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT = "home_low_storage_dismissed_free_percent"
    }
}
