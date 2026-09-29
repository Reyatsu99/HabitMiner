package com.habitminer.collection

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.habitminer.data.AppDatabase
import com.habitminer.data.DeviceEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HabitNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { notification ->
            if (notification.isOngoing) return
            serviceScope.launch {
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
            return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }
    }
}
