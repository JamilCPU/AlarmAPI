package com.brainwash.alarm.data

import androidx.room.*
import kotlinx.serialization.Serializable

@Entity(tableName = "alarms")
@Serializable
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,           // 0-23
    val minute: Int,         // 0-59
    val label: String = "",
    val enabled: Boolean = true,
    val repeatDays: Int = 0, // Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
    val vibrate: Boolean = true,
    val brainwashEnabled: Boolean = true, // Trigger YoutubeBrainwash on alarm
) {
    val timeFormatted: String get() = String.format("%02d:%02d", hour, minute)

    fun isRepeating(): Boolean = repeatDays != 0

    fun repeatsOn(dayOfWeek: Int): Boolean = (repeatDays and (1 shl (dayOfWeek - 1))) != 0
}

// API response wrapper
@Serializable
data class AlarmResponse(
    val alarms: List<Alarm>
)

@Serializable
data class NextAlarmResponse(
    val alarm: Alarm?,
    val triggerTimeMillis: Long?,
    val triggerTimeIso: String?
)

@Serializable
data class StatusResponse(
    val status: String,
    val version: String = "0.1.0",
    val serverPort: Int = 8080,
    val alarmsCount: Int = 0
)
