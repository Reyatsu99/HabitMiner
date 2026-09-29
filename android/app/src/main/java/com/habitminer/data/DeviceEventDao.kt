package com.habitminer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DeviceEventDao {
    @Insert
    suspend fun insert(event: DeviceEventEntity)

    @Query("SELECT COUNT(*) FROM device_events WHERE eventType = :eventType AND timestamp >= :sinceMs")
    suspend fun countSince(
        eventType: String,
        sinceMs: Long,
    ): Int

    @Query("DELETE FROM device_events WHERE timestamp < :timestampMs")
    suspend fun deleteOlderThan(timestampMs: Long)

    @Query("DELETE FROM device_events")
    suspend fun deleteAll()
}
