package glide.ui.scheduling

import glide.model.AcademicTerm
import glide.model.DayOfWeek
import glide.model.ScheduledClass
import glide.model.spansTerm
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val IsoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun parseIsoLocalDate(isoDate: String): LocalDate? {
    if (isoDate.isBlank()) return null
    return try {
        LocalDate.parse(isoDate, IsoDateFormatter)
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

fun AcademicTerm.dateRange(): ClosedRange<LocalDate>? {
    val start = parseIsoLocalDate(startDate) ?: return null
    val end = parseIsoLocalDate(endDate) ?: return null
    if (end.isBefore(start)) return null
    return start..end
}

fun AcademicTerm.containsDate(date: LocalDate): Boolean {
    val range = dateRange() ?: return false
    return date in range
}

fun findCurrentTerm(terms: List<AcademicTerm>, today: LocalDate = LocalDate.now()): AcademicTerm? =
    terms.firstOrNull { it.containsDate(today) }

fun defaultTermSelectionId(terms: List<AcademicTerm>, today: LocalDate = LocalDate.now()): String? {
    if (terms.isEmpty()) return null
    findCurrentTerm(terms, today)?.id?.let { return it }
    terms.firstOrNull { term ->
        val start = parseIsoLocalDate(term.startDate) ?: return@firstOrNull false
        start.isAfter(today)
    }?.id?.let { return it }
    return terms.lastOrNull()?.id
}

fun monthsInTerm(term: AcademicTerm): List<YearMonth> {
    val range = term.dateRange() ?: return emptyList()
    val months = mutableListOf<YearMonth>()
    var current = YearMonth.from(range.start)
    val endMonth = YearMonth.from(range.endInclusive)
    while (!current.isAfter(endMonth)) {
        months.add(current)
        current = current.plusMonths(1)
    }
    return months
}

data class TermCalendarDayCell(
    val date: LocalDate?,
    val inTerm: Boolean,
    val classes: List<ScheduledClass>,
)

fun classesOnDate(
    date: LocalDate,
    termId: String,
    classes: List<ScheduledClass>,
): List<ScheduledClass> =
    classes
        .filter { it.spansTerm(termId) && it.dayOfWeek.toJavaDayOfWeek() == date.dayOfWeek }
        .sortedWith(compareBy({ it.startTime }, { it.endTime }, { it.name }))

fun buildMonthGrid(
    yearMonth: YearMonth,
    term: AcademicTerm,
    classes: List<ScheduledClass>,
): List<TermCalendarDayCell> {
    val termRange = term.dateRange()
    val firstOfMonth = yearMonth.atDay(1)
    val startOffset = firstOfMonth.dayOfWeek.value - java.time.DayOfWeek.MONDAY.value

    val cells = mutableListOf<TermCalendarDayCell>()
    repeat(startOffset) {
        cells.add(TermCalendarDayCell(date = null, inTerm = false, classes = emptyList()))
    }

    for (day in 1..yearMonth.lengthOfMonth()) {
        val date = yearMonth.atDay(day)
        val inTerm = termRange != null && date in termRange
        val dayClasses = if (inTerm) classesOnDate(date, term.id, classes) else emptyList()
        cells.add(TermCalendarDayCell(date = date, inTerm = inTerm, classes = dayClasses))
    }

    while (cells.size % 7 != 0) {
        cells.add(TermCalendarDayCell(date = null, inTerm = false, classes = emptyList()))
    }
    return cells
}
