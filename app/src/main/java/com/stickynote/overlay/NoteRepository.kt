package com.stickynote.overlay

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sticker_prefs")

private val NOTE_KEY = stringPreferencesKey("persistent_note")

object NoteRepository {

    suspend fun saveNote(context: Context, text: String) {
        context.dataStore.edit { prefs ->
            prefs[NOTE_KEY] = text
        }
    }

    suspend fun loadNote(context: Context): String {
        return context.dataStore.data.map { prefs ->
            prefs[NOTE_KEY] ?: ""
        }.first()
    }
}
