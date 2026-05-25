package glide.data

import glide.model.Bill
import glide.model.BillStatus
import glide.model.SoldPlanEnrollment
import glide.model.SoldPlanEnrollmentStatus

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
}

fun Bill.isRenewalBill(): Boolean = description.contains("(renewal)", ignoreCase = true)
