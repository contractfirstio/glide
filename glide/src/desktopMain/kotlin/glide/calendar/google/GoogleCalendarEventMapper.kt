package glide.calendar.google

import glide.calendar.CalendarSyncEvent
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object GoogleCalendarEventMapper {
    private val LocalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private val systemTimeZone: String
        get() = ZoneId.systemDefault().id

    fun toGoogleEvent(event: CalendarSyncEvent): GoogleCalendarEvent =
        GoogleCalendarEvent(
            summary = event.title,
            description = event.notes,
            location = event.location,
            start = GoogleEventDateTime(dateTime = event.start, timeZone = systemTimeZone),
            end = GoogleEventDateTime(dateTime = event.end, timeZone = systemTimeZone),
            extendedProperties = GoogleEventExtendedProperties(
                `private` = mapOf(GoogleCalendarConfig.GLIDE_UID_PROPERTY to event.uid),
            ),
        )

    fun glideUid(event: GoogleCalendarEvent): String? =
        event.extendedProperties?.`private`?.get(GoogleCalendarConfig.GLIDE_UID_PROPERTY)

    fun matches(event: CalendarSyncEvent, existing: GoogleCalendarEvent): Boolean {
        val googleEvent = toGoogleEvent(event)
        return existing.summary == googleEvent.summary &&
            normalizeMultiline(existing.description) == normalizeMultiline(googleEvent.description) &&
            existing.location == googleEvent.location &&
            normalizeDateTime(existing.start?.dateTime) == normalizeDateTime(googleEvent.start?.dateTime) &&
            normalizeDateTime(existing.end?.dateTime) == normalizeDateTime(googleEvent.end?.dateTime)
    }

    private fun normalizeMultiline(value: String): String =
        value.replace("\r\n", "\n").trim()

    private fun normalizeDateTime(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return runCatching {
            OffsetDateTime.parse(value).toLocalDateTime().format(LocalDateTimeFormatter)
        }.getOrElse {
            runCatching {
                LocalDateTime.parse(value, LocalDateTimeFormatter).format(LocalDateTimeFormatter)
            }.getOrElse {
                value.take(19)
            }
        }
    }
}
