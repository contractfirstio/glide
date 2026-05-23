package glide.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

data class AcademicTerm(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** ISO local date (yyyy-MM-dd). */
    val startDate: String,
    val endDate: String,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

private val IsoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun parseIsoLocalDate(isoDate: String): LocalDate? {
    if (isoDate.isBlank()) return null
    return try {
        LocalDate.parse(isoDate, IsoDateFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
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

fun dateRangesOverlap(a: ClosedRange<LocalDate>, b: ClosedRange<LocalDate>): Boolean =
    !a.endInclusive.isBefore(b.start) && !b.endInclusive.isBefore(a.start)

fun AcademicTerm.overlapsTerm(other: AcademicTerm): Boolean {
    if (id == other.id) return false
    val thisRange = dateRange() ?: return false
    val otherRange = other.dateRange() ?: return false
    return dateRangesOverlap(thisRange, otherRange)
}

/** First term in [terms] whose dates overlap [candidate], excluding [excludeTermId]. */
fun findOverlappingTerm(
    terms: List<AcademicTerm>,
    candidate: AcademicTerm,
    excludeTermId: String? = null,
): AcademicTerm? =
    terms.firstOrNull { term ->
        term.id != excludeTermId && term.overlapsTerm(candidate)
    }

fun AcademicTerm.summaryLine(): String {
    if (startDate.isBlank() && endDate.isBlank()) return ""
    if (startDate.isBlank()) return endDate
    if (endDate.isBlank()) return startDate
    return "$startDate – $endDate"
}
