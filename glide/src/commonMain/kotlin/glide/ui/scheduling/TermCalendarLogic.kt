package glide.ui.scheduling

import glide.data.PeopleGroupStore
import glide.data.isPeopleGroupOnClassSession
import glide.data.rosterNameLabels
import glide.model.AcademicTerm
import glide.model.ScheduledClass
import glide.model.containsDate
import glide.model.dateRange
import glide.model.occursOn
import glide.model.parseIsoLocalDate
import glide.model.spansTerm
import java.time.LocalDate
import java.time.YearMonth

fun findCurrentTerm(terms: List<AcademicTerm>, today: LocalDate = LocalDate.now()): AcademicTerm? =
    terms.firstOrNull { it.containsDate(today) }

/** Term whose date range includes [isoDate], or null if none match. */
fun findTermContainingIsoDate(terms: List<AcademicTerm>, isoDate: String): AcademicTerm? {
    val date = parseIsoLocalDate(isoDate) ?: return null
    val matches = terms.filter { it.containsDate(date) }
    return when {
        matches.isEmpty() -> null
        matches.size == 1 -> matches.first()
        else -> matches.minWithOrNull(compareBy({ it.startDate }, { it.name }))
    }
}

/** Current term (if any) and every later term — default for new weekly classes. */
fun defaultRecurringClassTermIds(terms: List<AcademicTerm>, today: LocalDate = LocalDate.now()): Set<String> {
    if (terms.isEmpty()) return emptySet()
    val sorted = terms.sortedBy { it.startDate }
    val startIndex = sorted.indexOfFirst { it.containsDate(today) }.takeIf { it >= 0 }
        ?: sorted.indexOfFirst { term ->
            parseIsoLocalDate(term.startDate)?.let { !it.isBefore(today) } == true
        }.takeIf { it >= 0 }
        ?: return emptySet()
    return sorted.drop(startIndex).map { it.id }.toSet()
}

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
        .filter { it.spansTerm(termId) && it.occursOn(date) }
        .sortedWith(compareBy({ it.startTime }, { it.endTime }, { it.name }))

/** One line per customer group scheduled on this session: main contact and related names. */
fun rosterLinesForClass(scheduledClass: ScheduledClass, sessionDate: LocalDate): List<String> =
    scheduledClass.customerGroupIds
        .filter { groupId -> isPeopleGroupOnClassSession(groupId, scheduledClass, sessionDate) }
        .mapNotNull { groupId -> PeopleGroupStore.findById(groupId) }
        .map { group -> group.rosterNameLabels().joinToString(", ") }
        .filter { it.isNotBlank() }

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
