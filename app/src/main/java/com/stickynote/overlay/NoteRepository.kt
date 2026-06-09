package com.stickynote.overlay

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "sticker_prefs")

private val TABS_KEY = stringPreferencesKey("tabs_data")

data class NoteTab(
    val id: String,
    var title: String,
    var content: String
)

data class NoteData(
    val tabs: List<NoteTab>,
    val activeTabIndex: Int
)

object NoteRepository {

    private fun tabsToJson(tabs: List<NoteTab>, activeIndex: Int): String {
        val arr = JSONArray()
        for (tab in tabs) {
            val obj = JSONObject()
            obj.put("id", tab.id)
            obj.put("title", tab.title)
            obj.put("content", tab.content)
            arr.put(obj)
        }
        val root = JSONObject()
        root.put("tabs", arr)
        root.put("activeIndex", activeIndex)
        return root.toString()
    }

    private fun jsonToTabs(json: String): NoteData {
        val root = JSONObject(json)
        val arr = root.getJSONArray("tabs")
        val tabs = mutableListOf<NoteTab>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            tabs.add(NoteTab(
                id = obj.getString("id"),
                title = obj.optString("title", "Untitled"),
                content = obj.optString("content", "")
            ))
        }
        val activeIndex = root.optInt("activeIndex", 0).coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
        return NoteData(tabs, activeIndex)
    }

    suspend fun save(context: Context, tabs: List<NoteTab>, activeIndex: Int) {
        val json = tabsToJson(tabs, activeIndex)
        context.dataStore.edit { prefs ->
            prefs[TABS_KEY] = json
        }
    }

    suspend fun load(context: Context): NoteData {
        val json = context.dataStore.data.map { prefs ->
            prefs[TABS_KEY]
        }.first()

        if (json == null) {
            val defaultTab = NoteTab(
                id = java.util.UUID.randomUUID().toString(),
                title = "Note 1",
                content = ""
            )
            return NoteData(listOf(defaultTab), 0)
        }

        return try {
            jsonToTabs(json)
        } catch (_: Exception) {
            NoteData(listOf(NoteTab(
                id = java.util.UUID.randomUUID().toString(),
                title = "Note 1",
                content = ""
            )), 0)
        }
    }
}
