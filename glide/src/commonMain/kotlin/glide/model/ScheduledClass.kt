package glide.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID

enum class ClassScheduleKind(val label: String) {
    RECURRING("Weekly (recurring)"),
    SINGLE_DAY("Single day"),
}

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
    val customerGroupIds: List<String> = emptyList(),
    val locationId: String? = null,
    val dayOfWeek: DayOfWeek,
    /** ISO yyyy-MM-dd when this class runs once; null means weekly on [dayOfWeek]. */
    val singleDate: String? = null,
    /** 24-hour time, e.g. 09:00 */
    val startTime: String,
    val endTime: String,
    val notes: String = "",
    /** ARGB color for the term calendar; null uses a stable default from the class id. */
    val calendarColorArgb: Int? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

fun ScheduledClass.usesLocation(locationId: String): Boolean = this.locationId == locationId

fun ScheduledClass.isSingleDay(): Boolean = !singleDate.isNullOrBlank()

fun ScheduledClass.scheduleKind(): ClassScheduleKind =
    if (isSingleDay()) ClassScheduleKind.SINGLE_DAY else ClassScheduleKind.RECURRING

fun ScheduledClass.scheduleLine(): String {
    val time = "$startTime–$endTime"
    return if (isSingleDay()) {
        "${formatScheduleIsoDate(singleDate!!)} · $time"
    } else {
        "${dayOfWeek.label} · $time"
    }
}

private val ScheduleDateDisplayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

fun formatScheduleIsoDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        ScheduleDateDisplayFormatter.format(LocalDate.parse(isoDate))
    } catch (_: DateTimeParseException) {
        isoDate
    }
}

fun parseScheduleIsoDate(isoDate: String): LocalDate? {
    if (isoDate.isBlank()) return null
    return try {
        LocalDate.parse(isoDate)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun DayOfWeek.toJavaDayOfWeek(): java.time.DayOfWeek = when (this) {
    DayOfWeek.MONDAY -> java.time.DayOfWeek.MONDAY
    DayOfWeek.TUESDAY -> java.time.DayOfWeek.TUESDAY
    DayOfWeek.WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY
    DayOfWeek.THURSDAY -> java.time.DayOfWeek.THURSDAY
    DayOfWeek.FRIDAY -> java.time.DayOfWeek.FRIDAY
    DayOfWeek.SATURDAY -> java.time.DayOfWeek.SATURDAY
    DayOfWeek.SUNDAY -> java.time.DayOfWeek.SUNDAY
}

fun java.time.DayOfWeek.toModelDayOfWeek(): DayOfWeek = when (this) {
    java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
    java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
    java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
    java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
    java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
    java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
    java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
}

fun ScheduledClass.occursOn(date: LocalDate): Boolean {
    if (isSingleDay()) {
        return parseScheduleIsoDate(singleDate!!) == date
    }
    return dayOfWeek.toJavaDayOfWeek() == date.dayOfWeek
}

/** True when [sessionDate] is in the past, or it is today and [endTime] has been reached. */
fun ScheduledClass.sessionHasEndedForAttendance(
    sessionDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now(),
): Boolean {
    if (sessionDate.isAfter(today)) return false
    if (sessionDate.isBefore(today)) return true
    if (!isValidTime24h(endTime)) return false
    return compareTime24h(endTime, formatTime24h(now)) <= 0
}

/** Attendance can only be recorded after the class has finished. */
fun ScheduledClass.canTakeAttendance(
    sessionDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now(),
): Boolean = sessionHasEndedForAttendance(sessionDate, today, now)

private fun formatTime24h(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

fun compareScheduledClasses(): Comparator<ScheduledClass> = compareBy(
    { !it.isSingleDay() },
    { it.singleDate ?: "" },
    { it.dayOfWeek.sortOrder },
    { it.startTime },
    { it.name.lowercase(Locale.UK) },
)

fun ScheduledClass.timeRangeLine(): String = "$startTime–$endTime"

fun ScheduledClass.spansTerm(termId: String): Boolean = termId in termIds

fun ScheduledClass.hasCustomerGroup(groupId: String): Boolean = groupId in customerGroupIds

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
