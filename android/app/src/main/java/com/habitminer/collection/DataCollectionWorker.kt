package com.habitminer.collection

import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.habitminer.data.AppDatabase
import java.util.concurrent.TimeUnit

class DataCollectionWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(appContext)
            val usageDao = db.appUsageDao()
            val contextDao = db.contextDao()

            val retentionCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
            usageDao.deleteOlderThan(retentionCutoff)
            contextDao.deleteOlderThan(retentionCutoff)

            val lastTimestamp = usageDao.getLastInsertedTimestamp() ?: (System.currentTimeMillis() - 24 * 60 * 60 * 1000)

            val appIdentityResolver = com.habitminer.domain.AppIdentityResolver(appContext)
            val usageCollector = UsageDataCollector(appContext, appIdentityResolver)
            val newUsage = usageCollector.collectUsageSince(lastTimestamp)

            if (newUsage.isNotEmpty()) {
                usageDao.insertAll(newUsage)
            }

            val sensorCollector = SensorContextCollector(appContext)

            val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isScreenOn = pm.isInteractive

            val notifCount =
                if (HabitNotificationListener.isEnabled(appContext)) {
                    HabitNotificationListener.getNotificationCountLastHour()
                } else {
                    -1
                }

            // A placeholder for unlock count; to truly track unlock count would require a broadcast receiver
            val snapshot =
                sensorCollector.collectSnapshot(
                    unlockCount = 0,
                    isScreenOn = isScreenOn,
                    notificationCount = notifCount,
                )

            contextDao.insert(snapshot)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        fun schedulePeriodicWork(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<DataCollectionWorker>(15, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DataCollectionWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<DataCollectionWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
