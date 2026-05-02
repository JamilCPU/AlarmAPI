package com.brainwash.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.brainwash.alarm.data.AlarmDatabase
import com.brainwash.alarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i("BootReceiver", "Device booted — rescheduling alarms")

        val db = AlarmDatabase.getDatabase(context)
        val repo = AlarmRepository(db.alarmDao())
        val scheduler = AlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            val alarms = repo.getEnabledAlarms()
            for (alarm in alarms) {
                scheduler.schedule(alarm)
            }
            Log.i("BootReceiver", "Rescheduled ${alarms.size} alarms")
        }

        // Also restart the API server service
        val serviceIntent = Intent(context, ApiServerService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
