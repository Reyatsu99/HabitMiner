package com.habitminer.collection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitminer.data.AppDatabase
import com.habitminer.data.DeviceEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceEventReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getDatabase(context).deviceEventDao().insert(
                    DeviceEventEntity(eventType = EVENT_UNLOCK),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EVENT_UNLOCK = "UNLOCK"
        const val EVENT_NOTIFICATION = "NOTIFICATION"
    }
}
