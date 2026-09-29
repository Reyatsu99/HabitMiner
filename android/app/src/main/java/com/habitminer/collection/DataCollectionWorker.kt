package com.habitminer.collection

import android.content.Context
import android.os.PowerManager
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.habitminer.data.AppDatabase
import java.util.Calendar
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
            val eventDao = db.deviceEventDao()

            val preferences = appContext.getSharedPreferences("habitminer_model", Context.MODE_PRIVATE)
            val retentionDays = preferences.getInt("retention_days", 90).coerceIn(30, 180)
            val retentionCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
            usageDao.deleteOlderThan(retentionCutoff)
            contextDao.deleteOlderThan(retentionCutoff)
            eventDao.deleteOlderThan(retentionCutoff)

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

            val now = System.currentTimeMillis()
            val startOfDay = Calendar.getInstance().run {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                timeInMillis
            }
            val unlockCount = eventDao.countSince(DeviceEventReceiver.EVENT_UNLOCK, startOfDay)
            val notifCount = if (HabitNotificationListener.isEnabled(appContext)) {
                eventDao.countSince(DeviceEventReceiver.EVENT_NOTIFICATION, now - TimeUnit.HOURS.toMillis(1))
            } else -1
            val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val shouldSampleSensors = isScreenOn && (batteryLevel >= 15 || batteryManager.isCharging)

            val snapshot =
                sensorCollector.collectSnapshot(
                    unlockCount = unlockCount,
                    isScreenOn = isScreenOn,
                    notificationCount = notifCount,
                    collectSensors = shouldSampleSensors,
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
