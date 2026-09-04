package com.habitminer.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.habitminer.data.AppDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

class ExportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val locations = db.locationDao().getAllLocations()
            
            if (locations.isEmpty()) {
                Log.d("HabitMiner", "No locations to export.")
                return Result.success()
            }
            
            val jsonArray = JSONArray()
            for (loc in locations) {
                val jsonObj = JSONObject().apply {
                    put("id", loc.id)
                    put("latitude", loc.latitude)
                    put("longitude", loc.longitude)
                    put("timestamp", loc.timestamp)
                    put("accuracy", loc.accuracy.toDouble())
                    put("activity_state", loc.activityState)
                }
                jsonArray.put(jsonObj)
            }
            
            // Save to internal storage cache
            val exportFile = File(applicationContext.cacheDir, "trajectory_export_${System.currentTimeMillis()}.json")
            FileWriter(exportFile).use {
                it.write(jsonArray.toString(2))
            }
            
            Log.d("HabitMiner", "Exported ${locations.size} points to ${exportFile.absolutePath}")
            
            // Once successfully exported to a file, we could clear the DB or mark them as synced.
            // For now, we leave them in the DB.
            
            Result.success()
        } catch (e: Exception) {
            Log.e("HabitMiner", "Export failed", e)
            Result.failure()
        }
    }
}
