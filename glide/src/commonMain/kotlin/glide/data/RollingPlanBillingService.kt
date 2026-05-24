package glide.data

import glide.model.Bill
import glide.model.BillStatus
import glide.model.PlanEnrollment
import glide.model.PlanEnrollmentStatus

object RollingPlanBillingService {
    /**
     * Schedules a renewal bill when the plan is down to the last scheduled class in the period.
     * Based on calendar class count (plan lesson count), not attendance.
     */
    fun ensureRenewalBillIfPlanEnding(peopleGroupId: String): Boolean {
        val enrollment = PlanEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return false
        if (!enrollment.planSnapshot.rolling) return false
        return ensureRenewalBillIfPlanEnding(enrollment)
    }

    fun ensureRenewalBillIfPlanEnding(enrollment: PlanEnrollment): Boolean {
        if (enrollment.status != PlanEnrollmentStatus.ACTIVE) return false
        if (!enrollment.planSnapshot.rolling) return false
        val planSize = enrollment.planSnapshot.lessonCount.coerceAtLeast(1)
        val elapsed = countScheduledPlanSessionsInPeriod(
            peopleGroupId = enrollment.peopleGroupId,
            periodStartedAtMillis = enrollment.planPeriodStartedAtMillis,
        )
        val remaining = planSize - elapsed
        if (remaining > 1) return false
        if (BillStore.hasScheduledRenewalBill(enrollment.id)) return false
        BillStore.createRenewalBill(enrollment)
        return true
    }

    fun syncRollingPlanBilling(peopleGroupId: String) {
        val enrollment = PlanEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return
        if (enrollment.status == PlanEnrollmentStatus.CANCELLING) {
            RollingPlanCancellationService.tryCompleteCancellingEnrollment(enrollment.id)
            return
        }
        if (enrollment.status != PlanEnrollmentStatus.ACTIVE) return
        ensureRenewalBillIfPlanEnding(peopleGroupId)
    }

    fun syncAllActiveRollingPlanBilling() {
        PlanEnrollmentStore.all
            .filter { it.status == PlanEnrollmentStatus.ACTIVE && it.planSnapshot.rolling }
            .forEach { ensureRenewalBillIfPlanEnding(it) }
    }

    fun onPlanBillPaid(enrollmentId: String, paidAtMillis: Long) {
        PlanEnrollmentStore.resetPlanPeriod(enrollmentId, paidAtMillis)
    }
}

fun Bill.isRenewalBill(): Boolean = description.contains("(renewal)", ignoreCase = true)
