package com.habitminer.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.habitminer.data.AppDatabase
import com.habitminer.data.RawGpsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // For battery optimization, we track if user is STILL (simplified for this phase)
    private var isUserStill = false

    companion object {
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (isUserStill) {
                    // Skip recording if we know the user hasn't moved
                    return
                }
                
                for (location in locationResult.locations) {
                    Log.d("HabitMiner", "New Location: ${location.latitude}, ${location.longitude}")
                    saveLocationToDb(location)
                }
            }
        }
        
        setupNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        val notification = NotificationCompat.Builder(this, LocationTrackingService.CHANNEL_ID)
            .setContentTitle("HabitMiner is active")
            .setContentText("Collecting contextual location data...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        // TODO: Register ActivityRecognitionClient to update isUserStill flag
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LocationTrackingService.CHANNEL_ID,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun saveLocationToDb(location: android.location.Location) {
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val entity = RawGpsEntity(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = location.time / 1000, // Unix timestamp in seconds
                accuracy = location.accuracy,
                activityState = if (isUserStill) "STILL" else "IN_MOTION"
            )
            db.locationDao().insertLocation(entity)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not bound service
    }
}
