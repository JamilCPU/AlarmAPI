package com.brainwash.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.brainwash.alarm.data.Alarm
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) return

        val triggerTime = getNextTriggerTime(alarm)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.brainwash.alarm.ALARM_TRIGGER"
            putExtra("alarm_id", alarm.id)
            putExtra("brainwash_enabled", alarm.brainwashEnabled)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
            pendingIntent
        )
        Log.i("AlarmScheduler", "Scheduled alarm ${alarm.id} for ${alarm.timeFormatted} at $triggerTime")
    }

    fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.brainwash.alarm.ALARM_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.i("AlarmScheduler", "Cancelled alarm ${alarm.id}")
    }

    companion object {
        fun getNextTriggerTime(alarm: Alarm): Long {
            val now = Calendar.getInstance()
            val trigger = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If alarm time is already past today, schedule for tomorrow
            if (trigger.before(now) || trigger == now) {
                trigger.add(Calendar.DAY_OF_YEAR, 1)
            }

            // If repeating, find the next matching day
            if (alarm.isRepeating()) {
                for (i in 0..7) {
                    val dayOfWeek = trigger.get(Calendar.DAY_OF_WEEK)
                    // Convert Calendar day (Sun=1..Sat=7) to our bitmask (Mon=0..Sun=6)
                    val ourDay = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                    if (alarm.repeatsOn(ourDay)) break
                    trigger.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            return trigger.timeInMillis
        }
    }
}
