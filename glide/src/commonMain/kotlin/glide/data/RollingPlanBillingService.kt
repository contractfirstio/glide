package glide.data

import glide.model.Bill
import glide.model.BillStatus
import glide.model.SoldPlanEnrollment
import glide.model.SoldPlanEnrollmentStatus
import glide.model.parseIsoLocalDate
import java.time.Instant
import java.time.ZoneId

object RollingPlanBillingService {
    /**
     * Schedules a renewal bill after attendance for the final class in the current plan period
     * has been submitted.
     */
    fun ensureRenewalBillIfPlanEnding(soldPlanId: String): Boolean {
        val enrollment = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId) ?: return false
        if (!enrollment.planSnapshot.rolling) return false
        return ensureRenewalBillIfPlanEnding(enrollment)
    }

    fun ensureRenewalBillIfPlanEnding(enrollment: SoldPlanEnrollment): Boolean {
        if (enrollment.status != SoldPlanEnrollmentStatus.ACTIVE) return false
        if (!enrollment.planSnapshot.rolling) return false
        val planSize = enrollment.planSnapshot.lessonCount.coerceAtLeast(1)
        val submitted = countSubmittedPlanSessionsInPeriod(
            soldPlanId = enrollment.soldPlanId,
            periodStartedAtMillis = enrollment.planPeriodStartedAtMillis,
        )
        if (submitted < planSize) return false
        if (BillStore.hasScheduledRenewalBill(enrollment.id)) return false
        if (!hasFullWindowForNextBill(enrollment, planSize)) return false
        BillStore.createRenewalBill(enrollment)
        return true
    }

    fun syncRollingPlanBilling(soldPlanId: String) {
        val enrollment = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId) ?: return
        if (enrollment.status == SoldPlanEnrollmentStatus.CANCELLING) {
            RollingPlanCancellationService.tryCompleteCancellingEnrollment(enrollment.id)
            return
        }
        if (enrollment.status != SoldPlanEnrollmentStatus.ACTIVE) return
        ensureRenewalBillIfPlanEnding(soldPlanId)
    }

    fun syncAllActiveRollingPlanBilling() {
        SoldPlanEnrollmentStore.all
            .filter { it.status == SoldPlanEnrollmentStatus.ACTIVE && it.planSnapshot.rolling }
            .forEach { ensureRenewalBillIfPlanEnding(it) }
    }

    fun onPlanBillPaid(enrollmentId: String, paidAtMillis: Long) {
        SoldPlanEnrollmentStore.resetPlanPeriod(enrollmentId, paidAtMillis)
    }

    /**
     * Guard renewal billing: only create the next bill when a full plan-sized session window exists.
     * This prevents issuing a 10-class renewal that can only list part of the dates (e.g. 5).
     */
    private fun hasFullWindowForNextBill(enrollment: SoldPlanEnrollment, planSize: Int): Boolean {
        val scheduledClass = ClassStore.findClassContainingSoldPlan(enrollment.soldPlanId) ?: return false
        val periodStart = localDateFromMillis(enrollment.planPeriodStartedAtMillis) ?: return false
        val nextBillIndex = BillStore.forEnrollment(enrollment.id)
            .count { it.status != BillStatus.VOID }
        val available = computeAllClassSessionDates(scheduledClass, startFrom = periodStart)
            .filter { iso ->
                val date = parseIsoLocalDate(iso) ?: return@filter false
                isSoldPlanOnClassSession(enrollment.soldPlanId, scheduledClass, date)
            }
            .drop(nextBillIndex * planSize)
            .size
        return available >= planSize
    }

    private fun localDateFromMillis(millis: Long) =
        runCatching {
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        }.getOrNull()
}

fun Bill.isRenewalBill(): Boolean = description.contains("(renewal)", ignoreCase = true)
