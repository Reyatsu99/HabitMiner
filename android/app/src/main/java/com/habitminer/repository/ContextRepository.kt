package com.habitminer.repository

import com.habitminer.data.AppUsageDao
import com.habitminer.data.AppUsageEntity
import com.habitminer.data.ContextDao
import com.habitminer.data.ContextSnapshotEntity
import com.habitminer.data.DeviceEventDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextRepository
    @Inject
    constructor(
        private val appUsageDao: AppUsageDao,
        private val contextDao: ContextDao,
        private val deviceEventDao: DeviceEventDao,
    ) {
        fun getLatestSnapshot(): Flow<ContextSnapshotEntity?> = contextDao.getLatestSnapshot()

        fun getTodayUsage(startOfDayMs: Long): Flow<List<AppUsageEntity>> = appUsageDao.getTodayUsage(startOfDayMs)

        fun getAllUsage(): Flow<List<AppUsageEntity>> = appUsageDao.getAllUsage()

        suspend fun getSnapshotsSince(sinceMs: Long): List<ContextSnapshotEntity> = contextDao.getSnapshotsSince(sinceMs)

        suspend fun getUsageRevision(): String = appUsageDao.getUsageRevision()

        suspend fun getModelRevision(beforeMs: Long): String =
            "${appUsageDao.getModelRevision(beforeMs)}|${contextDao.getModelRevision(beforeMs)}"

        suspend fun getPackagesWithFallbackNames(): List<String> = appUsageDao.getPackagesWithPackageNameLabels()

        suspend fun updateFallbackAppName(
            packageName: String,
            appName: String,
        ) = appUsageDao.updateFallbackAppName(packageName, appName)

        suspend fun getUsageCount(): Int = appUsageDao.getUsageCount().first()

        suspend fun getLastInsertedUsageTimestamp(): Long? = appUsageDao.getLastInsertedTimestamp()

        suspend fun getSnapshotRevision(): String = contextDao.getSnapshotRevision()

        suspend fun insertSnapshot(snapshot: ContextSnapshotEntity) = contextDao.insert(snapshot)

        suspend fun insertAppUsage(usage: AppUsageEntity) = appUsageDao.insert(usage)

        suspend fun insertAllAppUsage(usages: List<AppUsageEntity>) = appUsageDao.insertAll(usages)

        suspend fun clearCollectedData() {
            appUsageDao.deleteAll()
            contextDao.deleteAll()
            deviceEventDao.deleteAll()
        }
    }
