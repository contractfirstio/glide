package glide.calendar

import glide.calendar.google.GoogleCalendarApiClient
import glide.calendar.google.GoogleCalendarConfig
import glide.calendar.google.GoogleCalendarSyncDebug
import glide.calendar.google.GoogleOAuthClient
import glide.calendar.google.GoogleTokenStore
import glide.data.AppSettingsStore
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

actual object GoogleCalendarSyncService {
    private val oauthClient = GoogleOAuthClient()
    private val apiClient = GoogleCalendarApiClient(oauthClient = oauthClient)
    private val syncScheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "glide-google-calendar-sync").apply { isDaemon = true }
    }

    @Volatile
    private var pendingSync: java.util.concurrent.ScheduledFuture<*>? = null

    actual fun isConnected(): Boolean = GoogleTokenStore.isConnected()

    actual fun connectionLabel(): String {
        val email = AppSettingsStore.settings.googleCalendarAccountEmail
        return when {
            !isConnected() -> "Not connected"
            email.isNotBlank() -> "Connected as $email"
            else -> "Connected"
        }
    }

    actual fun connect(): CalendarSyncResult {
        GoogleCalendarSyncDebug.log("connect.start")
        GoogleCalendarConfig.logOAuthConfig()
        return runCatching {
            val tokens = oauthClient.authorize().getOrThrow()
            GoogleTokenStore.save(tokens)
            AppSettingsStore.saveGoogleCalendarConnection(
                accountEmail = tokens.accountEmail,
                calendarId = AppSettingsStore.settings.googleCalendarId,
            )
            GoogleCalendarSyncDebug.log("connect.success", "email=${tokens.accountEmail}")
            AppSettingsStore.saveGoogleCalendarSyncEnabled(true)
            val syncResult = syncNow()
            val connectedMessage = if (tokens.accountEmail.isBlank()) {
                "Google Calendar connected."
            } else {
                "Google Calendar connected as ${tokens.accountEmail}."
            }
            when (syncResult) {
                is CalendarSyncResult.Success -> CalendarSyncResult.Success("$connectedMessage ${syncResult.message}")
                is CalendarSyncResult.Failure -> CalendarSyncResult.Success(
                    "$connectedMessage Sync will run when you add classes.",
                )
            }
        }.getOrElse { error ->
            val message = error.message ?: "Google Calendar connection failed."
            GoogleCalendarSyncDebug.log("connect.failed", message)
            AppSettingsStore.saveGoogleCalendarSyncStatus(
                message = message,
                syncedAtMillis = AppSettingsStore.settings.googleCalendarSyncLastSyncMillis,
            )
            CalendarSyncResult.Failure(message)
        }
    }

    actual fun disconnect(): CalendarSyncResult {
        GoogleCalendarSyncDebug.log("disconnect.start")
        val clearResult = clearSyncedEvents()
        GoogleTokenStore.clear()
        AppSettingsStore.clearGoogleCalendarConnection()
        GoogleCalendarSyncDebug.log("disconnect.success")
        return clearResult
    }

    actual fun setEnabled(enabled: Boolean): CalendarSyncResult {
        GoogleCalendarSyncDebug.log("setEnabled", "enabled=$enabled")
        if (!enabled) {
            AppSettingsStore.saveGoogleCalendarSyncEnabled(false)
            return clearSyncedEvents()
        }
        if (!isConnected()) {
            return CalendarSyncResult.Failure("Connect Google Calendar first.")
        }
        AppSettingsStore.saveGoogleCalendarSyncEnabled(true)
        return syncNow()
    }

    actual fun syncNow(): CalendarSyncResult {
        GoogleCalendarSyncDebug.log("syncNow.start")
        if (!isConnected()) {
            return CalendarSyncResult.Failure("Google Calendar is not connected.")
        }
        return runCatching {
        val payload = buildCalendarSyncPayload()
        GoogleCalendarSyncDebug.log("syncNow.payload", "events=${payload.events.size}")
        payload.events.firstOrNull()?.let { sample ->
            GoogleCalendarSyncDebug.log(
                "syncNow.sample",
                "title=${sample.title} notesLines=${sample.notes.lines().size}",
            )
        }
            val stats = runBlocking { apiClient.syncEvents(payload.events).getOrThrow() }
            val message = buildString {
                append("Synced ${payload.events.size} class sessions to Google Calendar")
                if (stats.created + stats.updated + stats.deleted > 0) {
                    append(" (")
                    append(buildList {
                        if (stats.created > 0) add("${stats.created} added")
                        if (stats.updated > 0) add("${stats.updated} updated")
                        if (stats.deleted > 0) add("${stats.deleted} removed")
                    }.joinToString(", "))
                    append(')')
                }
                append('.')
            }
            AppSettingsStore.saveGoogleCalendarSyncStatus(message)
            GoogleCalendarSyncDebug.log("syncNow.success", message)
            CalendarSyncResult.Success(message)
        }.getOrElse { error ->
            val message = error.message ?: "Google Calendar sync failed."
            GoogleCalendarSyncDebug.log("syncNow.failed", message)
            CalendarSyncResult.Failure(message)
        }
    }

    actual fun scheduleSyncIfEnabled() {
        if (!AppSettingsStore.settings.googleCalendarSyncEnabled) {
            GoogleCalendarSyncDebug.log("scheduleSyncIfEnabled.skipped", "sync disabled")
            return
        }
        if (!isConnected()) {
            GoogleCalendarSyncDebug.log("scheduleSyncIfEnabled.skipped", "not connected")
            return
        }
        GoogleCalendarSyncDebug.log("scheduleSyncIfEnabled", "debounce=2s")
        synchronized(syncScheduler) {
            pendingSync?.cancel(false)
            pendingSync = syncScheduler.schedule(
                { syncNow() },
                2,
                TimeUnit.SECONDS,
            )
        }
    }

    private fun clearSyncedEvents(): CalendarSyncResult {
        if (!isConnected()) {
            return CalendarSyncResult.Success("Google Calendar disconnected.")
        }
        return runCatching {
            val deleted = runBlocking { apiClient.clearManagedEvents().getOrThrow() }
            val message = if (deleted > 0) {
                "Removed $deleted Glide events from Google Calendar."
            } else {
                "Removed Glide events from Google Calendar."
            }
            AppSettingsStore.saveGoogleCalendarSyncStatus(message)
            GoogleCalendarSyncDebug.log("clearSyncedEvents.success", "deleted=$deleted")
            CalendarSyncResult.Success(message)
        }.getOrElse { error ->
            val message = error.message ?: "Could not clear Google Calendar events."
            GoogleCalendarSyncDebug.log("clearSyncedEvents.failed", message)
            CalendarSyncResult.Failure(message)
        }
    }
}
