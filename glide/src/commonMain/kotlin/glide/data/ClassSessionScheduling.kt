package glide.data

import glide.model.Term
import glide.model.PlanKind
import glide.model.PlanSnapshot
import glide.model.Class
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

data class PlanScheduleCheck(
    val requiredSessions: Int,
    val availableSessions: Int,
) {
    val canFullySchedule: Boolean get() = availableSessions >= requiredSessions
}

fun requiredClassSessionsForPlan(snapshot: PlanSnapshot): Int = when (snapshot.kind) {
    PlanKind.SINGLE_LESSON_PLAN -> 1
    else -> snapshot.lessonCount.coerceAtLeast(1)
}

fun validateWeeklyPlanFitsClass(soldPlanId: String, scheduledClass: Class): String? {
    if (!scheduledClass.isWeekly()) return null
    val enrollment = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId) ?: return null
    val required = requiredClassSessionsForPlan(enrollment.planSnapshot)
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
    scheduledClass: Class,
    selectedDays: Collection<DayOfWeek>,
    soldPlanId: String? = null,
    startFrom: LocalDate = soldPlanId?.let { soldPlanPlanPeriodStartDate(it) } ?: planScheduleStartDate(),
): List<String> {
    if (!scheduledClass.isWeekly()) return emptyList()
    val range = weekDateRangeFromIsoDate(scheduledClass.weekOfDate!!) ?: return emptyList()
    return selectedDays
        .sortedBy { it.sortOrder }
        .mapNotNull { day -> dateForDayInWeek(range, day) }
        .filter { !it.isBefore(startFrom) }
        .map { it.toString() }
}

fun assignWeeklySoldPlanClassSchedule(
    soldPlanId: String,
    scheduledClass: Class,
    selectedDays: Set<DayOfWeek>,
): PlanScheduleAssignment {
    val limit = sessionLimitForSoldPlan(soldPlanId)
        ?: SoldPlanEnrollmentStore.forSoldPlan(soldPlanId)?.let { requiredClassSessionsForPlan(it.planSnapshot) }
        ?: return PlanScheduleAssignment.NoSessionsAvailable(0)
    if (selectedDays.size != limit) {
        SoldPlanClassScheduleStore.remove(soldPlanId, scheduledClass.id)
        return PlanScheduleAssignment.Partial(limit, selectedDays.size)
    }
    val dates = weeklyClassSessionDates(scheduledClass, selectedDays, soldPlanId)
    if (dates.size < limit) {
        SoldPlanClassScheduleStore.remove(soldPlanId, scheduledClass.id)
        return if (dates.isEmpty()) {
            PlanScheduleAssignment.NoSessionsAvailable(limit)
        } else {
            PlanScheduleAssignment.Partial(limit, dates.size)
        }
    }
    SoldPlanClassScheduleStore.set(soldPlanId, scheduledClass.id, dates)
    return PlanScheduleAssignment.Fixed(limit)
}

/** Earliest date a plan may be scheduled on a class (today and later). */
fun planScheduleStartDate(today: LocalDate = LocalDate.now()): LocalDate = today

/** Earliest session date for this sold plan (plan start / enrollment), including past dates. */
fun soldPlanPlanPeriodStartDate(soldPlanId: String, today: LocalDate = LocalDate.now()): LocalDate {
    findSoldPlanById(soldPlanId)?.planStartDate?.let { parseIsoLocalDate(it) }?.let { return it }
    SoldPlanEnrollmentStore.forSoldPlan(soldPlanId)?.planPeriodStartedAtMillis
        ?.let { localDateFromEpochMillis(it) }
        ?.let { return it }
    return today
}

fun LocalDate.isOnOrAfterPlanScheduleStart(startFrom: LocalDate = planScheduleStartDate()): Boolean =
    !isBefore(startFrom)

fun filterFutureSessionDates(
    sessionDates: List<String>,
    startFrom: LocalDate = planScheduleStartDate(),
): List<String> =
    sessionDates.filter { iso -> parseIsoLocalDate(iso)?.isOnOrAfterPlanScheduleStart(startFrom) == true }

/** All future class occurrence dates across the class's linked terms (from today). */
fun computeAllClassSessionDates(
    scheduledClass: Class,
    startFrom: LocalDate = planScheduleStartDate(),
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

fun planScheduleCheckForClass(soldPlanId: String, scheduledClass: Class): PlanScheduleCheck? {
    val snapshot = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId)?.planSnapshot ?: return null
    val required = requiredClassSessionsForPlan(snapshot)
    val available = computeAllClassSessionDates(
        scheduledClass = scheduledClass,
        startFrom = soldPlanPlanPeriodStartDate(soldPlanId),
    ).size
    return PlanScheduleCheck(required, available)
}

/** First [maxSessions] class occurrence dates across the class's linked terms. */
fun computeClassSessionDates(
    scheduledClass: Class,
    maxSessions: Int,
    startFrom: LocalDate = planScheduleStartDate(),
): List<String> {
    if (maxSessions <= 0) return emptyList()
    return filterFutureSessionDates(
        computeAllClassSessionDates(scheduledClass, startFrom).take(maxSessions),
        startFrom,
    )
}

fun sessionLimitForSoldPlan(soldPlanId: String): Int? =
    SoldPlanEnrollmentStore.forSoldPlan(soldPlanId)?.planSnapshot?.classSessionLimit()

fun assignSoldPlanClassSchedule(soldPlanId: String, scheduledClass: Class): PlanScheduleAssignment {
    val limit = sessionLimitForSoldPlan(soldPlanId)
    if (limit == null) {
        SoldPlanClassScheduleStore.remove(soldPlanId, scheduledClass.id)
        return PlanScheduleAssignment.Unlimited
    }
    val dates = computeClassSessionDates(
        scheduledClass = scheduledClass,
        maxSessions = limit,
        startFrom = soldPlanPlanPeriodStartDate(soldPlanId),
    )
    if (dates.size < limit) {
        SoldPlanClassScheduleStore.remove(soldPlanId, scheduledClass.id)
        return if (dates.isEmpty()) {
            PlanScheduleAssignment.NoSessionsAvailable(limit)
        } else {
            PlanScheduleAssignment.Partial(limit, dates.size)
        }
    }
    SoldPlanClassScheduleStore.set(soldPlanId, scheduledClass.id, dates)
    return PlanScheduleAssignment.Fixed(limit)
}

fun ensureSoldPlanClassSchedule(soldPlanId: String, scheduledClass: Class) {
    if (sessionLimitForSoldPlan(soldPlanId) == null) return
    if (SoldPlanClassScheduleStore.sessionDatesFor(soldPlanId, scheduledClass.id) != null) return
    val check = planScheduleCheckForClass(soldPlanId, scheduledClass) ?: return
    if (!check.canFullySchedule) return
    assignSoldPlanClassSchedule(soldPlanId, scheduledClass)
}

sealed class PlanScheduleAssignment {
    data object Unlimited : PlanScheduleAssignment()
    data class Fixed(val sessions: Int) : PlanScheduleAssignment()
    data class Partial(val planSessions: Int, val scheduledSessions: Int) : PlanScheduleAssignment()
    data class NoSessionsAvailable(val planSessions: Int) : PlanScheduleAssignment()
}

fun PlanScheduleAssignment.toScheduleMessage(): String? = when (this) {
    PlanScheduleAssignment.Unlimited -> null
    is PlanScheduleAssignment.Fixed ->
        "Scheduled for $sessions class session${if (sessions == 1) "" else "s"} on this class."
    is PlanScheduleAssignment.Partial ->
        planCannotFullyScheduleMessage(planSessions, scheduledSessions)
    is PlanScheduleAssignment.NoSessionsAvailable ->
        planCannotFullyScheduleMessage(planSessions, 0)
}

fun planCannotFullyScheduleMessage(requiredSessions: Int, availableSessions: Int): String =
    if (availableSessions == 0) {
        "This plan needs $requiredSessions class session${if (requiredSessions == 1) "" else "s"} but there are " +
            "no matching dates on this class in its linked terms. Create a new term to extend the calendar."
    } else {
        "This plan needs $requiredSessions class session${if (requiredSessions == 1) "" else "s"} but only " +
            "$availableSessions ${if (availableSessions == 1) "is" else "are"} available in the linked terms. " +
            "Create a new term to extend the calendar."
    }

fun validatePlanSchedulesForClass(
    soldPlanIds: List<String>,
    scheduledClass: Class,
): String? {
    for (groupId in soldPlanIds) {
        val check = planScheduleCheckForClass(groupId, scheduledClass) ?: continue
        if (!check.canFullySchedule) {
            val label = findSoldPlanById(groupId)?.resolveMainClient()?.name?.takeIf { it.isNotBlank() }
                ?: "A sold plan"
            return "$label: ${planCannotFullyScheduleMessage(check.requiredSessions, check.availableSessions)}"
        }
    }
    return null
}

fun scheduledClassHasRollingCustomerGroup(scheduledClass: Class): Boolean =
    scheduledClass.soldPlanIds.any { groupId ->
        SoldPlanEnrollmentStore.forSoldPlan(groupId)?.planSnapshot?.rolling == true
    }

fun Class.linkedTerms(): List<Term> =
    termIds.mapNotNull { TermStore.findById(it) }

/** Rolling plan enrollments require a recurring class and every linked term to accept rolling plans. */
fun Class.canAcceptRollingSoldPlanEnrollments(): Boolean {
    if (isWeekly()) return false
    val linked = linkedTerms()
    return linked.isNotEmpty() && linked.all { it.acceptsRollingPlans }
}

fun rollingPlanNotAllowedOnClassMessage(scheduledClass: Class? = null): String =
    if (scheduledClass?.isWeekly() == true) {
        "Rolling plan enrollments cannot be assigned to weekly classes."
    } else {
        "Rolling plan enrollments require every linked term to accept rolling plans."
    }

fun peopleGroupHasRollingPlan(soldPlanId: String): Boolean =
    SoldPlanEnrollmentStore.forSoldPlan(soldPlanId)?.planSnapshot?.rolling == true

fun validateRollingSoldPlanEnrollmentForClass(
    soldPlanId: String,
    scheduledClass: Class,
): String? {
    if (!peopleGroupHasRollingPlan(soldPlanId)) return null
    if (scheduledClass.canAcceptRollingSoldPlanEnrollments()) return null
    val label = findSoldPlanById(soldPlanId)?.resolveMainClient()?.name?.takeIf { it.isNotBlank() }
        ?: "This sold plan"
    return "$label has a rolling plan. ${rollingPlanNotAllowedOnClassMessage(scheduledClass)}"
}

fun validateRollingSoldPlanEnrollmentsForClass(
    soldPlanIds: List<String>,
    scheduledClass: Class,
): String? {
    for (groupId in soldPlanIds) {
        validateRollingSoldPlanEnrollmentForClass(groupId, scheduledClass)?.let { return it }
    }
    return null
}

fun validateTermDisablingRollingPlans(term: Term): String? {
    if (term.acceptsRollingPlans) return null
    for (scheduledClass in ClassStore.classes) {
        if (term.id !in scheduledClass.termIds) continue
        for (groupId in scheduledClass.soldPlanIds) {
            if (!peopleGroupHasRollingPlan(groupId)) continue
            val label = findSoldPlanById(groupId)?.resolveMainClient()?.name?.takeIf { it.isNotBlank() }
                ?: "A sold plan"
            return "Cannot disable rolling plans on this term: \"$label\" on class \"${scheduledClass.name}\" " +
                "has a rolling plan."
        }
    }
    return null
}

/** Whether [newTerm] should be linked to [scheduledClass] when it is created (extends calendar forward). */
fun shouldAutoLinkNewTermToClass(scheduledClass: Class, newTerm: Term): Boolean {
    if (newTerm.id in scheduledClass.termIds) return false
    if (!newTerm.acceptsRollingPlans) return false
    if (scheduledClass.isSingleDay()) return false
    if (scheduledClass.isWeekly()) return false
    if (!scheduledClassHasRollingCustomerGroup(scheduledClass)) return false

    val newRange = newTerm.dateRange() ?: return false
    if (newRange.endInclusive.isBefore(planScheduleStartDate())) return false

    if (scheduledClass.termIds.isEmpty()) return true

    val linkedTerms = scheduledClass.termIds.mapNotNull { TermStore.findById(it) }
    if (linkedTerms.isEmpty()) return true

    val latestEnd = linkedTerms.mapNotNull { it.dateRange()?.endInclusive }.maxOrNull() ?: return true
    return !newRange.start.isBefore(latestEnd)
}

/** Adds [newTerm] to recurring classes with rolling groups and refreshes their plan schedules. */
fun extendClassesWithRollingGroupsForNewTerm(newTerm: Term) {
    ClassStore.classes.toList().forEach { scheduledClass ->
        if (!shouldAutoLinkNewTermToClass(scheduledClass, newTerm)) return@forEach
        val updated = scheduledClass.copy(termIds = scheduledClass.termIds + newTerm.id)
        ClassStore.update(updated)
        updated.soldPlanIds
            .filter { groupId -> SoldPlanEnrollmentStore.forSoldPlan(groupId)?.planSnapshot?.rolling == true }
            .forEach { groupId ->
                assignSoldPlanClassSchedule(groupId, updated)
                RollingPlanBillingService.syncRollingPlanBilling(groupId)
            }
    }
}

fun isSoldPlanOnClassSession(
    soldPlanId: String,
    scheduledClass: Class,
    sessionDate: LocalDate,
): Boolean {
    if (soldPlanId !in scheduledClass.soldPlanIds) return false
    if (sessionDate.isBefore(soldPlanPlanPeriodStartDate(soldPlanId))) return false
    if (sessionLimitForSoldPlan(soldPlanId) != null) {
        val check = planScheduleCheckForClass(soldPlanId, scheduledClass)
        if (check != null && !check.canFullySchedule) return false
    }
    ensureSoldPlanClassSchedule(soldPlanId, scheduledClass)
    return SoldPlanClassScheduleStore.isScheduledForSession(soldPlanId, scheduledClass.id, sessionDate)
}

private fun localDateFromEpochMillis(millis: Long): LocalDate? =
    runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
