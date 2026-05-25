package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.SoldPlanClassSchedule
import glide.model.parseIsoLocalDate
import java.time.LocalDate

object SoldPlanClassScheduleStore {
    private val _schedules = mutableStateListOf<SoldPlanClassSchedule>()

    val all: List<SoldPlanClassSchedule> get() = _schedules

    internal fun replaceAll(schedules: List<SoldPlanClassSchedule>) {
        _schedules.clear()
        _schedules.addAll(schedules)
    }

    fun sessionDatesFor(soldPlanId: String, classId: String): List<String>? =
        _schedules.find { it.soldPlanId == soldPlanId && it.classId == classId }
            ?.sessionDates

    fun set(soldPlanId: String, classId: String, sessionDates: List<String>) {
        remove(soldPlanId, classId)
        val futureDates = filterFutureSessionDates(sessionDates).distinct().sorted()
        if (futureDates.isEmpty()) return
        _schedules.add(
            SoldPlanClassSchedule(
                soldPlanId = soldPlanId,
                classId = classId,
                sessionDates = futureDates,
            ),
        )
        persistAppData()
    }

    fun remove(soldPlanId: String, classId: String) {
        val removed = _schedules.removeAll { it.soldPlanId == soldPlanId && it.classId == classId }
        if (removed) persistAppData()
    }

    fun clearForClass(classId: String) {
        val removed = _schedules.removeAll { it.classId == classId }
        if (removed) persistAppData()
    }

    fun clearForSoldPlan(soldPlanId: String) {
        val removed = _schedules.removeAll { it.soldPlanId == soldPlanId }
        if (removed) persistAppData()
    }

    fun isScheduledForSession(
        soldPlanId: String,
        classId: String,
        sessionDate: LocalDate,
    ): Boolean {
        if (sessionDate.isBefore(soldPlanPlanPeriodStartDate(soldPlanId))) return false
        val dates = sessionDatesFor(soldPlanId, classId) ?: return true
        return sessionDate.toString() in dates
    }

    fun scheduledSessionCount(soldPlanId: String, classId: String): Int? =
        sessionDatesFor(soldPlanId, classId)?.let { filterFutureSessionDates(it).size }

    fun formatSessionDatesLabel(soldPlanId: String, classId: String): String? {
        val dates = sessionDatesFor(soldPlanId, classId)?.let { filterFutureSessionDates(it) }
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
