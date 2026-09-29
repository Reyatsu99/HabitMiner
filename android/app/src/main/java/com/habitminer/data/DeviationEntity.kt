package com.habitminer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "deviations",
    indices = [
        androidx.room.Index(value = ["timestamp"]),
    ],
)
data class DeviationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val timeBin: String,
    val deviationType: String,
    val description: String,
    val zScore: Float,
    val normalizedScore: Float,
    val affectedCategory: String,
)
