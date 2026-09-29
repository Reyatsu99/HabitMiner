package com.habitminer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_snapshots",
    indices = [
        androidx.room.Index(value = ["timestamp"]),
    ],
)
data class ContextSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val motionState: String,
    val lightLevel: Float,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val isScreenOn: Boolean,
    val unlockCount: Int,
    val notificationCount: Int,
)
