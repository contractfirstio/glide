package glide.data

import glide.model.ClassSessionKey
import glide.model.parseIsoLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Class sessions on the customer's calendar in the current plan period (not attendance).
 * Each scheduled class date counts toward the plan's class count; absences are handled via credits.
 */
fun countScheduledPlanSessionsInPeriod(
    peopleGroupId: String,
    periodStartedAtMillis: Long,
    throughDate: LocalDate = LocalDate.now(),
): Int {
    val scheduledClass = ScheduledClassStore.findClassContainingCustomerGroup(peopleGroupId) ?: return 0
    val periodStart = localDateFromMillis(periodStartedAtMillis) ?: return 0
    return computeAllClassSessionDates(scheduledClass, startFrom = periodStart)
        .count { iso ->
            val date = parseIsoLocalDate(iso) ?: return@count false
            !date.isAfter(throughDate) &&
                isPeopleGroupOnClassSession(peopleGroupId, scheduledClass, date)
        }
}

/**
 * Submitted class sessions in the current plan period (present or absent).
 * Rolling-plan renewal bills are created after the last session's attendance is taken.
 */
fun countSubmittedPlanSessionsInPeriod(
    peopleGroupId: String,
    periodStartedAtMillis: Long,
    throughDate: LocalDate = LocalDate.now(),
): Int {
    val scheduledClass = ScheduledClassStore.findClassContainingCustomerGroup(peopleGroupId) ?: return 0
    val periodStart = localDateFromMillis(periodStartedAtMillis) ?: return 0
    return computeAllClassSessionDates(scheduledClass, startFrom = periodStart)
        .count { iso ->
            val date = parseIsoLocalDate(iso) ?: return@count false
            if (date.isAfter(throughDate)) return@count false
            if (!isPeopleGroupOnClassSession(peopleGroupId, scheduledClass, date)) return@count false
            val session = ClassSessionKey(scheduledClass.id, iso)
            if (!ClassAttendanceStore.isSessionSubmitted(session)) return@count false
            val groupAttendeeKeys = attendeesForClass(scheduledClass, date)
                .filter { it.peopleGroupId == peopleGroupId }
                .map { it.key }
            groupAttendeeKeys.isNotEmpty() &&
                groupAttendeeKeys.all { key -> ClassAttendanceStore.statusFor(session, key) != null }
        }
}

private fun localDateFromMillis(millis: Long): LocalDate? =
    runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
