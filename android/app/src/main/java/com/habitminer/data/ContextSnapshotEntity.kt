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
    val accelMean: Float,
    val accelVariance: Float,
    val accelStd: Float,
    val accelMin: Float,
    val accelMax: Float,
    val accelEnergy: Float,
    val gyroMean: Float,
    val gyroVariance: Float,
    val gyroStd: Float,
    val gyroMin: Float,
    val gyroMax: Float,
    val gyroEnergy: Float,
    val lightLux: Float,
    val proximityNear: Boolean?,
    val stepsSinceLastSnapshot: Int,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val isScreenOn: Boolean,
    val unlockCount: Int,
    val notificationsLastHour: Int,
)
