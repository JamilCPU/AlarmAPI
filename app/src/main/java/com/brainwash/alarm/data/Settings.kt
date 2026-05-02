package com.brainwash.alarm.data

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("alarm_api_settings", Context.MODE_PRIVATE)

    var brainwashHost: String
        get() = prefs.getString("brainwash_host", "") ?: ""
        set(value) = prefs.edit().putString("brainwash_host", value).apply()

    var brainwashPort: Int
        get() = prefs.getInt("brainwash_port", 5000)
        set(value) = prefs.edit().putInt("brainwash_port", value).apply()

    var apiServerPort: Int
        get() = prefs.getInt("api_server_port", 8080)
        set(value) = prefs.edit().putInt("api_server_port", value).apply()

    var snoozeDurationMinutes: Int
        get() = prefs.getInt("snooze_duration", 5)
        set(value) = prefs.edit().putInt("snooze_duration", value).apply()

    val brainwashBaseUrl: String
        get() = if (brainwashHost.isNotBlank()) "http://$brainwashHost:$brainwashPort" else ""
}
