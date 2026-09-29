package com.habitminer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage",
    indices = [
        androidx.room.Index(value = ["startTime", "endTime"]),
        androidx.room.Index(value = ["dayType", "timeSlot"]),
        androidx.room.Index(value = ["packageName"]),
    ],
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val appCategory: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val timeSlot: String,
    val dayType: String,
)
