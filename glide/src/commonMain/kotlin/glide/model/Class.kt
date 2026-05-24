package glide.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

enum class ClassScheduleKind(val label: String) {
    RECURRING("Recurring weekly"),
    WEEKLY("Weekly"),
    SINGLE_DAY("Single day"),
}

enum class DayOfWeek(val label: String, val shortLabel: String, val sortOrder: Int) {
    MONDAY("Monday", "Mon", 1),
    TUESDAY("Tuesday", "Tue", 2),
    WEDNESDAY("Wednesday", "Wed", 3),
    THURSDAY("Thursday", "Thu", 4),
    FRIDAY("Friday", "Fri", 5),
    SATURDAY("Saturday", "Sat", 6),
    SUNDAY("Sunday", "Sun", 7),
}

data class Class(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val termIds: List<String> = emptyList(),
    val soldPlanIds: List<String> = emptyList(),
    val locationId: String? = null,
    val dayOfWeek: DayOfWeek,
    /** ISO yyyy-MM-dd when this class runs once; null for recurring or weekly classes. */
    val singleDate: String? = null,
    /** ISO yyyy-MM-dd for any day in a one-off week; set with [weeklyDays] for weekly classes. */
    val weekOfDate: String? = null,
    /** Days within [weekOfDate]'s week when this class runs (non-recurring weekly). */
    val weeklyDays: List<DayOfWeek> = emptyList(),
    /** 24-hour time, e.g. 09:00 */
    val startTime: String,
    val endTime: String,
    val notes: String = "",
    /** ARGB color for the term calendar; null uses a stable default from the class id. */
    val calendarColorArgb: Int? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

fun Class.usesLocation(locationId: String): Boolean = this.locationId == locationId

fun Class.isSingleDay(): Boolean = scheduleKind() == ClassScheduleKind.SINGLE_DAY

fun Class.isWeekly(): Boolean = scheduleKind() == ClassScheduleKind.WEEKLY

fun Class.scheduleKind(): ClassScheduleKind = when {
    !singleDate.isNullOrBlank() -> ClassScheduleKind.SINGLE_DAY
    !weekOfDate.isNullOrBlank() && weeklyDays.isNotEmpty() -> ClassScheduleKind.WEEKLY
    else -> ClassScheduleKind.RECURRING
}

fun weekDateRangeFromIsoDate(isoDate: String): ClosedRange<LocalDate>? {
    val anchor = parseScheduleIsoDate(isoDate) ?: return null
    val start = anchor.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    return start..start.plusDays(6)
}

fun formatWeeklyDaysLabel(days: List<DayOfWeek>): String =
    days.sortedBy { it.sortOrder }.joinToString(", ") { it.shortLabel }

fun Class.scheduleLine(): String {
    val time = "$startTime–$endTime"
    return when (scheduleKind()) {
        ClassScheduleKind.SINGLE_DAY -> "${formatScheduleIsoDate(singleDate!!)} · $time"
        ClassScheduleKind.WEEKLY -> {
            val daysLabel = formatWeeklyDaysLabel(weeklyDays)
            val weekLabel = weekDateRangeFromIsoDate(weekOfDate!!)?.let { range ->
                "${formatScheduleIsoDate(range.start.toString())} – " +
                    formatScheduleIsoDate(range.endInclusive.toString())
            } ?: formatScheduleIsoDate(weekOfDate!!)
            "$daysLabel · $weekLabel · $time"
        }
        ClassScheduleKind.RECURRING -> "${dayOfWeek.label} · $time"
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

private val InvoiceClassSessionDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.UK)

fun formatInvoiceClassSessionDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        InvoiceClassSessionDateFormatter.format(LocalDate.parse(isoDate))
    } catch (_: DateTimeParseException) {
        isoDate
    }
}

fun Class.timeRangeLabel(): String = "$startTime–$endTime"

fun formatInvoiceClassSessionLabel(isoDate: String, cls: Class): String {
    val dateLabel = formatInvoiceClassSessionDate(isoDate)
    if (dateLabel.isBlank()) return ""
    return "${dateLabel} · ${cls.timeRangeLabel()}"
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

fun Class.occursOn(date: LocalDate): Boolean = when (scheduleKind()) {
    ClassScheduleKind.SINGLE_DAY -> parseScheduleIsoDate(singleDate!!) == date
    ClassScheduleKind.WEEKLY -> {
        val range = weekDateRangeFromIsoDate(weekOfDate!!)
        range != null && date in range && weeklyDays.any { it.toJavaDayOfWeek() == date.dayOfWeek }
    }
    ClassScheduleKind.RECURRING -> dayOfWeek.toJavaDayOfWeek() == date.dayOfWeek
}

/** True when [sessionDate] is in the past, or it is today and [endTime] has been reached. */
fun Class.sessionHasEndedForAttendance(
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
fun Class.canTakeAttendance(
    sessionDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now(),
): Boolean = sessionHasEndedForAttendance(sessionDate, today, now)

private fun formatTime24h(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

fun compareClasses(): Comparator<Class> = compareBy(
    { when (it.scheduleKind()) {
        ClassScheduleKind.RECURRING -> 0
        ClassScheduleKind.WEEKLY -> 1
        ClassScheduleKind.SINGLE_DAY -> 2
    } },
    { it.singleDate ?: it.weekOfDate ?: "" },
    { it.weeklyDays.minOfOrNull { day -> day.sortOrder } ?: it.dayOfWeek.sortOrder },
    { it.startTime },
    { it.name.lowercase(Locale.UK) },
)

fun Class.timeRangeLine(): String = "$startTime–$endTime"

fun Class.spansTerm(termId: String): Boolean = termId in termIds

fun Class.hasSoldPlan(soldPlanId: String): Boolean = soldPlanId in soldPlanIds

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
