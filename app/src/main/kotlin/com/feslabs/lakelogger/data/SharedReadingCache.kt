package com.feslabs.lakelogger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.lakeDataStore by preferencesDataStore(name = "lake_reading_cache")

/**
 * Caches the last successfully fetched reading on-disk so both the app UI
 * and the home-screen widget can show recent data instantly (and fall back
 * to it if a network refresh fails), without waiting on their own network
 * call. Since Android widgets run in-process (unlike iOS's separate
 * extension), this is a plain DataStore rather than a cross-process App
 * Group container, but serves the same purpose.
 */
class SharedReadingCache(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val readingKey = stringPreferencesKey("last_reading_json")
    private val fetchedAtKey = longPreferencesKey("last_reading_fetched_at_epoch_ms")

    suspend fun save(reading: LakeReading) {
        val encoded = json.encodeToString(LakeReading.serializer(), reading)
        context.lakeDataStore.edit { prefs ->
            prefs[readingKey] = encoded
            prefs[fetchedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun load(): CachedReading? {
        val prefs = context.lakeDataStore.data.first()
        val encoded = prefs[readingKey] ?: return null
        val reading = try {
            json.decodeFromString(LakeReading.serializer(), encoded)
        } catch (e: Exception) {
            return null
        }
        val fetchedAt = prefs[fetchedAtKey] ?: return null
        return CachedReading(reading, fetchedAt)
    }
}

data class CachedReading(val reading: LakeReading, val fetchedAtEpochMillis: Long)
