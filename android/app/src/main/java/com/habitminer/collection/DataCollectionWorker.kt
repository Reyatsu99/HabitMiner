package com.habitminer.collection

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.habitminer.data.AppUsageDao
import com.habitminer.data.ContextDao
import com.habitminer.data.DeviceEventDao
import com.habitminer.domain.AppIdentityResolver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class DataCollectionWorker
    @AssistedInject
    constructor(
        @Assisted private val appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val usageDao: AppUsageDao,
        private val contextDao: ContextDao,
        private val eventDao: DeviceEventDao,
        private val usageCollector: UsageDataCollector,
        private val sensorCollector: SensorContextCollector,
        private val appIdentityResolver: AppIdentityResolver,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            return try {
                val preferences = appContext.getSharedPreferences("habitminer_model", Context.MODE_PRIVATE)
                if (!preferences.getBoolean("collection_enabled", true)) {
                    return Result.success()
                }

                val retentionDays = preferences.getInt("retention_days", 90).coerceIn(30, 180)
                val retentionCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
                usageDao.deleteOlderThan(retentionCutoff)
                contextDao.deleteOlderThan(retentionCutoff)
                eventDao.deleteOlderThan(retentionCutoff)

                val lastTimestamp =
                    usageDao.getLastInsertedTimestamp()
                        ?: (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14))

                val prevPkg = usageDao.getLastUsedNonLauncherPackage(appIdentityResolver.getLauncherPackages())
                val newUsage = usageCollector.collectUsageSince(lastTimestamp, prevPkg)
                if (newUsage.isNotEmpty()) {
                    usageDao.insertAll(newUsage)
                }

                val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isScreenOn = pm.isInteractive

                val now = System.currentTimeMillis()
                val startOfDay =
                    Calendar.getInstance().run {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        timeInMillis
                    }
                val unlockCount = eventDao.countSince(DeviceEventReceiver.EVENT_UNLOCK, startOfDay)
                val notificationsLastHour =
                    if (HabitNotificationListener.isEnabled(appContext)) {
                        eventDao.countSince(DeviceEventReceiver.EVENT_NOTIFICATION, now - TimeUnit.HOURS.toMillis(1))
                    } else {
                        -1
                    }
                val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                var batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                var isCharging = batteryManager.isCharging
                if (batteryLevel !in 0..100) {
                    val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                    val batteryIntent = appContext.registerReceiver(null, filter)
                    if (batteryIntent != null) {
                        val level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                        val scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                        if (level >= 0 && scale > 0) {
                            batteryLevel = (level * 100 / scale)
                        } else {
                            batteryLevel = -1
                        }
                        val status = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                        isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    } else {
                        batteryLevel = -1
                    }
                }

                val shouldSampleSensors = isScreenOn && (batteryLevel >= 15 || isCharging)

                val snapshot =
                    sensorCollector.collectSnapshot(
                        unlockCount = unlockCount,
                        isScreenOn = isScreenOn,
                        notificationsLastHour = notificationsLastHour,
                        collectSensors = shouldSampleSensors,
                        batteryLevel = batteryLevel,
                        isCharging = isCharging,
                    )

                contextDao.insert(snapshot)

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "DataCollectionWorker failed", e)
                Result.retry()
            }
        }

        companion object {
            private const val TAG = "HabitMiner"
            private const val WORK_NAME = "DataCollectionWorker"

            fun schedulePeriodicWork(context: Context) {
                val constraints =
                    androidx.work.Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()

                val request =
                    PeriodicWorkRequestBuilder<DataCollectionWorker>(15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
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
