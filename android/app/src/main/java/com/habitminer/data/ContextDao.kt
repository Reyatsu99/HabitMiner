package com.habitminer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: ContextSnapshotEntity)

    @Query("SELECT * FROM context_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSnapshot(): Flow<ContextSnapshotEntity?>

    @Query("SELECT * FROM context_snapshots WHERE timestamp >= :startMs AND timestamp <= :endMs ORDER BY timestamp ASC")
    suspend fun getSnapshotsForDateRange(
        startMs: Long,
        endMs: Long,
    ): List<ContextSnapshotEntity>

    @Query("SELECT * FROM context_snapshots WHERE timestamp >= :sinceMs ORDER BY timestamp ASC")
    suspend fun getSnapshotsSince(sinceMs: Long): List<ContextSnapshotEntity>

    @Query(
        "SELECT COALESCE(" +
            "date(MAX(timestamp) / 1000, 'unixepoch', 'localtime') || ':' || " +
            "CAST(CAST(strftime('%H', MAX(timestamp) / 1000, 'unixepoch', 'localtime') AS INTEGER) / 6 AS TEXT), " +
            "''" +
            ") FROM context_snapshots",
    )
    suspend fun getSnapshotRevision(): String

    @Query("SELECT COUNT(*) || ':' || COALESCE(MAX(timestamp), 0) FROM context_snapshots WHERE timestamp < :beforeMs")
    suspend fun getModelRevision(beforeMs: Long): String

    @Query("DELETE FROM context_snapshots WHERE timestamp < :timestampMs")
    suspend fun deleteOlderThan(timestampMs: Long)

    @Query("DELETE FROM context_snapshots")
    suspend fun deleteAll()
}
