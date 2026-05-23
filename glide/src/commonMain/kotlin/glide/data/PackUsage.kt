package glide.data

import glide.model.parseIsoLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Class sessions on the customer's calendar in the current pack period (not attendance).
 * Each scheduled class date counts toward the plan's class count; absences are handled via credits.
 */
fun countScheduledPackSessionsInPeriod(
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

private fun localDateFromMillis(millis: Long): LocalDate? =
    runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
