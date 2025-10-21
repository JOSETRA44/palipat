package com.example.palipat.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.lang.reflect.Type

class StorageService(context: Context) {
    private val gson = Gson()
    private val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("timers_prefs") }
    )

    data class SessionLog(
        val timerId: Int,
        val name: String,
        val mode: String,
        val startTime: Long,
        val endTime: Long,
        val durationMillis: Long
    )

    private val KEY_TIMERS = stringPreferencesKey("timers_json")
    private val KEY_SESSIONS = stringPreferencesKey("sessions_json")
    private val KEY_SOUND_URI = stringPreferencesKey("sound_uri")

    data class TimerRecord(
        val id: Int,
        val name: String,
        val mode: String, // "Countdown" or "Stopwatch"
        val durationMillis: Long,
        val remainingMillis: Long,
        val elapsedMillis: Long,
        val isRunning: Boolean,
        val lastStartedAt: Long?
    )

    suspend fun save(list: List<TimerRecord>) {
        val json = gson.toJson(list)
        store.edit { prefs ->
            prefs[KEY_TIMERS] = json
        }
    }

    fun observe(): Flow<List<TimerRecord>> = store.data.map { prefs ->
        val json = prefs[KEY_TIMERS] ?: return@map emptyList()
        runCatching {
            val listType: Type = object : TypeToken<List<TimerRecord>>() {}.type
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, listType) as List<TimerRecord>
        }.getOrDefault(emptyList())
    }

    fun observeSessions(): Flow<List<SessionLog>> = store.data.map { prefs ->
        val json = prefs[KEY_SESSIONS] ?: return@map emptyList()
        runCatching {
            val listType: Type = object : TypeToken<List<SessionLog>>() {}.type
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, listType) as List<SessionLog>
        }.getOrDefault(emptyList())
    }

    suspend fun appendSession(newSession: SessionLog) {
        val prefs = store.data.first()
        val currentJson = prefs[KEY_SESSIONS]
        val current: List<SessionLog> = if (currentJson.isNullOrEmpty()) emptyList() else runCatching {
            val listType: Type = object : TypeToken<List<SessionLog>>() {}.type
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(currentJson, listType) as List<SessionLog>
        }.getOrDefault(emptyList())

        val updated = current + newSession
        val json = gson.toJson(updated)
        store.edit { p ->
            p[KEY_SESSIONS] = json
        }
    }

    fun observeSoundUri(): Flow<String?> = store.data.map { prefs ->
        prefs[KEY_SOUND_URI]
    }

    suspend fun setSoundUri(uriString: String?) {
        store.edit { p ->
            if (uriString.isNullOrEmpty()) p.remove(KEY_SOUND_URI) else p[KEY_SOUND_URI] = uriString
        }
    }
}
