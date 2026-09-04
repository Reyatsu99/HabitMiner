package com.habitminer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.habitminer.service.LocationTrackingService

/**
 * Application class — initialises global singletons on startup.
 * Declared in AndroidManifest via android:name=".HabitMinerApp"
 */
class HabitMinerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * Creates all notification channels required by the app.
     * Must be called before starting any ForegroundService.
     * On API < 26 channels don't exist but the call is safe.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val locationChannel = NotificationChannel(
                LocationTrackingService.CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW // Low = no sound, shown in status bar only
            ).apply {
                description = "Persistent notification for background GPS collection. HabitMiner never transmits your data to the cloud."
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(locationChannel)
        }
    }
}
