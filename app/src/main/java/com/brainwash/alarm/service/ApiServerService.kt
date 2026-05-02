package com.brainwash.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.brainwash.alarm.api.createApiServer
import com.brainwash.alarm.data.AlarmDatabase
import com.brainwash.alarm.data.AlarmRepository
import com.brainwash.alarm.data.Settings
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ApiServerService : Service() {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var serverJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serverJob?.cancel()
        server?.stop(1000, 2000)
        Log.i(TAG, "API server stopped")
        super.onDestroy()
    }

    private fun startServer() {
        val db = AlarmDatabase.getDatabase(applicationContext)
        val repo = AlarmRepository(db.alarmDao())
        val settings = Settings(applicationContext)
        val scheduler = AlarmScheduler(applicationContext)
        val port = settings.apiServerPort

        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                server = embeddedServer(Netty, port = port) {
                    createApiServer(repo, scheduler, settings)
                }
                Log.i(TAG, "API server starting on port $port")
                server?.start(wait = true)
            } catch (e: Exception) {
                Log.e(TAG, "API server failed to start", e)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "API Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "AlarmAPI HTTP server status"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AlarmAPI")
            .setContentText("API server running on port ${Settings(this).apiServerPort}")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ApiServerService"
        private const val CHANNEL_ID = "api_server_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
