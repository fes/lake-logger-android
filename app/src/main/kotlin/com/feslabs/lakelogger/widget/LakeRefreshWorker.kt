package com.feslabs.lakelogger.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.feslabs.lakelogger.data.LakeRepository
import java.util.concurrent.TimeUnit

/**
 * Periodically refreshes the cached reading and any placed widgets in the
 * background, mirroring the iOS widget's hourly `TimelineProvider` refresh.
 * WorkManager's minimum periodic interval is 15 minutes; the logger uploads
 * roughly hourly, so 30 minutes keeps the widget reasonably fresh without
 * excessive battery/network use.
 */
class LakeRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            LakeRepository(applicationContext).refreshCurrent()
            Result.success()
        } catch (e: Exception) {
            // Transient network/API failures should be retried, not treated
            // as a permanent failure — the widget still has its last cached
            // reading to show in the meantime.
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "lake_widget_refresh"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<LakeRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
