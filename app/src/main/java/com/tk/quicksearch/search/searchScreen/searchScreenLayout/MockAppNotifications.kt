package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.BuildConfig
import com.tk.quicksearch.search.data.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// TODO: Remove before release. Temporary mock data for testing the App notifications glance UI.
/** Debug-only fake App notifications, shown in place of the real ones while [ENABLED]. */
internal object MockAppNotifications {
    val ENABLED = BuildConfig.DEBUG && true

    private const val KEY_PREFIX = "mock-app-notification-"

    private val state = MutableStateFlow(build())
    val notifications: StateFlow<List<AppNotification>> = state.asStateFlow()

    fun isMock(notification: AppNotification): Boolean = notification.key.startsWith(KEY_PREFIX)

    fun dismiss(notification: AppNotification) {
        state.value = state.value.filter { it.key != notification.key }
    }

    private fun build(): List<AppNotification> {
        val now = System.currentTimeMillis()
        var index = 0
        fun mock(packageName: String, title: String?, text: String?, minutesAgo: Int) =
            AppNotification(
                key = "$KEY_PREFIX${index++}",
                packageName = packageName,
                title = title,
                text = text,
                postTime = now - minutesAgo * 60_000L,
                contentIntent = null,
            )
        return listOf(
            mock("com.microsoft.teams", "Design sync", "Priya: Can you share the updated mockups before 3?", 1),
            mock("com.microsoft.teams", "Design sync", "Alex: I pushed the new icons to the shared folder", 6),
            mock("com.microsoft.teams", "Standup", "Jordan: Running 5 minutes late", 20),
            mock("com.microsoft.office.outlook", "Invoice #48213 is ready", "Your monthly statement from Columbia Gas is available to view.", 3),
            mock("com.microsoft.office.outlook", "Re: Weekend plans", "Sounds good, see you Saturday!", 45),
            mock("com.amazon.mShop.android.shopping", "Out for delivery", "Your package with USB-C Cable (2-Pack) will arrive today by 8 PM.", 8),
            mock("com.linkedin.android", "Sam Rivera viewed your profile", null, 12),
            mock(
                "com.robinhood.android",
                "Price alert: AAPL",
                "Apple is up 4.2% today. This is a deliberately long notification body to check how the row wraps or truncates when the text runs past two lines.",
                15,
            ),
            mock("com.reddit.frontpage", "Trending in r/androiddev", "Compose 2.0 is out — what's new?", 30),
            mock("me.lyft.android", "Your driver is arriving", "Look for a white Toyota Camry, plate 7XYZ123", 32),
            mock("com.google.android.keep", null, "Buy milk, eggs, coffee", 50),
            mock("com.spotify.music", "New release from an artist you follow", null, 90),
        )
    }
}
