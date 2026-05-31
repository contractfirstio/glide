package glide.calendar

import kotlinx.serialization.Serializable

@Serializable
data class CalendarSyncPayload(
    val events: List<CalendarSyncEvent>,
)

@Serializable
data class CalendarSyncEvent(
    val uid: String,
    val title: String,
    val start: String,
    val end: String,
    val location: String = "",
    val notes: String = "",
)

sealed interface CalendarSyncResult {
    data class Success(val message: String) : CalendarSyncResult
    data class Failure(val message: String) : CalendarSyncResult
}
