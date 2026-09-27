package com.tk.quicksearch.search.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.reminders.ReminderPermissions
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.LlmRequest
import com.tk.quicksearch.tools.aiSearch.modelSupportsGrounding
import com.tk.quicksearch.tools.aiSearch.prepareWebSearch
import com.tk.quicksearch.tools.aiSearch.providerSupportsNativeSearch
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Shared by scheduled delivery and the editor's preview. Call from a background dispatcher. */
suspend fun fetchCustomInfoAnswer(context: Context, item: CustomInfoItem): Result<String> {
    val preferences = UserAppPreferences(context)
    val provider = AiSearchLlmProviderRegistry.get(item.providerId, context)
    val key = preferences.getLlmApiKey(item.providerId)
    if (key.isNullOrBlank()) {
        return Result.failure(IllegalStateException(context.getString(R.string.direct_search_error_no_key)))
    }
    val models = provider.fallbackTextModels
    val web = prepareWebSearch(
        userPreferences = preferences,
        searchQuery = item.prompt,
        prompt = item.prompt,
        nativeSearchSupported = providerSupportsNativeSearch(item.providerId) &&
            modelSupportsGrounding(item.modelId, models, item.providerId),
        nativeSearchRequested = item.webSearch,
    )
    val customPayload = preferences.getCustomLlmAdvancedPayloadByProvider()[item.providerId]
    return provider.fetchAnswer(
        apiKey = key,
        context = context,
        request = LlmRequest(
            query = web.prompt,
            modelId = item.modelId,
            useGroundingWithGoogleSearch = web.useNativeSearch,
            thinkingEnabled = item.thinking && item.providerId != AiSearchLlmProviderId.OPENAI && !item.providerId.isCustom,
            useSystemInstruction = models.firstOrNull { it.id == item.modelId }?.supportsSystemInstructions ?: true,
            systemInstruction = "Answer the user's request briefly. Keep the entire answer to at most 5–6 short lines.",
            advancedPayloadJson = customPayload?.second?.takeIf { customPayload.first },
        ),
    ).map { it.text }
}

/** An alarm starts a network job; JobScheduler keeps the request alive if the network is unavailable. */
object CustomInfoScheduler {
    private const val ALARM_ACTION = "com.tk.quicksearch.CUSTOM_INFO_DUE"
    private const val EXTRA_ID = "id"
    private const val JOB_BASE = 4_000_000

    fun schedule(context: Context, item: CustomInfoItem) {
        val due = item.dueMillis ?: return
        if (due <= System.currentTimeMillis()) {
            enqueue(context, item.id)
            return
        }
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = alarmIntent(context, item.id)
        if (ReminderPermissions.canScheduleExactAlarms(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, intent)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, intent)
        }
    }

    fun cancel(context: Context, id: Int) {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(context, id))
        context.getSystemService(JobScheduler::class.java)?.cancel(JOB_BASE + id)
        NotificationManagerCompat.from(context).cancel(JOB_BASE + id)
    }

    fun rescheduleAll(context: Context) {
        CustomInfoRepository(context).all().filter {
            it.status == CustomInfoItem.PENDING || it.status == CustomInfoItem.RUNNING
        }.forEach {
            schedule(context, it)
        }
    }

    fun enqueue(context: Context, id: Int) {
        val job = JobInfo.Builder(JOB_BASE + id, ComponentName(context, CustomInfoJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setMinimumLatency(0L)
            .setExtras(PersistableBundle().apply { putInt(EXTRA_ID, id) })
            .build()
        context.getSystemService(JobScheduler::class.java)?.schedule(job)
    }

    private fun alarmIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            JOB_BASE + id,
            Intent(context, CustomInfoAlarmReceiver::class.java).setAction(ALARM_ACTION).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    internal fun onAlarm(context: Context, intent: Intent) {
        if (intent.action != ALARM_ACTION) return
        val id = intent.getIntExtra(EXTRA_ID, -1)
        if (id > 0 && CustomInfoRepository(context).get(id)?.status == CustomInfoItem.PENDING) enqueue(context, id)
    }

    internal fun notifyResult(context: Context, item: CustomInfoItem) {
        if (!item.sendNotification || !ReminderPermissions.hasPostNotifications(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel("custom_info", context.getString(R.string.custom_info_title), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(context, JOB_BASE + item.id, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, "custom_info")
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(item.title)
            .setContentText(item.answer)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.answer))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(JOB_BASE + item.id, notification) }
    }
}

class CustomInfoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = CustomInfoScheduler.onAlarm(context, intent)
}

class CustomInfoJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Int, Job>()

    override fun onStartJob(params: JobParameters): Boolean {
        val id = params.extras.getInt("id", -1)
        if (id <= 0) return false
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val repository = CustomInfoRepository(applicationContext)
            try {
                val item = repository.get(id)
                if (item == null || item.status == CustomInfoItem.COMPLETE) {
                    jobFinished(params, false)
                    return@launch
                }
                repository.update(id) { it.copy(status = CustomInfoItem.RUNNING) }
                val result = fetchCustomInfoAnswer(applicationContext, item)
                val answer = result.getOrElse { it.message ?: getString(R.string.direct_search_error_generic) }
                repository.update(id) {
                    it.copy(status = if (result.isSuccess) CustomInfoItem.COMPLETE else CustomInfoItem.ERROR, answer = answer)
                }
                repository.get(id)?.let { CustomInfoScheduler.notifyResult(applicationContext, it) }
                jobFinished(params, false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                repository.update(id) {
                    it.copy(status = CustomInfoItem.ERROR, answer = error.message ?: getString(R.string.direct_search_error_generic))
                }
                jobFinished(params, false)
            } finally {
                jobs.remove(id)
            }
        }
        jobs[id] = job
        job.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        jobs.remove(params.extras.getInt("id", -1))?.cancel()
        return true
    }
}
