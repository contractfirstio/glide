package glide.calendar.google

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GoogleCalendarListResponse(
    val items: List<GoogleCalendarListEntry> = emptyList(),
)

@Serializable
internal data class GoogleCalendarListEntry(
    val id: String = "",
    val summary: String = "",
)

@Serializable
internal data class GoogleCalendarResource(
    val id: String? = null,
    val summary: String = "",
    @SerialName("timeZone") val timeZone: String = "",
)

@Serializable
internal data class GoogleEventDateTime(
    @SerialName("dateTime") val dateTime: String = "",
    @SerialName("timeZone") val timeZone: String = "",
)

@Serializable
internal data class GoogleEventExtendedProperties(
    val `private`: Map<String, String> = emptyMap(),
)

@Serializable
internal data class GoogleCalendarEvent(
    val id: String? = null,
    val summary: String = "",
    val description: String = "",
    val location: String = "",
    val start: GoogleEventDateTime? = null,
    val end: GoogleEventDateTime? = null,
    @SerialName("extendedProperties") val extendedProperties: GoogleEventExtendedProperties? = null,
)

@Serializable
internal data class GoogleEventsListResponse(
    val items: List<GoogleCalendarEvent> = emptyList(),
    @SerialName("nextPageToken") val nextPageToken: String? = null,
)

@Serializable
internal data class GoogleApiErrorBody(
    val error: GoogleApiErrorDetail? = null,
)

@Serializable
internal data class GoogleApiErrorDetail(
    val message: String = "",
)

internal data class GoogleSyncStats(
    val created: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0,
)
