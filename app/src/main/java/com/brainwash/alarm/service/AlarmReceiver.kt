package com.brainwash.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.brainwash.alarm.data.Settings
import com.brainwash.alarm.ui.AlarmTriggerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1)
        val brainwashEnabled = intent.getBooleanExtra("brainwash_enabled", true)

        Log.i("AlarmReceiver", "Alarm triggered: id=$alarmId, brainwash=$brainwashEnabled")

        // Launch the alarm trigger activity (shows on lock screen)
        val triggerIntent = Intent(context, AlarmTriggerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("alarm_id", alarmId)
        }
        context.startActivity(triggerIntent)

        // If brainwash integration is enabled, trigger playback on Pi-2
        if (brainwashEnabled) {
            CoroutineScope(Dispatchers.IO).launch {
                triggerBrainwashPlayback(context)
            }
        }
    }

    private suspend fun triggerBrainwashPlayback(context: Context) {
        val settings = Settings(context)
        val baseUrl = settings.brainwashBaseUrl
        if (baseUrl.isBlank()) {
            Log.w("AlarmReceiver", "Brainwash host not configured — skipping playback trigger")
            return
        }

        try {
            val url = java.net.URL("$baseUrl/api/feed/play")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.write("{}".toByteArray())

            val responseCode = conn.responseCode
            Log.i("AlarmReceiver", "Brainwash play triggered: HTTP $responseCode")
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to trigger brainwash playback", e)
        }
    }
}
