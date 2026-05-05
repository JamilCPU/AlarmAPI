package com.brainwash.alarm.api

import com.brainwash.alarm.data.*
import com.brainwash.alarm.service.AlarmScheduler
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

fun Application.createApiServer(
    repo: AlarmRepository,
    scheduler: AlarmScheduler,
    settings: Settings
) {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true })
    }

    routing {
        // GET /api/health
        get("/api/health") {
            val alarms = repo.getAllAlarmsList()
            call.respond(StatusResponse(
                status = "ok",
                alarmsCount = alarms.size
            ))
        }

        // GET /api/alarms — list all alarms
        get("/api/alarms") {
            val alarms = repo.getAllAlarmsList()
            call.respond(AlarmResponse(alarms = alarms))
        }

        // GET /api/alarms/next — get next upcoming alarm
        get("/api/alarms/next") {
            val enabled = repo.getEnabledAlarms()
            if (enabled.isEmpty()) {
                call.respond(NextAlarmResponse(alarm = null, triggerTimeMillis = null, triggerTimeIso = null))
                return@get
            }

            // Find the alarm with the soonest trigger time
            val now = System.currentTimeMillis()
            var nextAlarm: Alarm? = null
            var nextTrigger: Long = Long.MAX_VALUE

            for (alarm in enabled) {
                val trigger = AlarmScheduler.getNextTriggerTime(alarm)
                if (trigger < nextTrigger) {
                    nextTrigger = trigger
                    nextAlarm = alarm
                }
            }

            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date(nextTrigger))
            call.respond(NextAlarmResponse(
                alarm = nextAlarm,
                triggerTimeMillis = nextTrigger,
                triggerTimeIso = iso
            ))
        }

        // GET /api/alarms/{id}
        get("/api/alarms/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val alarm = repo.getAlarmById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Alarm not found"))
            call.respond(alarm)
        }

        // POST /api/alarms — create a new alarm
        // Body: {"hour": 7, "minute": 30, "label": "Wake up", "enabled": true, "brainwashEnabled": true}
        post("/api/alarms") {
            val alarm = call.receive<Alarm>()
            val id = repo.insert(alarm)
            val saved = alarm.copy(id = id)
            if (saved.enabled) scheduler.schedule(saved)
            call.respond(HttpStatusCode.Created, saved)
        }

        // PUT /api/alarms/{id} — update an alarm
        put("/api/alarms/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val existing = repo.getAlarmById(id)
                ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Alarm not found"))

            val updated = call.receive<Alarm>().copy(id = id)
            repo.update(updated)

            // Reschedule or cancel
            scheduler.cancel(existing)
            if (updated.enabled) scheduler.schedule(updated)

            call.respond(updated)
        }

        // DELETE /api/alarms/{id}
        delete("/api/alarms/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val alarm = repo.getAlarmById(id)
            if (alarm != null) {
                scheduler.cancel(alarm)
                repo.deleteById(id)
            }
            call.respond(HttpStatusCode.NoContent)
        }

        // POST /api/alarms/{id}/toggle — enable/disable
        post("/api/alarms/{id}/toggle") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val alarm = repo.getAlarmById(id)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Alarm not found"))

            val newEnabled = !alarm.enabled
            repo.setEnabled(id, newEnabled)
            val updated = alarm.copy(enabled = newEnabled)

            if (newEnabled) scheduler.schedule(updated)
            else scheduler.cancel(updated)

            call.respond(updated)
        }

        // GET /api/settings
        get("/api/settings") {
            call.respond(mapOf(
                "brainwash_host" to settings.brainwashHost,
                "brainwash_port" to settings.brainwashPort.toString(),
                "api_server_port" to settings.apiServerPort.toString(),
                "snooze_duration_minutes" to settings.snoozeDurationMinutes.toString()
            ))
        }
    }
}
