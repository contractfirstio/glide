package glide.data

import glide.model.SoldPlanEnrollment
import glide.model.SoldPlanEnrollmentStatus

object RollingPlanCancellationService {
    /** Stops renewal; customer finishes the current plan period on the schedule. */
    fun cancelRenewal(enrollmentId: String): Boolean {
        val enrollment = SoldPlanEnrollmentStore.findById(enrollmentId) ?: return false
        if (!enrollment.planSnapshot.rolling) return false
        if (enrollment.status != SoldPlanEnrollmentStatus.ACTIVE) return false

        SoldPlanEnrollmentStore.setStatus(
            enrollmentId = enrollmentId,
            status = SoldPlanEnrollmentStatus.CANCELLING,
            renewalStoppedAtMillis = System.currentTimeMillis(),
        )
        BillStore.voidScheduledRenewalBills(enrollmentId)
        tryCompleteCancellingEnrollment(enrollmentId)
        return true
    }

    /** When all classes in the period are scheduled, mark the enrollment as fully cancelled. */
    fun tryCompleteCancellingEnrollment(enrollmentId: String): Boolean {
        val enrollment = SoldPlanEnrollmentStore.findById(enrollmentId) ?: return false
        if (enrollment.status != SoldPlanEnrollmentStatus.CANCELLING) return false
        if (!isPlanPeriodScheduledComplete(enrollment)) return false
        SoldPlanEnrollmentStore.setStatus(enrollmentId, SoldPlanEnrollmentStatus.CANCELLED)
        return true
    }

    fun syncCancellingEnrollment(soldPlanId: String) {
        val enrollment = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId) ?: return
        if (enrollment.status == SoldPlanEnrollmentStatus.CANCELLING) {
            tryCompleteCancellingEnrollment(enrollment.id)
        }
    }

    fun isPlanPeriodScheduledComplete(enrollment: SoldPlanEnrollment): Boolean {
        if (!enrollment.planSnapshot.rolling) return false
        val planSize = enrollment.planSnapshot.lessonCount.coerceAtLeast(1)
        val scheduled = countScheduledPlanSessionsInPeriod(
            soldPlanId = enrollment.soldPlanId,
            periodStartedAtMillis = enrollment.planPeriodStartedAtMillis,
        )
        return scheduled >= planSize
    }
}
