package com.habitminer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CategoryDuration(
    val appCategory: String,
    val totalDuration: Long,
)

@Dao
interface AppUsageDao {
    @Insert
    suspend fun insert(usage: AppUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usages: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage WHERE dayType = :dayType AND timeSlot = :timeSlot ORDER BY startTime DESC")
    fun getUsageForTimeBin(
        dayType: String,
        timeSlot: String,
    ): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE startTime >= :startMs AND endTime <= :endMs ORDER BY startTime ASC")
    suspend fun getUsageForDateRange(
        startMs: Long,
        endMs: Long,
    ): List<AppUsageEntity>

    @Query("SELECT * FROM app_usage ORDER BY startTime DESC")
    fun getAllUsage(): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE startTime >= :startOfDayMs ORDER BY startTime DESC")
    fun getTodayUsage(startOfDayMs: Long): Flow<List<AppUsageEntity>>

    @Query(
        "SELECT appCategory, SUM(durationMs) as totalDuration FROM app_usage " +
            "WHERE startTime >= :startMs AND endTime <= :endMs GROUP BY appCategory ORDER BY totalDuration DESC",
    )
    suspend fun getTotalDurationByCategory(
        startMs: Long,
        endMs: Long,
    ): List<CategoryDuration>

    @Query("SELECT COUNT(*) FROM app_usage")
    fun getUsageCount(): Flow<Int>

    @Query("SELECT COUNT(*) || ':' || COALESCE(MAX(endTime), 0) || ':' || COALESCE(SUM(durationMs), 0) FROM app_usage")
    suspend fun getUsageRevision(): String

    @Query(
        "SELECT COUNT(*) || ':' || COALESCE(MAX(endTime), 0) || ':' || " +
            "COALESCE(SUM(durationMs), 0) FROM app_usage WHERE startTime < :beforeMs",
    )
    suspend fun getModelRevision(beforeMs: Long): String

    @Query("SELECT DISTINCT packageName FROM app_usage WHERE appName = packageName")
    suspend fun getPackagesWithPackageNameLabels(): List<String>

    @Query("UPDATE app_usage SET appName = :appName WHERE packageName = :packageName AND appName = packageName")
    suspend fun updateFallbackAppName(
        packageName: String,
        appName: String,
    )

    @Query("DELETE FROM app_usage WHERE startTime < :timestampMs")
    suspend fun deleteOlderThan(timestampMs: Long)

    @Query("DELETE FROM app_usage")
    suspend fun deleteAll()

    @Query("SELECT MAX(endTime) FROM app_usage")
    suspend fun getLastInsertedTimestamp(): Long?

    @Query("SELECT packageName FROM app_usage WHERE packageName NOT IN (:launcherPackages) ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastUsedNonLauncherPackage(launcherPackages: List<String>): String?
}
