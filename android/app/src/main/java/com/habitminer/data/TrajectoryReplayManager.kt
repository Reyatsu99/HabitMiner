package com.habitminer.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads GeoLife PLT files or CSV synthetic data to simulate real GPS movement
 * without actually driving around. Injects data directly into Room DB.
 */
class TrajectoryReplayManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun replayGeolifePltFile(inputStreamReader: InputStreamReader) {
        scope.launch {
            try {
                val reader = BufferedReader(inputStreamReader)
                var line: String?
                var lineCount = 0
                val entities = mutableListOf<RawGpsEntity>()
                
                // Skip the first 6 lines of GeoLife header
                for (i in 0 until 6) {
                    reader.readLine()
                }

                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split(",")
                    if (parts.size >= 7) {
                        val lat = parts[0].toDoubleOrNull() ?: continue
                        val lon = parts[1].toDoubleOrNull() ?: continue
                        val alt = parts[3].toDoubleOrNull() ?: continue
                        val dateStr = parts[5]
                        val timeStr = parts[6]
                        
                        // Simplistic parsing for replay purposes.
                        // In a real app we'd convert this accurately using SimpleDateFormat
                        // We will just mock the timestamp for sequential entry
                        
                        entities.add(
                            RawGpsEntity(
                                latitude = lat,
                                longitude = lon,
                                timestamp = System.currentTimeMillis() / 1000 + lineCount, // Simulated sequential time
                                accuracy = 5.0f,
                                activityState = "REPLAY"
                            )
                        )
                        lineCount++
                    }
                }
                
                db.locationDao().insertLocations(entities)
                Log.d("HabitMiner", "Replayed $lineCount points successfully.")
                
            } catch (e: Exception) {
                Log.e("HabitMiner", "Failed to replay trajectory", e)
            }
        }
    }
}
