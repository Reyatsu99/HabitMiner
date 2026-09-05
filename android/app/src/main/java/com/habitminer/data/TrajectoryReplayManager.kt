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

    fun injectSyntheticDemoData(pointCount: Int = 50) {
        scope.launch {
            val baseTime = System.currentTimeMillis() / 1000
            val baseLat = 39.9000
            val baseLon = 116.3000
            val cal = java.util.Calendar.getInstance()
            
            val entities = mutableListOf<RawGpsEntity>()
            for (i in 0 until pointCount) {
                val timestamp = baseTime + (i * 60)
                cal.timeInMillis = timestamp * 1000L
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                
                var audio = 40f
                var light = 200f
                var screen = false
                
                when (hour) {
                    in 0..6 -> { audio = 15f; light = 2f; screen = false } // Sleeping
                    in 9..11, in 14..16 -> { audio = 35f; light = 300f; screen = false } // Deep Focus
                    in 19..22 -> { audio = 65f; light = 100f; screen = true } // Phone usage / Socializing
                    else -> { audio = 50f; light = 250f; screen = Math.random() > 0.7 } // Mixed
                }
                
                val act = if (i % 2 == 0) "STILL" else "IN_MOTION"
                if (act != "STILL") {
                    audio += 20f
                    screen = false
                }
                
                val latJitter = (Math.random() - 0.5) * 0.005
                val lonJitter = (Math.random() - 0.5) * 0.005
                entities.add(
                    RawGpsEntity(
                        latitude = baseLat + latJitter,
                        longitude = baseLon + lonJitter,
                        timestamp = timestamp,
                        accuracy = 4.5f,
                        activityState = act,
                        audioLevel = audio,
                        lightLevel = light,
                        isScreenOn = screen
                    )
                )
            }
            db.locationDao().insertLocations(entities)
            Log.d("HabitMiner", "Injected $pointCount synthetic demo points into Room DB.")
        }
    }
}

