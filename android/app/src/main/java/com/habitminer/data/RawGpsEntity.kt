package com.habitminer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "raw_gps")
data class RawGpsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "accuracy")
    val accuracy: Float,
    
    @ColumnInfo(name = "activity_state")
    val activityState: String // e.g., STILL, WALKING, IN_VEHICLE
)
