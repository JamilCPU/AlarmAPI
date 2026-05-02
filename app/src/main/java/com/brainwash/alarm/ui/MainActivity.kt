package com.brainwash.alarm.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.brainwash.alarm.AlarmApp
import com.brainwash.alarm.R
import com.brainwash.alarm.data.Alarm
import com.brainwash.alarm.data.AlarmRepository
import com.brainwash.alarm.service.AlarmScheduler
import com.brainwash.alarm.service.ApiServerService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var repo: AlarmRepository
    private lateinit var scheduler: AlarmScheduler
    private lateinit var adapter: AlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = (application as AlarmApp).database
        repo = AlarmRepository(db.alarmDao())
        scheduler = AlarmScheduler(this)

        // Start API server
        startForegroundService(Intent(this, ApiServerService::class.java))

        // Setup RecyclerView
        adapter = AlarmAdapter(
            onToggle = { alarm -> toggleAlarm(alarm) },
            onDelete = { alarm -> deleteAlarm(alarm) }
        )
        val recyclerView = findViewById<RecyclerView>(R.id.alarm_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Observe alarms
        lifecycleScope.launch {
            repo.allAlarms.collectLatest { alarms ->
                adapter.submitList(alarms)
                findViewById<TextView>(R.id.empty_text).visibility =
                    if (alarms.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // FAB to add alarm
        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            lifecycleScope.launch {
                val alarm = Alarm(hour = hour, minute = minute)
                val id = repo.insert(alarm)
                scheduler.schedule(alarm.copy(id = id))
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun toggleAlarm(alarm: Alarm) {
        lifecycleScope.launch {
            val newEnabled = !alarm.enabled
            repo.setEnabled(alarm.id, newEnabled)
            if (newEnabled) scheduler.schedule(alarm.copy(enabled = true))
            else scheduler.cancel(alarm)
        }
    }

    private fun deleteAlarm(alarm: Alarm) {
        lifecycleScope.launch {
            scheduler.cancel(alarm)
            repo.delete(alarm)
        }
    }
}

// RecyclerView Adapter
class AlarmAdapter(
    private val onToggle: (Alarm) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    private var alarms: List<Alarm> = emptyList()

    fun submitList(newAlarms: List<Alarm>) {
        alarms = newAlarms
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(alarms[position])
    }

    override fun getItemCount(): Int = alarms.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val timeText: TextView = view.findViewById(R.id.alarm_time)
        private val labelText: TextView = view.findViewById(R.id.alarm_label)
        private val enableSwitch: Switch = view.findViewById(R.id.alarm_switch)
        private val deleteBtn: ImageButton = view.findViewById(R.id.alarm_delete)

        fun bind(alarm: Alarm) {
            timeText.text = alarm.timeFormatted
            labelText.text = alarm.label.ifEmpty { if (alarm.brainwashEnabled) "Morning Feed" else "Alarm" }
            enableSwitch.isChecked = alarm.enabled
            enableSwitch.setOnClickListener { onToggle(alarm) }
            deleteBtn.setOnClickListener { onDelete(alarm) }
        }
    }
}
