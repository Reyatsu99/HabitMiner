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

                for (i in 0 until gpsArray.length()) {
                    val obj = gpsArray.getJSONObject(i)
                    entities.add(
                        RawGpsEntity(
                            latitude     = obj.getDouble("latitude"),
                            longitude    = obj.getDouble("longitude"),
                            timestamp    = obj.getLong("timestamp"),
                            accuracy     = obj.getDouble("accuracy").toFloat(),
                            activityState = obj.getString("activityState")
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
