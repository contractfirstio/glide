package glide.data

import glide.model.PlanKind
import glide.model.PlanSnapshot
import glide.model.ScheduledClass
import glide.model.containsDate
import glide.model.dateRange
import glide.model.occursOn
import java.time.LocalDate

data class PackScheduleCheck(
    val requiredSessions: Int,
    val availableSessions: Int,
) {
    val canFullySchedule: Boolean get() = availableSessions >= requiredSessions
}

fun requiredClassSessionsForPack(snapshot: PlanSnapshot): Int = when (snapshot.kind) {
    PlanKind.SINGLE_LESSON_PACK -> 1
    else -> snapshot.lessonCount.coerceAtLeast(1)
}

/** All future class occurrence dates across the class's linked terms (from today). */
fun computeAllClassSessionDates(
    scheduledClass: ScheduledClass,
    startFrom: LocalDate = LocalDate.now(),
): List<String> {
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
    return dates.distinct().sorted().map { it.toString() }
}

fun packScheduleCheckForClass(peopleGroupId: String, scheduledClass: ScheduledClass): PackScheduleCheck? {
    val snapshot = PackEnrollmentStore.forPeopleGroup(peopleGroupId)?.planSnapshot ?: return null
    val required = requiredClassSessionsForPack(snapshot)
    val available = computeAllClassSessionDates(scheduledClass).size
    return PackScheduleCheck(required, available)
}

/** First [maxSessions] class occurrence dates across the class's linked terms. */
fun computeClassSessionDates(
    scheduledClass: ScheduledClass,
    maxSessions: Int,
    startFrom: LocalDate = LocalDate.now(),
): List<String> {
    if (maxSessions <= 0) return emptyList()
    return computeAllClassSessionDates(scheduledClass, startFrom).take(maxSessions)
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
    if (dates.size < limit) {
        PackClassScheduleStore.remove(peopleGroupId, scheduledClass.id)
        return if (dates.isEmpty()) {
            PackScheduleAssignment.NoSessionsAvailable(limit)
        } else {
            PackScheduleAssignment.Partial(limit, dates.size)
        }
    }
    PackClassScheduleStore.set(peopleGroupId, scheduledClass.id, dates)
    return PackScheduleAssignment.Fixed(limit)
}

fun ensurePackClassSchedule(peopleGroupId: String, scheduledClass: ScheduledClass) {
    val limit = sessionLimitForPeopleGroup(peopleGroupId) ?: return
    if (PackClassScheduleStore.sessionDatesFor(peopleGroupId, scheduledClass.id) != null) return
    val check = packScheduleCheckForClass(peopleGroupId, scheduledClass) ?: return
    if (!check.canFullySchedule) return
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
        packCannotFullyScheduleMessage(packSessions, scheduledSessions)
    is PackScheduleAssignment.NoSessionsAvailable ->
        packCannotFullyScheduleMessage(packSessions, 0)
}

fun packCannotFullyScheduleMessage(requiredSessions: Int, availableSessions: Int): String =
    if (availableSessions == 0) {
        "This pack needs $requiredSessions class session${if (requiredSessions == 1) "" else "s"} but there are " +
            "no matching dates on this class in its linked terms. Create a new term to extend the calendar."
    } else {
        "This pack needs $requiredSessions class session${if (requiredSessions == 1) "" else "s"} but only " +
            "$availableSessions ${if (availableSessions == 1) "is" else "are"} available in the linked terms. " +
            "Create a new term to extend the calendar."
    }

fun validatePackSchedulesForClass(
    customerGroupIds: List<String>,
    scheduledClass: ScheduledClass,
): String? {
    for (groupId in customerGroupIds) {
        val check = packScheduleCheckForClass(groupId, scheduledClass) ?: continue
        if (!check.canFullySchedule) {
            val label = PeopleGroupStore.findById(groupId)?.resolveMainContact()?.name?.takeIf { it.isNotBlank() }
                ?: "A customer group"
            return "$label: ${packCannotFullyScheduleMessage(check.requiredSessions, check.availableSessions)}"
        }
    }
    return null
}

fun isPeopleGroupOnClassSession(
    peopleGroupId: String,
    scheduledClass: ScheduledClass,
    sessionDate: LocalDate,
): Boolean {
    if (peopleGroupId !in scheduledClass.customerGroupIds) return false
    if (sessionLimitForPeopleGroup(peopleGroupId) != null) {
        val check = packScheduleCheckForClass(peopleGroupId, scheduledClass)
        if (check != null && !check.canFullySchedule) return false
    }
    ensurePackClassSchedule(peopleGroupId, scheduledClass)
    return PackClassScheduleStore.isScheduledForSession(peopleGroupId, scheduledClass.id, sessionDate)
}
