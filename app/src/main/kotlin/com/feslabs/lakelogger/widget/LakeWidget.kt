package com.feslabs.lakelogger.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.feslabs.lakelogger.data.CachedReading
import com.feslabs.lakelogger.data.LakeApiClient
import com.feslabs.lakelogger.data.LakeApiException
import com.feslabs.lakelogger.data.SharedReadingCache

/**
 * Android's home-screen widget equivalent of the iOS app's WidgetKit
 * extension. Unlike iOS, Glance widgets run in the host app's own process,
 * so there's no separate extension target — this class is both the
 * "TimelineProvider" and the composable content.
 */
class LakeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cache = SharedReadingCache(context)

        // Try a quick network refresh first so the widget shows fresh data
        // right after it's added or manually refreshed, but always fall back
        // to the cache (populated by LakeRefreshWorker/the app) if the
        // network call is slow, offline, or fails outright.
        val state: LakeWidgetState = try {
            val latest = LakeApiClient().fetchCurrent()
            if (latest != null) {
                cache.save(latest)
                LakeWidgetState.Data(reading = latest, fetchedAtEpochMillis = System.currentTimeMillis())
            } else {
                cachedOrUnavailable(cache, errorMessage = null)
            }
        } catch (e: LakeApiException) {
            cachedOrUnavailable(cache, errorMessage = e.message)
        }

        provideContent {
            LakeWidgetContent(state = state)
        }
    }

    private suspend fun cachedOrUnavailable(cache: SharedReadingCache, errorMessage: String?): LakeWidgetState {
        val cached: CachedReading? = cache.load()
        return if (cached != null) {
            LakeWidgetState.Data(reading = cached.reading, fetchedAtEpochMillis = cached.fetchedAtEpochMillis)
        } else {
            LakeWidgetState.Unavailable(errorMessage ?: "No data yet")
        }
    }
}

sealed class LakeWidgetState {
    data class Data(val reading: com.feslabs.lakelogger.data.LakeReading, val fetchedAtEpochMillis: Long) : LakeWidgetState()
    data class Unavailable(val message: String) : LakeWidgetState()
}

/** Registers [LakeWidget] with the Android widget host; declared in AndroidManifest.xml. */
class LakeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LakeWidget()
}
