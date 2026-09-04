package com.habitminer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: RawGpsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<RawGpsEntity>)

    @Query("SELECT * FROM raw_gps ORDER BY timestamp ASC")
    fun getAllLocationsFlow(): Flow<List<RawGpsEntity>>
    
    @Query("SELECT * FROM raw_gps ORDER BY timestamp ASC")
    suspend fun getAllLocations(): List<RawGpsEntity>

    @Query("DELETE FROM raw_gps")
    suspend fun deleteAllLocations()
    
    @Query("SELECT COUNT(*) FROM raw_gps")
    fun getLocationCountFlow(): Flow<Int>
}
