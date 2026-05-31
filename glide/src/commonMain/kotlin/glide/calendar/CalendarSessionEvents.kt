package glide.calendar

import glide.data.ClassStore
import glide.data.LocationStore
import glide.data.TermStore
import glide.model.Class
import glide.model.Term
import glide.model.dateRange
import glide.ui.scheduling.classesOnDate
import glide.ui.scheduling.rosterLinesForClass
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LocalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

fun calendarEventUid(classId: String, sessionDate: LocalDate): String =
    "$classId|$sessionDate"

fun buildCalendarSyncPayload(): CalendarSyncPayload =
    CalendarSyncPayload(events = buildCalendarSyncEvents())

internal fun buildCalendarSyncEvents(): List<CalendarSyncEvent> {
    val terms = TermStore.sortedChronologically()
    val classes = ClassStore.classes
    if (terms.isEmpty() || classes.isEmpty()) return emptyList()

    val events = linkedMapOf<String, CalendarSyncEvent>()
    for (term in terms) {
        val range = term.dateRange() ?: continue
        var date = range.start
        while (!date.isAfter(range.endInclusive)) {
            for (scheduledClass in classesOnDate(date, term.id, classes)) {
                val uid = calendarEventUid(scheduledClass.id, date)
                events[uid] = scheduledClass.toCalendarSyncEvent(date, term)
            }
            date = date.plusDays(1)
        }
    }
    return events.values.toList()
}

private fun Class.toCalendarSyncEvent(sessionDate: LocalDate, term: Term): CalendarSyncEvent {
    val rosterSummary = rosterLinesForClass(this, sessionDate)
        .joinToString(", ")
        .takeIf { it.isNotBlank() }
    val noteLines = buildList {
        add(term.name)
        if (rosterSummary != null) add(rosterSummary)
        if (notes.isNotBlank()) add(notes)
    }
    val title = if (rosterSummary != null) "$name — $rosterSummary" else name
    return CalendarSyncEvent(
        uid = calendarEventUid(id, sessionDate),
        title = title,
        start = sessionDate.atTimeFrom24h(startTime),
        end = sessionDate.atTimeFrom24h(endTime),
        location = locationLabel(locationId),
        notes = noteLines.joinToString("\n"),
    )
}

private fun locationLabel(locationId: String?): String {
    val location = locationId?.let { LocationStore.findById(it) } ?: return ""
    if (location.name.isNotBlank()) return location.name
    return location.formattedAddressLines().joinToString(", ")
}

private fun LocalDate.atTimeFrom24h(time24: String): String {
    val parts = time24.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return atTime(hour, minute).format(LocalDateTimeFormatter)
}
