package com.brainwash.alarm.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val dao: AlarmDao) {
    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()

    suspend fun getAllAlarmsList(): List<Alarm> = dao.getAllAlarmsList()
    suspend fun getEnabledAlarms(): List<Alarm> = dao.getEnabledAlarms()
    suspend fun getAlarmById(id: Long): Alarm? = dao.getAlarmById(id)
    suspend fun insert(alarm: Alarm): Long = dao.insertAlarm(alarm)
    suspend fun update(alarm: Alarm) = dao.updateAlarm(alarm)
    suspend fun delete(alarm: Alarm) = dao.deleteAlarm(alarm)
    suspend fun deleteById(id: Long) = dao.deleteAlarmById(id)
    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setAlarmEnabled(id, enabled)
}
