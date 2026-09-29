package com.habitminer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "baseline")
data class BaselineEntity(
    @PrimaryKey val timeBin: String,
    val avgScreenTimeMs: Long,
    val stdScreenTimeMs: Long,
    val avgSessionCount: Float,
    val stdSessionCount: Float,
    val avgUnlockCount: Float,
    val typicalCategoriesJson: String,
    val dominantMotionState: String,
    val avgLightLevel: Float,
    val updatedAt: Long,
    val dataPointCount: Int,
)
