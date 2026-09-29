package com.habitminer.collection

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class HabitNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val pkg = it.packageName
            val now = System.currentTimeMillis()
            notificationLog.add(NotificationEntry(pkg, now))
            clearOldEntries()
        }
    }

    companion object {
        private data class NotificationEntry(val packageName: String, val timestamp: Long)

        private val notificationLog = mutableListOf<NotificationEntry>()

        fun getNotificationCountLastHour(): Int {
            val now = System.currentTimeMillis()
            val oneHourAgo = now - 60 * 60 * 1000
            return notificationLog.count { it.timestamp >= oneHourAgo }
        }

        fun clearOldEntries() {
            val now = System.currentTimeMillis()
            val twoHoursAgo = now - 2 * 60 * 60 * 1000
            notificationLog.removeAll { it.timestamp < twoHoursAgo }
        }

        fun isEnabled(context: Context): Boolean {
            val cn = ComponentName(context, HabitNotificationListener::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(cn.flattenToString())
        }
    }
}
