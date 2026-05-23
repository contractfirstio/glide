package glide.model

import java.util.UUID

enum class DayOfWeek(val label: String, val sortOrder: Int) {
    MONDAY("Monday", 1),
    TUESDAY("Tuesday", 2),
    WEDNESDAY("Wednesday", 3),
    THURSDAY("Thursday", 4),
    FRIDAY("Friday", 5),
    SATURDAY("Saturday", 6),
    SUNDAY("Sunday", 7),
}

data class ScheduledClass(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val termIds: List<String> = emptyList(),
    val locationId: String? = null,
    val dayOfWeek: DayOfWeek,
    /** 24-hour time, e.g. 09:00 */
    val startTime: String,
    val endTime: String,
    val notes: String = "",
    /** ARGB color for the term calendar; null uses a stable default from the class id. */
    val calendarColorArgb: Int? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

fun ScheduledClass.usesLocation(locationId: String): Boolean = this.locationId == locationId

fun ScheduledClass.scheduleLine(): String =
    "${dayOfWeek.label} · $startTime–$endTime"

fun ScheduledClass.spansTerm(termId: String): Boolean = termId in termIds

fun isValidTime24h(value: String): Boolean {
    val parts = value.split(":")
    if (parts.size != 2) return false
    val hour = parts[0].toIntOrNull() ?: return false
    val minute = parts[1].toIntOrNull() ?: return false
    return hour in 0..23 && minute in 0..59
}

fun compareTime24h(start: String, end: String): Int {
    val startParts = start.split(":")
    val endParts = end.split(":")
    val startMins = startParts[0].toInt() * 60 + startParts[1].toInt()
    val endMins = endParts[0].toInt() * 60 + endParts[1].toInt()
    return startMins.compareTo(endMins)
}
