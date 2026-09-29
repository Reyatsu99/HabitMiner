package com.habitminer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HabitMinerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Habit Deviation Alerts"
        val descriptionText = "Notifications for habit deviations"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel =
            NotificationChannel("habit_deviations", name, importance).apply {
                description = descriptionText
            }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
