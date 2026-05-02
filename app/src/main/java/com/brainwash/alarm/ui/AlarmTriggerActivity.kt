package com.brainwash.alarm.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.brainwash.alarm.R
import com.brainwash.alarm.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AlarmTriggerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_trigger)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Fetch the current playing video from YoutubeBrainwash and open it
        CoroutineScope(Dispatchers.IO).launch {
            val videoUrl = fetchNowPlayingUrl()
            if (videoUrl != null) {
                runOnUiThread {
                    findViewById<TextView>(R.id.trigger_title).text = "Opening Morning Feed..."
                }
                // Small delay so the user sees the alarm screen
                Thread.sleep(2000)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                intent.setPackage("com.google.android.youtube")
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btn_dismiss).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_snooze).setOnClickListener {
            // TODO: reschedule alarm for snooze duration
            finish()
        }
    }

    private fun fetchNowPlayingUrl(): String? {
        val settings = Settings(this)
        val baseUrl = settings.brainwashBaseUrl
        if (baseUrl.isBlank()) return null

        return try {
            val url = URL("$baseUrl/api/feed/now")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val body = reader.readText()
                reader.close()
                conn.disconnect()
                // Parse the youtube_url from the response
                val regex = "\"youtube_url\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                regex.find(body)?.groupValues?.get(1)
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
