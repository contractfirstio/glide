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
        val scheduleDates = filterSessionDatesFromPlanPeriodStart(soldPlanId, sessionDates)
        if (scheduleDates.isEmpty()) return
        _schedules.add(
            SoldPlanClassSchedule(
                soldPlanId = soldPlanId,
                classId = classId,
                sessionDates = scheduleDates,
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
        sessionDatesFor(soldPlanId, classId)?.let { filterSessionDatesFromPlanPeriodStart(soldPlanId, it).size }

    fun formatSessionDatesLabel(soldPlanId: String, classId: String): String? {
        val dates = sessionDatesFor(soldPlanId, classId)?.let { filterSessionDatesFromPlanPeriodStart(soldPlanId, it) }
            ?: return null
        if (dates.isEmpty()) return null
        return dates.joinToString(", ") { iso ->
            parseIsoLocalDate(iso)?.let { formatScheduleDateShort(it) } ?: iso
        }
    }
}

private fun filterSessionDatesFromPlanPeriodStart(soldPlanId: String, sessionDates: List<String>): List<String> {
    val planPeriodStart = soldPlanPlanPeriodStartDate(soldPlanId)
    return sessionDates
        .filter { iso -> parseIsoLocalDate(iso)?.let { !it.isBefore(planPeriodStart) } == true }
        .distinct()
        .sorted()
}

private fun formatScheduleDateShort(date: LocalDate): String {
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "${date.dayOfMonth} $month"
}
