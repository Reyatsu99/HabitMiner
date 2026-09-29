package com.habitminer.collection

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.habitminer.data.AppDatabase
import com.habitminer.data.DeviceEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { notification ->
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(applicationContext).deviceEventDao().insert(
                    DeviceEventEntity(
                        eventType = DeviceEventReceiver.EVENT_NOTIFICATION,
                        packageName = notification.packageName,
                    ),
                )
            }
        }
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val cn = ComponentName(context, HabitNotificationListener::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(cn.flattenToString())
        }
    }
}
