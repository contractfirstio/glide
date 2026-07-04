package glide.data

import glide.model.AttendanceSessionKey
import glide.model.BillStatus
import glide.model.Class
import glide.model.DayOfWeek
import glide.model.isIssuedToCustomer
import glide.model.dateRange
import glide.model.isWeekly
import glide.model.occursOn
import glide.model.parseIsoLocalDate
import glide.model.sessionHasEndedForAttendance
import java.time.LocalDate
import java.time.LocalTime

sealed class TransferSoldPlanResult {
    data class Success(val message: String) : TransferSoldPlanResult()
    data object SoldPlanNotFound : TransferSoldPlanResult()
    data object NotOnAClass : TransferSoldPlanResult()
    data object SameClass : TransferSoldPlanResult()
    data object TargetClassNotFound : TransferSoldPlanResult()
    data class OutstandingAttendance(val sessions: List<PendingAttendanceSession>) : TransferSoldPlanResult()
    data class PlanFullyUsed(val usedSessions: Int, val planSessions: Int) : TransferSoldPlanResult()
    data class PlanCannotBeFullyScheduled(
        val planSessions: Int,
        val availableSessions: Int,
    ) : TransferSoldPlanResult()
    data class CapacityExceeded(
        val currentHeadcount: Int,
        val groupHeadcount: Int,
        val maxCapacity: Int,
    ) : TransferSoldPlanResult()
    data class DailyCapacityExceeded(
        val day: DayOfWeek,
        val currentHeadcount: Int,
        val groupHeadcount: Int,
        val maxCapacity: Int,
    ) : TransferSoldPlanResult()
    data class RollingPlanNotAllowedOnClass(val message: String) : TransferSoldPlanResult()
    data class WeeklyDayCountMismatch(val requiredDays: Int, val selectedDays: Int) : TransferSoldPlanResult()
    data class ScheduleAssignmentFailed(val message: String) : TransferSoldPlanResult()
}

fun TransferSoldPlanResult.toUserMessage(): String = when (this) {
    is TransferSoldPlanResult.Success -> message
    TransferSoldPlanResult.SoldPlanNotFound -> "Sold plan not found."
    TransferSoldPlanResult.NotOnAClass -> "This sold plan is not assigned to a class."
    TransferSoldPlanResult.SameClass -> "This sold plan is already on that class."
    TransferSoldPlanResult.TargetClassNotFound -> "Class not found."
    is TransferSoldPlanResult.OutstandingAttendance -> {
        val count = sessions.size
        if (count == 1) {
            "Submit attendance for ${sessions.first().className} " +
                "(${formatTransferSessionDate(sessions.first().sessionDate)}) before moving class."
        } else {
            "Submit attendance for $count past class sessions before moving class."
        }
    }
    is TransferSoldPlanResult.PlanFullyUsed ->
        "All $planSessions plan sessions have been used ($usedSessions submitted). Nothing left to schedule on a new class."
    is TransferSoldPlanResult.PlanCannotBeFullyScheduled ->
        planCannotFullyScheduleMessage(planSessions, availableSessions)
    is TransferSoldPlanResult.CapacityExceeded ->
        "Room capacity exceeded ($currentHeadcount + $groupHeadcount > $maxCapacity)."
    is TransferSoldPlanResult.DailyCapacityExceeded ->
        "${day.label} is full ($currentHeadcount + $groupHeadcount > $maxCapacity)."
    is TransferSoldPlanResult.RollingPlanNotAllowedOnClass -> message
    is TransferSoldPlanResult.WeeklyDayCountMismatch ->
        "Select $requiredDays ${if (requiredDays == 1) "day" else "days"} for the remaining sessions " +
            "($selectedDays selected)."
    is TransferSoldPlanResult.ScheduleAssignmentFailed -> message
}

fun transferBillStatusWarning(soldPlanId: String): String? {
    val hasIssuedOrPaid = BillStore.forSoldPlan(soldPlanId).any { bill ->
        bill.status != BillStatus.VOID && bill.isIssuedToCustomer()
    }
    return if (hasIssuedOrPaid) {
        "This sold plan has issued or paid bills. The class schedule will change; bill amounts are not adjusted automatically."
    } else {
        null
    }
}

data class SoldPlanTransferPreview(
    val soldPlanId: String,
    val fromClass: Class,
    val planSessions: Int?,
    val usedSessions: Int,
    val remainingSessions: Int?,
    val defaultEffectiveDate: LocalDate,
    val billWarning: String?,
)

fun buildSoldPlanTransferPreview(
    soldPlanId: String,
    targetClass: Class,
    today: LocalDate = LocalDate.now(),
): SoldPlanTransferPreview? {
    val fromClass = ClassStore.findClassContainingSoldPlan(soldPlanId) ?: return null
    val enrollment = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId) ?: return null
    val limit = enrollment.planSnapshot.classSessionLimit()
    val used = countSubmittedSessionsForSoldPlanOnClass(soldPlanId, fromClass, throughDate = today)
    return SoldPlanTransferPreview(
        soldPlanId = soldPlanId,
        fromClass = fromClass,
        planSessions = limit,
        usedSessions = used,
        remainingSessions = limit?.let { (it - used).coerceAtLeast(0) },
        defaultEffectiveDate = defaultClassTransferEffectiveDate(targetClass, today),
        billWarning = transferBillStatusWarning(soldPlanId),
    )
}

/** First class date on [targetClass] on or after [today]; falls back to [today]. */
fun defaultClassTransferEffectiveDate(
    targetClass: Class,
    today: LocalDate = LocalDate.now(),
): LocalDate =
    computeAllClassSessionDates(targetClass, startFrom = today)
        .firstOrNull()
        ?.let { parseIsoLocalDate(it) }
        ?: today

fun findOutstandingAttendanceForSoldPlanTransfer(
    soldPlanId: String,
    fromClass: Class,
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now(),
): List<PendingAttendanceSession> {
    val pending = mutableListOf<PendingAttendanceSession>()
    collectPastSessionsNeedingAttendanceForSoldPlan(soldPlanId, fromClass, today, now, pending)
    return pending.sortedWith(compareBy({ it.sessionDate }, { it.className.lowercase() }))
}

fun countSubmittedSessionsForSoldPlanOnClass(
    soldPlanId: String,
    scheduledClass: Class,
    throughDate: LocalDate = LocalDate.now(),
): Int {
    val periodStart = soldPlanPlanPeriodStartDate(soldPlanId)
    return computeAllClassSessionDates(scheduledClass, startFrom = periodStart)
        .count { iso ->
            val date = parseIsoLocalDate(iso) ?: return@count false
            if (date.isAfter(throughDate)) return@count false
            if (!isSoldPlanOnClassSession(soldPlanId, scheduledClass, date)) return@count false
            val session = AttendanceSessionKey(scheduledClass.id, iso)
            if (!AttendanceStore.isSessionSubmitted(session)) return@count false
            val attendeeKeys = attendeesForClass(scheduledClass, date)
                .filter { it.soldPlanId == soldPlanId }
                .map { it.key }
            attendeeKeys.isNotEmpty() &&
                attendeeKeys.all { key -> AttendanceStore.statusFor(session, key) != null }
        }
}

fun transferSoldPlanToClass(
    soldPlanId: String,
    targetClassId: String,
    effectiveDate: LocalDate,
    weeklySelectedDays: Set<DayOfWeek>? = null,
): TransferSoldPlanResult {
    if (findSoldPlanById(soldPlanId) == null) return TransferSoldPlanResult.SoldPlanNotFound

    val fromClass = ClassStore.findClassContainingSoldPlan(soldPlanId)
        ?: return TransferSoldPlanResult.NotOnAClass

    val targetClass = ClassStore.findById(targetClassId)
        ?: return TransferSoldPlanResult.TargetClassNotFound

    if (fromClass.id == targetClass.id) return TransferSoldPlanResult.SameClass

    findOutstandingAttendanceForSoldPlanTransfer(soldPlanId, fromClass).let { outstanding ->
        if (outstanding.isNotEmpty()) {
            return TransferSoldPlanResult.OutstandingAttendance(outstanding)
        }
    }

    validateRollingSoldPlanEnrollmentForClass(soldPlanId, targetClass)?.let { message ->
        return TransferSoldPlanResult.RollingPlanNotAllowedOnClass(message)
    }

    val enrollment = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId)
    val sessionLimit = enrollment?.planSnapshot?.classSessionLimit()
    val remainingSessions = sessionLimit?.let { limit ->
        val used = countSubmittedSessionsForSoldPlanOnClass(soldPlanId, fromClass)
        val remaining = (limit - used).coerceAtLeast(0)
        if (remaining == 0) {
            return TransferSoldPlanResult.PlanFullyUsed(usedSessions = used, planSessions = limit)
        }
        remaining
    }

    if (remainingSessions != null) {
        planScheduleCheckForClass(
            soldPlanId = soldPlanId,
            scheduledClass = targetClass,
            requiredSessions = remainingSessions,
            startFrom = effectiveDate,
        )?.let { check ->
            if (!check.canFullySchedule) {
                return TransferSoldPlanResult.PlanCannotBeFullyScheduled(
                    planSessions = check.requiredSessions,
                    availableSessions = check.availableSessions,
                )
            }
        }
    } else {
        planScheduleCheckForClass(
            soldPlanId = soldPlanId,
            scheduledClass = targetClass,
            startFrom = effectiveDate,
        )?.let { check ->
            if (!check.canFullySchedule) {
                return TransferSoldPlanResult.PlanCannotBeFullyScheduled(
                    planSessions = check.requiredSessions,
                    availableSessions = check.availableSessions,
                )
            }
        }
    }

    val group = findSoldPlanById(soldPlanId)!!

    if (targetClass.isWeekly() && remainingSessions != null) {
        val days = weeklySelectedDays
            ?: return TransferSoldPlanResult.WeeklyDayCountMismatch(
                requiredDays = remainingSessions,
                selectedDays = 0,
            )
        if (days.size != remainingSessions) {
            return TransferSoldPlanResult.WeeklyDayCountMismatch(
                requiredDays = remainingSessions,
                selectedDays = days.size,
            )
        }
        validateWeeklyPlanFitsClass(soldPlanId, targetClass)?.let { message ->
            return TransferSoldPlanResult.RollingPlanNotAllowedOnClass(message)
        }
        validateWeeklyDayCapacity(
            scheduledClass = targetClass,
            addingSoldPlanId = soldPlanId,
            selectedDays = days,
            currentSoldPlanIds = targetClass.soldPlanIds,
        )?.let { capacityResult ->
            return when (capacityResult) {
                is AddSoldPlanResult.DailyCapacityExceeded -> TransferSoldPlanResult.DailyCapacityExceeded(
                    day = capacityResult.day,
                    currentHeadcount = capacityResult.currentHeadcount,
                    groupHeadcount = capacityResult.groupHeadcount,
                    maxCapacity = capacityResult.maxCapacity,
                )
                is AddSoldPlanResult.CapacityExceeded -> TransferSoldPlanResult.CapacityExceeded(
                    currentHeadcount = capacityResult.currentHeadcount,
                    groupHeadcount = capacityResult.groupHeadcount,
                    maxCapacity = capacityResult.maxCapacity,
                )
                else -> TransferSoldPlanResult.CapacityExceeded(
                    currentHeadcount = 0,
                    groupHeadcount = group.classAttendeeCount(),
                    maxCapacity = targetClass.locationId?.let { LocationStore.findById(it)?.maxCapacity } ?: 0,
                )
            }
        }
    } else {
        targetClass.locationId?.let { locationId ->
            val maxCapacity = LocationStore.findById(locationId)?.maxCapacity
            if (maxCapacity != null) {
                val current = headcountForSoldPlans(targetClass.soldPlanIds)
                val adding = group.classAttendeeCount()
                if (current + adding > maxCapacity) {
                    return TransferSoldPlanResult.CapacityExceeded(
                        currentHeadcount = current,
                        groupHeadcount = adding,
                        maxCapacity = maxCapacity,
                    )
                }
            }
        }
    }

    val fromWithRemoved = fromClass.copy(soldPlanIds = fromClass.soldPlanIds - soldPlanId)
    ClassStore.update(fromWithRemoved)
    SoldPlanClassScheduleStore.remove(soldPlanId, fromClass.id)

    val targetWithAdded = targetClass.copy(soldPlanIds = targetClass.soldPlanIds + soldPlanId)
    ClassStore.update(targetWithAdded)

    val assignmentMessage = when {
        targetClass.isWeekly() && remainingSessions != null -> {
            val assignment = assignWeeklySoldPlanClassSchedule(
                soldPlanId = soldPlanId,
                scheduledClass = targetWithAdded,
                selectedDays = weeklySelectedDays!!,
                sessionCount = remainingSessions,
                startFrom = effectiveDate,
            )
            assignment.toScheduleMessage()?.let { msg ->
                rollbackSoldPlanClassTransfer(soldPlanId, fromClass, targetClass)
                return TransferSoldPlanResult.ScheduleAssignmentFailed(msg)
            }
            assignment.toTransferSuccessDetail(remainingSessions)
        }
        remainingSessions != null -> {
            val assignment = assignSoldPlanClassSchedule(
                soldPlanId = soldPlanId,
                scheduledClass = targetWithAdded,
                maxSessions = remainingSessions,
                startFrom = effectiveDate,
            )
            assignment.toScheduleMessage()?.let { msg ->
                rollbackSoldPlanClassTransfer(soldPlanId, fromClass, targetClass)
                return TransferSoldPlanResult.ScheduleAssignmentFailed(msg)
            }
            assignment.toTransferSuccessDetail(remainingSessions)
        }
        else -> {
            assignSoldPlanClassSchedule(soldPlanId, targetWithAdded)
            "Rolling plan moved to \"${targetClass.name}\"."
        }
    }

    RollingPlanBillingService.syncRollingPlanBilling(soldPlanId)

    return TransferSoldPlanResult.Success(
        message = "Moved to \"${targetClass.name}\". $assignmentMessage",
    )
}

private fun rollbackSoldPlanClassTransfer(
    soldPlanId: String,
    fromClass: Class,
    targetClass: Class,
) {
    ClassStore.findById(targetClass.id)?.let { currentTarget ->
        ClassStore.update(currentTarget.copy(soldPlanIds = currentTarget.soldPlanIds - soldPlanId))
    }
    SoldPlanClassScheduleStore.remove(soldPlanId, targetClass.id)
    val restoredFrom = ClassStore.findById(fromClass.id) ?: fromClass
    ClassStore.update(restoredFrom.copy(soldPlanIds = restoredFrom.soldPlanIds + soldPlanId))
    assignSoldPlanClassSchedule(soldPlanId, restoredFrom)
}

private fun PlanScheduleAssignment.toTransferSuccessDetail(remainingSessions: Int): String =
    when (this) {
        is PlanScheduleAssignment.Fixed ->
            "$remainingSessions remaining session${if (remainingSessions == 1) "" else "s"} scheduled."
        PlanScheduleAssignment.Unlimited -> "Schedule updated."
        else -> "Schedule updated."
    }

private fun collectPastSessionsNeedingAttendanceForSoldPlan(
    soldPlanId: String,
    scheduledClass: Class,
    today: LocalDate,
    now: LocalTime,
    pending: MutableList<PendingAttendanceSession>,
) {
    val dates = mutableSetOf<LocalDate>()
    for (termId in scheduledClass.termIds) {
        val term = TermStore.findById(termId) ?: continue
        val range = term.dateRange() ?: continue
        var date = range.start
        while (!date.isAfter(range.endInclusive) && !date.isAfter(today)) {
            if (
                scheduledClass.occursOn(date) &&
                scheduledClass.sessionHasEndedForAttendance(sessionDate = date, today = today, now = now) &&
                isSoldPlanOnClassSession(soldPlanId, scheduledClass, date)
            ) {
                dates.add(date)
            }
            date = date.plusDays(1)
        }
    }
    for (date in dates) {
        val pendingSession = pendingAttendanceForSession(scheduledClass, date) ?: continue
        pending.add(pendingSession)
    }
}

private fun formatTransferSessionDate(date: LocalDate): String =
    "${date.dayOfMonth} ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)}"
