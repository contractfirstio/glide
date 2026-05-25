package glide.data

import glide.model.AttendanceSessionKey
import glide.model.parseIsoLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Class sessions on the customer's calendar in the current plan period (not attendance).
 * Each scheduled class date counts toward the plan's class count; absences are handled via credits.
 */
fun countScheduledPlanSessionsInPeriod(
    soldPlanId: String,
    periodStartedAtMillis: Long,
    throughDate: LocalDate = LocalDate.now(),
): Int {
    val scheduledClass = ClassStore.findClassContainingSoldPlan(soldPlanId) ?: return 0
    val periodStart = localDateFromMillis(periodStartedAtMillis) ?: return 0
    return computeAllClassSessionDates(scheduledClass, startFrom = periodStart)
        .count { iso ->
            val date = parseIsoLocalDate(iso) ?: return@count false
            !date.isAfter(throughDate) &&
                isSoldPlanOnClassSession(soldPlanId, scheduledClass, date)
        }
}

/**
 * Submitted class sessions in the current plan period (present or absent).
 * Rolling-plan renewal bills are created after the last session's attendance is taken.
 */
fun countSubmittedPlanSessionsInPeriod(
    soldPlanId: String,
    periodStartedAtMillis: Long,
    throughDate: LocalDate = LocalDate.now(),
): Int {
    val scheduledClass = ClassStore.findClassContainingSoldPlan(soldPlanId) ?: return 0
    val periodStart = localDateFromMillis(periodStartedAtMillis) ?: return 0
    return computeAllClassSessionDates(scheduledClass, startFrom = periodStart)
        .count { iso ->
            val date = parseIsoLocalDate(iso) ?: return@count false
            if (date.isAfter(throughDate)) return@count false
            if (!isSoldPlanOnClassSession(soldPlanId, scheduledClass, date)) return@count false
            val session = AttendanceSessionKey(scheduledClass.id, iso)
            if (!AttendanceStore.isSessionSubmitted(session)) return@count false
            val groupAttendeeKeys = attendeesForClass(scheduledClass, date)
                .filter { it.soldPlanId == soldPlanId }
                .map { it.key }
            groupAttendeeKeys.isNotEmpty() &&
                groupAttendeeKeys.all { key -> AttendanceStore.statusFor(session, key) != null }
        }
}

private fun localDateFromMillis(millis: Long): LocalDate? =
    runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
