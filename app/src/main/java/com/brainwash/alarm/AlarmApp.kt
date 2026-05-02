package com.brainwash.alarm

import android.app.Application
import com.brainwash.alarm.data.AlarmDatabase

class AlarmApp : Application() {
    val database: AlarmDatabase by lazy { AlarmDatabase.getDatabase(this) }
}
