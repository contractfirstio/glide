package glide.data

import glide.model.AcademicTerm
import glide.model.PlanKind
import glide.model.PlanSnapshot
import glide.model.ScheduledClass
import glide.model.DayOfWeek
import glide.model.dateRange
import glide.model.formatWeeklyDaysLabel
import glide.model.isSingleDay
import glide.model.isWeekly
import glide.model.occursOn
import glide.model.parseIsoLocalDate
import glide.model.weekDateRangeFromIsoDate
import glide.model.toJavaDayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

fun validateWeeklyPlanFitsClass(peopleGroupId: String, scheduledClass: ScheduledClass): String? {
    if (!scheduledClass.isWeekly()) return null
    val enrollment = PackEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return null
    val required = requiredClassSessionsForPack(enrollment.planSnapshot)
    val available = scheduledClass.weeklyDays.size
    if (required <= available) return null
    return "This plan requires $required days but this weekly class only runs on $available days " +
        "(${formatWeeklyDaysLabel(scheduledClass.weeklyDays)})."
}

fun dateForDayInWeek(range: ClosedRange<LocalDate>, day: DayOfWeek): LocalDate? {
    var date = range.start
    while (!date.isAfter(range.endInclusive)) {
        if (day.toJavaDayOfWeek() == date.dayOfWeek) return date
        date = date.plusDays(1)
    }
    return null
}

fun weeklyClassSessionDates(
    scheduledClass: ScheduledClass,
    selectedDays: Collection<DayOfWeek>,
    peopleGroupId: String? = null,
    startFrom: LocalDate = peopleGroupId?.let { peopleGroupPackPeriodStartDate(it) } ?: packScheduleStartDate(),
): List<String> {
    if (!scheduledClass.isWeekly()) return emptyList()
    val range = weekDateRangeFromIsoDate(scheduledClass.weekOfDate!!) ?: return emptyList()
    return selectedDays
        .sortedBy { it.sortOrder }
        .mapNotNull { day -> dateForDayInWeek(range, day) }
        .filter { !it.isBefore(startFrom) }
        .map { it.toString() }
}

fun assignWeeklyPackClassSchedule(
    peopleGroupId: String,
    scheduledClass: ScheduledClass,
    selectedDays: Set<DayOfWeek>,
): PackScheduleAssignment {
    val limit = sessionLimitForPeopleGroup(peopleGroupId)
        ?: PackEnrollmentStore.forPeopleGroup(peopleGroupId)?.let { requiredClassSessionsForPack(it.planSnapshot) }
        ?: return PackScheduleAssignment.NoSessionsAvailable(0)
    if (selectedDays.size != limit) {
        PackClassScheduleStore.remove(peopleGroupId, scheduledClass.id)
        return PackScheduleAssignment.Partial(limit, selectedDays.size)
    }
    val dates = weeklyClassSessionDates(scheduledClass, selectedDays, peopleGroupId)
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

/** Earliest date a pack may be scheduled on a class (today and later). */
fun packScheduleStartDate(today: LocalDate = LocalDate.now()): LocalDate = today

/** Earliest session date for this sold pack (plan start / enrollment), including past dates. */
fun peopleGroupPackPeriodStartDate(peopleGroupId: String, today: LocalDate = LocalDate.now()): LocalDate {
    PeopleGroupStore.findById(peopleGroupId)?.planStartDate?.let { parseIsoLocalDate(it) }?.let { return it }
    PackEnrollmentStore.forPeopleGroup(peopleGroupId)?.packPeriodStartedAtMillis
        ?.let { localDateFromEpochMillis(it) }
        ?.let { return it }
    return today
}

fun LocalDate.isOnOrAfterPackScheduleStart(startFrom: LocalDate = packScheduleStartDate()): Boolean =
    !isBefore(startFrom)

fun filterFutureSessionDates(
    sessionDates: List<String>,
    startFrom: LocalDate = packScheduleStartDate(),
): List<String> =
    sessionDates.filter { iso -> parseIsoLocalDate(iso)?.isOnOrAfterPackScheduleStart(startFrom) == true }

/** All future class occurrence dates across the class's linked terms (from today). */
fun computeAllClassSessionDates(
    scheduledClass: ScheduledClass,
    startFrom: LocalDate = packScheduleStartDate(),
): List<String> {
    val terms = scheduledClass.termIds.mapNotNull { TermStore.findById(it) }.sortedBy { it.startDate }
    val dates = mutableListOf<LocalDate>()
    for (term in terms) {
        val range = term.dateRange() ?: continue
        var date = if (range.start.isBefore(startFrom)) startFrom else range.start
        while (!date.isAfter(range.endInclusive)) {
            if (scheduledClass.occursOn(date)) {
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
    startFrom: LocalDate = packScheduleStartDate(),
): List<String> {
    if (maxSessions <= 0) return emptyList()
    return filterFutureSessionDates(
        computeAllClassSessionDates(scheduledClass, startFrom).take(maxSessions),
        startFrom,
    )
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

fun scheduledClassHasRollingCustomerGroup(scheduledClass: ScheduledClass): Boolean =
    scheduledClass.customerGroupIds.any { groupId ->
        PackEnrollmentStore.forPeopleGroup(groupId)?.planSnapshot?.rolling == true
    }

fun ScheduledClass.linkedAcademicTerms(): List<AcademicTerm> =
    termIds.mapNotNull { TermStore.findById(it) }

/** Rolling pack enrollments require a recurring class and every linked term to accept rolling plans. */
fun ScheduledClass.canAcceptRollingPackEnrollments(): Boolean {
    if (isWeekly()) return false
    val linked = linkedAcademicTerms()
    return linked.isNotEmpty() && linked.all { it.acceptsRollingPlans }
}

fun rollingPackNotAllowedOnClassMessage(scheduledClass: ScheduledClass? = null): String =
    if (scheduledClass?.isWeekly() == true) {
        "Rolling pack enrollments cannot be assigned to weekly classes."
    } else {
        "Rolling pack enrollments require every linked term to accept rolling plans."
    }

fun peopleGroupHasRollingPack(peopleGroupId: String): Boolean =
    PackEnrollmentStore.forPeopleGroup(peopleGroupId)?.planSnapshot?.rolling == true

fun validateRollingPackEnrollmentForClass(
    peopleGroupId: String,
    scheduledClass: ScheduledClass,
): String? {
    if (!peopleGroupHasRollingPack(peopleGroupId)) return null
    if (scheduledClass.canAcceptRollingPackEnrollments()) return null
    val label = PeopleGroupStore.findById(peopleGroupId)?.resolveMainContact()?.name?.takeIf { it.isNotBlank() }
        ?: "This customer group"
    return "$label has a rolling pack. ${rollingPackNotAllowedOnClassMessage(scheduledClass)}"
}

fun validateRollingPackEnrollmentsForClass(
    customerGroupIds: List<String>,
    scheduledClass: ScheduledClass,
): String? {
    for (groupId in customerGroupIds) {
        validateRollingPackEnrollmentForClass(groupId, scheduledClass)?.let { return it }
    }
    return null
}

fun validateTermDisablingRollingPlans(term: AcademicTerm): String? {
    if (term.acceptsRollingPlans) return null
    for (scheduledClass in ScheduledClassStore.classes) {
        if (term.id !in scheduledClass.termIds) continue
        for (groupId in scheduledClass.customerGroupIds) {
            if (!peopleGroupHasRollingPack(groupId)) continue
            val label = PeopleGroupStore.findById(groupId)?.resolveMainContact()?.name?.takeIf { it.isNotBlank() }
                ?: "A customer group"
            return "Cannot disable rolling plans on this term: \"$label\" on class \"${scheduledClass.name}\" " +
                "has a rolling pack."
        }
    }
    return null
}

/** Whether [newTerm] should be linked to [scheduledClass] when it is created (extends calendar forward). */
fun shouldAutoLinkNewTermToClass(scheduledClass: ScheduledClass, newTerm: AcademicTerm): Boolean {
    if (newTerm.id in scheduledClass.termIds) return false
    if (!newTerm.acceptsRollingPlans) return false
    if (scheduledClass.isSingleDay()) return false
    if (scheduledClass.isWeekly()) return false
    if (!scheduledClassHasRollingCustomerGroup(scheduledClass)) return false

    val newRange = newTerm.dateRange() ?: return false
    if (newRange.endInclusive.isBefore(packScheduleStartDate())) return false

    if (scheduledClass.termIds.isEmpty()) return true

    val linkedTerms = scheduledClass.termIds.mapNotNull { TermStore.findById(it) }
    if (linkedTerms.isEmpty()) return true

    val latestEnd = linkedTerms.mapNotNull { it.dateRange()?.endInclusive }.maxOrNull() ?: return true
    return !newRange.start.isBefore(latestEnd)
}

/** Adds [newTerm] to recurring classes with rolling groups and refreshes their pack schedules. */
fun extendClassesWithRollingGroupsForNewTerm(newTerm: AcademicTerm) {
    ScheduledClassStore.classes.toList().forEach { scheduledClass ->
        if (!shouldAutoLinkNewTermToClass(scheduledClass, newTerm)) return@forEach
        val updated = scheduledClass.copy(termIds = scheduledClass.termIds + newTerm.id)
        ScheduledClassStore.update(updated)
        updated.customerGroupIds
            .filter { groupId -> PackEnrollmentStore.forPeopleGroup(groupId)?.planSnapshot?.rolling == true }
            .forEach { groupId ->
                assignPackClassSchedule(groupId, updated)
                RollingPackBillingService.syncRollingPackBilling(groupId)
            }
    }
}

fun isPeopleGroupOnClassSession(
    peopleGroupId: String,
    scheduledClass: ScheduledClass,
    sessionDate: LocalDate,
): Boolean {
    if (peopleGroupId !in scheduledClass.customerGroupIds) return false
    if (sessionDate.isBefore(peopleGroupPackPeriodStartDate(peopleGroupId))) return false
    if (sessionLimitForPeopleGroup(peopleGroupId) != null) {
        val check = packScheduleCheckForClass(peopleGroupId, scheduledClass)
        if (check != null && !check.canFullySchedule) return false
    }
    ensurePackClassSchedule(peopleGroupId, scheduledClass)
    return PackClassScheduleStore.isScheduledForSession(peopleGroupId, scheduledClass.id, sessionDate)
}

private fun localDateFromEpochMillis(millis: Long): LocalDate? =
    runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
