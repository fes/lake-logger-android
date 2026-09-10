package com.feslabs.lakelogger.data

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.feslabs.lakelogger.widget.LakeWidget

/**
 * Coordinates fetching from [LakeApiClient], persisting to [SharedReadingCache],
 * and nudging any placed widgets to redraw. Shared by the app's ViewModel and
 * the background [com.feslabs.lakelogger.widget.LakeRefreshWorker].
 */
class LakeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val api = LakeApiClient()
    private val cache = SharedReadingCache(appContext)

    suspend fun cachedReading(): CachedReading? = cache.load()

    suspend fun refreshCurrent(): LakeReading? {
        val latest = api.fetchCurrent()
        if (latest != null) {
            cache.save(latest)
            LakeWidget().updateAll(appContext)
        }
        return latest
    }

    suspend fun refreshHistory(days: Int): List<LakeReading> = api.fetchHistory(days)

    /** Returns true if at least one instance of the widget is on a home screen. */
    suspend fun hasActiveWidgets(): Boolean =
        GlanceAppWidgetManager(appContext).getGlanceIds(LakeWidget::class.java).isNotEmpty()
}
