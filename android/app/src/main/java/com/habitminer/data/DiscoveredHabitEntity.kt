package com.habitminer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_habits")
data class DiscoveredHabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitName: String,
    val patternDescription: String,
    val appSequence: String,
    val confidence: Float,
    val occurrenceCount: Int,
    val timeSlot: String,
    val dayType: String,
    val discoveredAt: Long,
    val lastSeenAt: Long,
)
