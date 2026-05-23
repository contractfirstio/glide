package glide.billing

import glide.data.LocationStore
import glide.data.PackClassScheduleStore
import glide.data.PackEnrollmentStore
import glide.data.PeopleGroupStore
import glide.data.ScheduledClassStore
import glide.data.computeAllClassSessionDates
import glide.data.computeClassSessionDates
import glide.data.peopleGroupPackPeriodStartDate
import glide.data.rosterNameLabels
import glide.data.sessionLimitForPeopleGroup
import glide.model.Bill
import glide.model.ScheduledClass
import glide.model.formatScheduleIsoDate
import glide.model.parseIsoLocalDate
import glide.model.scheduleLine
import java.time.LocalDate

data class InvoiceClassSchedule(
    val className: String,
    val classDetails: String,
    val studentNamesLabel: String,
    val billingWindowStartLabel: String,
    val firstScheduledSessionLabel: String,
)

fun Bill.toInvoiceClassSchedule(): InvoiceClassSchedule? {
    val group = PeopleGroupStore.findById(peopleGroupId) ?: return null
    val scheduledClass = ScheduledClassStore.findClassContainingCustomerGroup(peopleGroupId) ?: return null
    if (PackEnrollmentStore.findById(enrollmentId) == null) return null

    val locationName = scheduledClass.locationId
        ?.let { LocationStore.findById(it)?.name?.takeIf { name -> name.isNotBlank() } }
    val classDetails = buildString {
        append(scheduledClass.scheduleLine())
        locationName?.let { append(" · ").append(it) }
    }

    val periodStart = peopleGroupPackPeriodStartDate(peopleGroupId)
    val sessionDates = scheduledSessionDatesForPackPeriod(peopleGroupId, scheduledClass, periodStart)
    val firstSession = sessionDates.firstOrNull()?.let { formatScheduleIsoDate(it) }

    return InvoiceClassSchedule(
        className = scheduledClass.name,
        classDetails = classDetails,
        studentNamesLabel = group.rosterNameLabels().joinToString(", ").ifBlank { "—" },
        billingWindowStartLabel = formatScheduleIsoDate(periodStart.toString()),
        firstScheduledSessionLabel = firstSession ?: "Not scheduled yet",
    )
}

private fun scheduledSessionDatesForPackPeriod(
    peopleGroupId: String,
    scheduledClass: ScheduledClass,
    periodStart: LocalDate,
): List<String> {
    PackClassScheduleStore.sessionDatesFor(peopleGroupId, scheduledClass.id)?.let { dates ->
        return dates.filter { iso ->
            parseIsoLocalDate(iso)?.let { !it.isBefore(periodStart) } == true
        }
    }
    val limit = sessionLimitForPeopleGroup(peopleGroupId)
    return if (limit != null) {
        computeClassSessionDates(scheduledClass, limit, startFrom = periodStart)
    } else {
        computeAllClassSessionDates(scheduledClass, startFrom = periodStart)
    }
}
