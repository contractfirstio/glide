package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.parseIsoLocalDate
import java.time.LocalDate

data class PackClassSchedule(
    val peopleGroupId: String,
    val scheduledClassId: String,
    /** ISO yyyy-MM-dd session dates, in chronological order. */
    val sessionDates: List<String>,
)

object PackClassScheduleStore {
    private val _schedules = mutableStateListOf<PackClassSchedule>()

    fun sessionDatesFor(peopleGroupId: String, scheduledClassId: String): List<String>? =
        _schedules.find { it.peopleGroupId == peopleGroupId && it.scheduledClassId == scheduledClassId }
            ?.sessionDates

    fun set(peopleGroupId: String, scheduledClassId: String, sessionDates: List<String>) {
        remove(peopleGroupId, scheduledClassId)
        val futureDates = filterFutureSessionDates(sessionDates).distinct().sorted()
        if (futureDates.isEmpty()) return
        _schedules.add(
            PackClassSchedule(
                peopleGroupId = peopleGroupId,
                scheduledClassId = scheduledClassId,
                sessionDates = futureDates,
            ),
        )
    }

    fun remove(peopleGroupId: String, scheduledClassId: String) {
        _schedules.removeAll { it.peopleGroupId == peopleGroupId && it.scheduledClassId == scheduledClassId }
    }

    fun clearForClass(scheduledClassId: String) {
        _schedules.removeAll { it.scheduledClassId == scheduledClassId }
    }

    fun clearForGroup(peopleGroupId: String) {
        _schedules.removeAll { it.peopleGroupId == peopleGroupId }
    }

    fun isScheduledForSession(
        peopleGroupId: String,
        scheduledClassId: String,
        sessionDate: LocalDate,
    ): Boolean {
        if (!sessionDate.isOnOrAfterPackScheduleStart()) return false
        val dates = sessionDatesFor(peopleGroupId, scheduledClassId) ?: return true
        return sessionDate.toString() in dates
    }

    fun scheduledSessionCount(peopleGroupId: String, scheduledClassId: String): Int? =
        sessionDatesFor(peopleGroupId, scheduledClassId)?.let { filterFutureSessionDates(it).size }

    fun formatSessionDatesLabel(peopleGroupId: String, scheduledClassId: String): String? {
        val dates = sessionDatesFor(peopleGroupId, scheduledClassId)?.let { filterFutureSessionDates(it) }
            ?: return null
        if (dates.isEmpty()) return null
        return dates.joinToString(", ") { iso ->
            parseIsoLocalDate(iso)?.let { formatScheduleDateShort(it) } ?: iso
        }
    }
}

private fun formatScheduleDateShort(date: LocalDate): String {
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "${date.dayOfMonth} $month"
}
