package com.tk.quicksearch.search.data

import android.content.Context
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class CustomInfoItem(
    val id: Int,
    val title: String,
    val prompt: String,
    val providerId: AiSearchLlmProviderId,
    val modelId: String,
    val webSearch: Boolean,
    val thinking: Boolean,
    val dueMillis: Long?,
    val sendNotification: Boolean,
    val showOnHome: Boolean = true,
    val status: String = PENDING,
    val answer: String = "",
) {
    companion object {
        const val PENDING = "pending"
        const val RUNNING = "running"
        const val COMPLETE = "complete"
        const val ERROR = "error"
    }
}

/** Small durable queue and home-card history. API keys stay in the existing encrypted preferences. */
class CustomInfoRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("custom_info", Context.MODE_PRIVATE)

    fun all(): List<CustomInfoItem> = synchronized(lock) {
        val json = JSONArray(prefs.getString("items", "[]"))
        (0 until json.length()).mapNotNull { index ->
            runCatching {
                val item = json.getJSONObject(index)
                CustomInfoItem(
                    id = item.getInt("id"),
                    title = item.optString("title").ifBlank { item.getString("prompt") },
                    prompt = item.getString("prompt"),
                    providerId = AiSearchLlmProviderId.fromStorageValue(item.getString("provider")),
                    modelId = item.getString("model"),
                    webSearch = item.optBoolean("webSearch"),
                    thinking = item.optBoolean("thinking"),
                    dueMillis = item.optLong("due", 0L).takeIf { it > 0L },
                    sendNotification = item.optBoolean("notify"),
                    showOnHome = item.optBoolean("showOnHome", true),
                    status = item.optString("status", CustomInfoItem.PENDING),
                    answer = item.optString("answer"),
                )
            }.getOrNull()
        }
    }

    fun get(id: Int): CustomInfoItem? = all().firstOrNull { it.id == id }

    fun add(item: CustomInfoItem) = synchronized(lock) { write(all() + item) }

    fun update(id: Int, transform: (CustomInfoItem) -> CustomInfoItem) = synchronized(lock) {
        write(all().map { if (it.id == id) transform(it) else it })
    }

    fun delete(id: Int) = synchronized(lock) { write(all().filterNot { it.id == id }) }

    private fun write(items: List<CustomInfoItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("prompt", item.prompt)
                    put("provider", item.providerId.storageValue)
                    put("model", item.modelId)
                    put("webSearch", item.webSearch)
                    put("thinking", item.thinking)
                    put("due", item.dueMillis ?: 0L)
                    put("notify", item.sendNotification)
                    put("showOnHome", item.showOnHome)
                    put("status", item.status)
                    put("answer", item.answer)
                },
            )
        }
        prefs.edit().putString("items", array.toString()).apply()
        changes.value++
    }

    companion object {
        private val lock = Any()
        val changes = MutableStateFlow(0)
    }
}
