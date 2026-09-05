package com.habitminer.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads a pre-processed GeoLife trajectory JSON from assets/demo_trajectory.json
 * into the Room database, giving the app real data to work with instantly.
 *
 * The JSON was exported by engine/export_geolife_json.py from Microsoft's
 * GeoLife Research Dataset (User 000, Beijing, 2007-2012).
 */
class GeoLifeDataLoader(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun loadIntoDatabase(onComplete: (count: Int) -> Unit = {}) {
        scope.launch {
            try {
                val assetManager = context.assets
                val assetFiles = assetManager.list("") ?: emptyArray()

                if ("demo_trajectory.json" !in assetFiles) {
                    Log.w("HabitMiner", "demo_trajectory.json not found in assets. Run engine/export_geolife_json.py first.")
                    onComplete(0)
                    return@launch
                }

                val stream = assetManager.open("demo_trajectory.json")
                val reader = BufferedReader(InputStreamReader(stream))
                val json = JSONObject(reader.readText())
                reader.close()

                val gpsArray = json.getJSONArray("gps_records")
                val entities = mutableListOf<RawGpsEntity>()
                val cal = java.util.Calendar.getInstance()

                for (i in 0 until gpsArray.length()) {
                    val obj = gpsArray.getJSONObject(i)
                    val timestamp = obj.getLong("timestamp")
                    
                    // Simulate sensor context based on hour of day to show off the HabitEngine
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
                    
                    val act = obj.getString("activityState")
                    if (act != "STILL") {
                        audio += 20f // louder in transit
                        screen = false
                    }

                    entities.add(
                        RawGpsEntity(
                            latitude     = obj.getDouble("latitude"),
                            longitude    = obj.getDouble("longitude"),
                            timestamp    = timestamp,
                            accuracy     = obj.getDouble("accuracy").toFloat(),
                            activityState = act,
                            audioLevel = audio,
                            lightLevel = light,
                            isScreenOn = screen
                        )
                    )
                }

                // Clear existing data and load fresh GeoLife data
                db.locationDao().deleteAllLocations()
                db.locationDao().insertLocations(entities)

                Log.d("HabitMiner", "Loaded ${entities.size} real GeoLife GPS points into Room DB")
                onComplete(entities.size)

            } catch (e: Exception) {
                Log.e("HabitMiner", "Failed to load GeoLife data", e)
                onComplete(0)
            }
        }
    }
}
