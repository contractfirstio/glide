package glide.billing

import glide.data.BillStore
import glide.data.LocationStore
import glide.data.SoldPlanClassScheduleStore
import glide.data.SoldPlanEnrollmentStore
import glide.data.SoldPlanStore
import glide.data.ClassStore
import glide.data.computeAllClassSessionDates
import glide.data.soldPlanPlanPeriodStartDate
import glide.data.requiredClassSessionsForPlan
import glide.data.rosterNameLabels
import glide.data.findSoldPlanById
import glide.model.Bill
import glide.model.BillStatus
import glide.model.Class
import glide.model.formatInvoiceClassSessionLabel
import glide.model.formatScheduleIsoDate
import glide.model.parseIsoLocalDate
import glide.model.scheduleLine
import java.time.LocalDate

data class InvoiceClassSchedule(
    val className: String,
    val classDetails: String,
    val locationAddressLines: List<String>,
    val studentNamesLabel: String,
    val billingWindowStartLabel: String,
    val scheduledSessionLabels: List<String>,
)

fun Bill.toInvoiceClassSchedule(): InvoiceClassSchedule? {
    val group = findSoldPlanById(soldPlanId) ?: return null
    val scheduledClass = ClassStore.findClassContainingSoldPlan(soldPlanId) ?: return null
    if (SoldPlanEnrollmentStore.findById(enrollmentId) == null) return null

    val location = scheduledClass.locationId?.let { LocationStore.findById(it) }
    val locationName = location?.name?.takeIf { name -> name.isNotBlank() }
    val classDetails = buildString {
        append(scheduledClass.scheduleLine())
        locationName?.let { append(" · ").append(it) }
    }
    val locationAddressLines = location?.formattedAddressLines().orEmpty()

    val periodStart = soldPlanPlanPeriodStartDate(soldPlanId)
    val sessionDates = planBillClassSessionDates(soldPlanId, scheduledClass, periodStart)
    val scheduledSessionLabels = sessionDates
        .map { formatInvoiceClassSessionLabel(it, scheduledClass) }
        .filter { it.isNotBlank() }

    return InvoiceClassSchedule(
        className = scheduledClass.name,
        classDetails = classDetails,
        locationAddressLines = locationAddressLines,
        studentNamesLabel = group.rosterNameLabels().joinToString(", ").ifBlank { "—" },
        billingWindowStartLabel = formatScheduleIsoDate(periodStart.toString()),
        scheduledSessionLabels = scheduledSessionLabels,
    )
}

private fun Bill.planBillClassSessionDates(
    soldPlanId: String,
    scheduledClass: Class,
    periodStart: LocalDate,
): List<String> {
    val enrollment = SoldPlanEnrollmentStore.findById(enrollmentId) ?: return emptyList()
    val sessionCount = requiredClassSessionsForPlan(enrollment.planSnapshot)
    val billIndex = planBillIndex()

    val storedDates = SoldPlanClassScheduleStore.sessionDatesFor(soldPlanId, scheduledClass.id)
    if (billIndex == 0 && storedDates != null) {
        return storedDates
            .filter { iso -> parseIsoLocalDate(iso)?.let { !it.isBefore(periodStart) } == true }
            .take(sessionCount)
    }

    val skip = billIndex * sessionCount
    return computeAllClassSessionDates(scheduledClass, startFrom = periodStart)
        .drop(skip)
        .take(sessionCount)
}

private fun Bill.planBillIndex(): Int {
    val index = BillStore.forEnrollment(enrollmentId)
        .filter { it.status != BillStatus.VOID }
        .sortedBy { it.createdAtMillis }
        .indexOfFirst { it.id == id }
    return if (index < 0) 0 else index
}
