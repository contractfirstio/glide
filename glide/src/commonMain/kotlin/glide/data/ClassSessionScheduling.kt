package glide.data

import glide.model.ScheduledClass
import glide.model.containsDate
import glide.model.dateRange
import glide.model.occursOn
import glide.model.parseIsoLocalDate
import java.time.LocalDate

/** First [maxSessions] class occurrence dates across the class's linked terms. */
fun computeClassSessionDates(
    scheduledClass: ScheduledClass,
    maxSessions: Int,
    startFrom: LocalDate = LocalDate.now(),
): List<String> {
    if (maxSessions <= 0) return emptyList()
    val terms = scheduledClass.termIds.mapNotNull { TermStore.findById(it) }.sortedBy { it.startDate }
    val dates = mutableListOf<LocalDate>()
    for (term in terms) {
        val range = term.dateRange() ?: continue
        var date = range.start
        while (!date.isAfter(range.endInclusive)) {
            if (!date.isBefore(startFrom) && scheduledClass.occursOn(date)) {
                dates.add(date)
            }
            date = date.plusDays(1)
        }
    }
    return dates.distinct().sorted().take(maxSessions).map { it.toString() }
}

fun sessionLimitForPeopleGroup(peopleGroupId: String): Int? =
    PackEnrollmentStore.forPeopleGroup(peopleGroupId)?.planSnapshot?.classSessionLimit()

fun assignPackClassSchedule(peopleGroupId: String, scheduledClass: ScheduledClass): PackScheduleAssignment {
    val limit = sessionLimitForPeopleGroup(peopleGroupId)
    if (limit == null) {
        PackClassScheduleStore.remove(peopleGroupId, scheduledClass.id)
        return PackScheduleAssignment.Unlimited
    }
    val dates = computeClassSessionDates(scheduledClass, limit)
    if (dates.isEmpty()) {
        PackClassScheduleStore.remove(peopleGroupId, scheduledClass.id)
        return PackScheduleAssignment.NoSessionsAvailable(limit)
    }
    PackClassScheduleStore.set(peopleGroupId, scheduledClass.id, dates)
    return if (dates.size < limit) {
        PackScheduleAssignment.Partial(limit, dates.size)
    } else {
        PackScheduleAssignment.Fixed(limit)
    }
}

fun ensurePackClassSchedule(peopleGroupId: String, scheduledClass: ScheduledClass) {
    val limit = sessionLimitForPeopleGroup(peopleGroupId) ?: return
    if (PackClassScheduleStore.sessionDatesFor(peopleGroupId, scheduledClass.id) != null) return
    assignPackClassSchedule(peopleGroupId, scheduledClass)
}

sealed class PackScheduleAssignment {
    data object Unlimited : PackScheduleAssignment()
    data class Fixed(val sessions: Int) : PackScheduleAssignment()
    data class Partial(val packSessions: Int, val scheduledSessions: Int) : PackScheduleAssignment()
    data class NoSessionsAvailable(val packSessions: Int) : PackScheduleAssignment()
}

fun PackScheduleAssignment.toScheduleMessage(): String? = when (this) {
    PackScheduleAssignment.Unlimited -> null
    is PackScheduleAssignment.Fixed ->
        "Scheduled for $sessions class session${if (sessions == 1) "" else "s"} on this class."
    is PackScheduleAssignment.Partial ->
        "Only $scheduledSessions session${if (scheduledSessions == 1) "" else "s"} available in the class terms " +
            "(pack has $packSessions)."
    is PackScheduleAssignment.NoSessionsAvailable ->
        "No class dates available in the linked terms for this pack ($packSessions sessions)."
}

fun isPeopleGroupOnClassSession(
    peopleGroupId: String,
    scheduledClass: ScheduledClass,
    sessionDate: LocalDate,
): Boolean {
    if (peopleGroupId !in scheduledClass.customerGroupIds) return false
    ensurePackClassSchedule(peopleGroupId, scheduledClass)
    return PackClassScheduleStore.isScheduledForSession(peopleGroupId, scheduledClass.id, sessionDate)
}
