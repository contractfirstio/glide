package glide.calendar.google

import glide.calendar.CalendarSyncEvent
import glide.data.AppSettingsStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.time.ZoneId
import kotlinx.serialization.json.Json

internal class GoogleCalendarApiClient(
    private val oauthClient: GoogleOAuthClient = GoogleOAuthClient(),
    private val httpClient: HttpClient = defaultHttpClient(),
) {
    suspend fun ensureGlideCalendarId(): Result<String> {
        AppSettingsStore.settings.googleCalendarId.takeIf { it.isNotBlank() }?.let {
            return Result.success(it)
        }
        return findGlideCalendarId()
            .onSuccess { calendarId ->
                AppSettingsStore.saveGoogleCalendarConnection(
                    accountEmail = AppSettingsStore.settings.googleCalendarAccountEmail,
                    calendarId = calendarId,
                )
            }
            .recoverCatching { createGlideCalendar() }
    }

    suspend fun syncEvents(events: List<CalendarSyncEvent>): Result<GoogleSyncStats> = runCatching {
        val calendarId = ensureGlideCalendarId().getOrThrow()
        val tokens = loadAuthorizedTokens().getOrThrow()
        val existing = listManagedEvents(calendarId, tokens.accessToken)
        val desiredUids = events.map { it.uid }.toSet()

        var deleted = 0
        for ((uid, googleEventId) in existing) {
            if (uid !in desiredUids) {
                deleteEvent(calendarId, googleEventId, tokens.accessToken)
                deleted += 1
            }
        }

        val existingByUid = existing.toMap()
        var created = 0
        var updated = 0
        for (event in events) {
            val googleEvent = GoogleCalendarEventMapper.toGoogleEvent(event)
            val existingId = existingByUid[event.uid]
            if (existingId == null) {
                insertEvent(calendarId, googleEvent, tokens.accessToken)
                created += 1
            } else {
                val existingEvent = fetchEvent(calendarId, existingId, tokens.accessToken)
                if (existingEvent == null || !GoogleCalendarEventMapper.matches(event, existingEvent)) {
                    updateEvent(calendarId, existingId, googleEvent, tokens.accessToken)
                    updated += 1
                }
            }
        }
        GoogleSyncStats(created = created, updated = updated, deleted = deleted)
    }

    suspend fun clearManagedEvents(): Result<Int> = runCatching {
        val calendarId = AppSettingsStore.settings.googleCalendarId
        if (calendarId.isBlank()) return@runCatching 0
        val tokens = loadAuthorizedTokens().getOrThrow()
        val existing = listManagedEvents(calendarId, tokens.accessToken)
        for ((_, googleEventId) in existing) {
            deleteEvent(calendarId, googleEventId, tokens.accessToken)
        }
        existing.size
    }

    private suspend fun findGlideCalendarId(): Result<String> {
        val tokens = loadAuthorizedTokens().getOrThrow()
        val response = authorizedGet(
            url = "https://www.googleapis.com/calendar/v3/users/me/calendarList",
            accessToken = tokens.accessToken,
        )
        val body = response.body<GoogleCalendarListResponse>()
        val match = body.items.firstOrNull { it.summary == GoogleCalendarConfig.GLIDE_CALENDAR_TITLE }
        return if (match?.id.isNullOrBlank()) {
            Result.failure(IllegalStateException("Glide calendar not found."))
        } else {
            Result.success(match.id)
        }
    }

    private suspend fun createGlideCalendar(): String {
        val tokens = loadAuthorizedTokens().getOrThrow()
        val response = authorizedPost(
            url = "https://www.googleapis.com/calendar/v3/calendars",
            accessToken = tokens.accessToken,
            body = GoogleCalendarResource(
                summary = GoogleCalendarConfig.GLIDE_CALENDAR_TITLE,
                timeZone = ZoneId.systemDefault().id,
            ),
        )
        val created = response.body<GoogleCalendarResource>()
        val calendarId = created.id ?: error(apiErrorMessage(response.bodyAsText(), "Could not create Glide calendar."))
        AppSettingsStore.saveGoogleCalendarConnection(
            accountEmail = tokens.accountEmail.ifBlank { AppSettingsStore.settings.googleCalendarAccountEmail },
            calendarId = calendarId,
        )
        return calendarId
    }

    private suspend fun listManagedEvents(
        calendarId: String,
        accessToken: String,
    ): Map<String, String> {
        val managed = linkedMapOf<String, String>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("https://www.googleapis.com/calendar/v3/calendars/")
                append(calendarId)
                append("/events?singleEvents=true&maxResults=2500")
                pageToken?.let {
                    append("&pageToken=")
                    append(it)
                }
            }
            val response = authorizedGet(url = url, accessToken = accessToken)
            val page = response.body<GoogleEventsListResponse>()
            for (event in page.items) {
                val uid = GoogleCalendarEventMapper.glideUid(event) ?: continue
                val eventId = event.id ?: continue
                managed[uid] = eventId
            }
            pageToken = page.nextPageToken
        } while (!pageToken.isNullOrBlank())
        return managed
    }

    private suspend fun fetchEvent(
        calendarId: String,
        eventId: String,
        accessToken: String,
    ): GoogleCalendarEvent? {
        val response = authorizedGet(
            url = "https://www.googleapis.com/calendar/v3/calendars/$calendarId/events/$eventId",
            accessToken = accessToken,
        )
        return if (response.status.isSuccess()) {
            response.body()
        } else {
            null
        }
    }

    private suspend fun insertEvent(
        calendarId: String,
        event: GoogleCalendarEvent,
        accessToken: String,
    ) {
        val response = authorizedPost(
            url = "https://www.googleapis.com/calendar/v3/calendars/$calendarId/events",
            accessToken = accessToken,
            body = event,
        )
        if (!response.status.isSuccess()) {
            error(apiErrorMessage(response.bodyAsText(), "Could not create calendar event."))
        }
    }

    private suspend fun updateEvent(
        calendarId: String,
        eventId: String,
        event: GoogleCalendarEvent,
        accessToken: String,
    ) {
        val response = httpClient.put("https://www.googleapis.com/calendar/v3/calendars/$calendarId/events/$eventId") {
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(event)
        }
        if (!response.status.isSuccess()) {
            error(apiErrorMessage(response.bodyAsText(), "Could not update calendar event."))
        }
    }

    private suspend fun deleteEvent(
        calendarId: String,
        eventId: String,
        accessToken: String,
    ) {
        val response = httpClient.delete(
            "https://www.googleapis.com/calendar/v3/calendars/$calendarId/events/$eventId",
        ) {
            header("Authorization", "Bearer $accessToken")
        }
        if (!response.status.isSuccess() && response.status.value != 404) {
            error(apiErrorMessage(response.bodyAsText(), "Could not delete calendar event."))
        }
    }

    private suspend fun loadAuthorizedTokens(): Result<GoogleOAuthTokens> {
        val stored = GoogleTokenStore.load()
            ?: return Result.failure(IllegalStateException("Google Calendar is not connected."))
        return oauthClient.ensureValidAccessToken(stored)
    }

    private suspend fun authorizedGet(url: String, accessToken: String) =
        httpClient.get(url) {
            header("Authorization", "Bearer $accessToken")
        }.also { response ->
            if (!response.status.isSuccess()) {
                error(apiErrorMessage(response.bodyAsText(), "Google Calendar request failed."))
            }
        }

    private suspend fun authorizedPost(
        url: String,
        accessToken: String,
        body: Any,
    ) = httpClient.post(url) {
        header("Authorization", "Bearer $accessToken")
        contentType(ContentType.Application.Json)
        setBody(body)
    }.also { response ->
        if (!response.status.isSuccess()) {
            error(apiErrorMessage(response.bodyAsText(), "Google Calendar request failed."))
        }
    }

    private fun apiErrorMessage(rawBody: String, fallback: String): String =
        runCatching {
            defaultJson.decodeFromString<GoogleApiErrorBody>(rawBody).error?.message
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback

    companion object {
        private val defaultJson = Json { ignoreUnknownKeys = true }

        private fun defaultHttpClient(): HttpClient =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(defaultJson)
                }
            }
    }
}
