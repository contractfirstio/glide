package glide.model

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

fun AcademicTerm.summaryLine(): String {
    if (startDate.isBlank() && endDate.isBlank()) return ""
    if (startDate.isBlank()) return endDate
    if (endDate.isBlank()) return startDate
    return "$startDate – $endDate"
}
