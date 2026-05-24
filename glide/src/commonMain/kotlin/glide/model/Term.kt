package glide.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

import kotlinx.serialization.Serializable
@Serializable
data class Term(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** ISO local date (yyyy-MM-dd). */
    val startDate: String,
    val endDate: String,
    val notes: String = "",
    /** When true, rolling plan classes may span into this term. */
    val acceptsRollingPlans: Boolean = true,
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

fun Term.dateRange(): ClosedRange<LocalDate>? {
    val start = parseIsoLocalDate(startDate) ?: return null
    val end = parseIsoLocalDate(endDate) ?: return null
    if (end.isBefore(start)) return null
    return start..end
}

fun Term.containsDate(date: LocalDate): Boolean {
    val range = dateRange() ?: return false
    return date in range
}

fun dateRangesOverlap(a: ClosedRange<LocalDate>, b: ClosedRange<LocalDate>): Boolean =
    !a.endInclusive.isBefore(b.start) && !b.endInclusive.isBefore(a.start)

fun Term.overlapsTerm(other: Term): Boolean {
    if (id == other.id) return false
    val thisRange = dateRange() ?: return false
    val otherRange = other.dateRange() ?: return false
    return dateRangesOverlap(thisRange, otherRange)
}

/** First term in [terms] whose dates overlap [candidate], excluding [excludeTermId]. */
fun findOverlappingTerm(
    terms: List<Term>,
    candidate: Term,
    excludeTermId: String? = null,
): Term? =
    terms.firstOrNull { term ->
        term.id != excludeTermId && term.overlapsTerm(candidate)
    }

fun Term.summaryLine(): String {
    if (startDate.isBlank() && endDate.isBlank()) return ""
    if (startDate.isBlank()) return endDate
    if (endDate.isBlank()) return startDate
    return "$startDate – $endDate"
}

private fun Term.sortableStartDate(): LocalDate? = parseIsoLocalDate(startDate)

private fun Term.sortableEndDate(): LocalDate? = parseIsoLocalDate(endDate)

/** Latest terms first (reverse chronological by start date). */
fun compareTermsReverseChronological(): Comparator<Term> =
    compareByDescending<Term> { it.sortableStartDate() ?: LocalDate.MIN }
        .thenByDescending { it.sortableEndDate() ?: LocalDate.MIN }
        .thenBy { it.name.lowercase() }

/** Earliest terms first (for term calendar navigation). */
fun compareTermsChronological(): Comparator<Term> =
    compareBy<Term> { it.sortableStartDate() ?: LocalDate.MAX }
        .thenBy { it.sortableEndDate() ?: LocalDate.MAX }
        .thenBy { it.name.lowercase() }