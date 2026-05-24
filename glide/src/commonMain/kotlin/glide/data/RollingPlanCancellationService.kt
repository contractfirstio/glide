package glide.data

import glide.model.PlanEnrollment
import glide.model.PlanEnrollmentStatus

object RollingPlanCancellationService {
    /** Stops renewal; customer finishes the current plan period on the schedule. */
    fun cancelRenewal(enrollmentId: String): Boolean {
        val enrollment = PlanEnrollmentStore.findById(enrollmentId) ?: return false
        if (!enrollment.planSnapshot.rolling) return false
        if (enrollment.status != PlanEnrollmentStatus.ACTIVE) return false

        PlanEnrollmentStore.setStatus(
            enrollmentId = enrollmentId,
            status = PlanEnrollmentStatus.CANCELLING,
            renewalStoppedAtMillis = System.currentTimeMillis(),
        )
        BillStore.voidScheduledRenewalBills(enrollmentId)
        tryCompleteCancellingEnrollment(enrollmentId)
        return true
    }

    /** When all classes in the period are scheduled, mark the enrollment as fully cancelled. */
    fun tryCompleteCancellingEnrollment(enrollmentId: String): Boolean {
        val enrollment = PlanEnrollmentStore.findById(enrollmentId) ?: return false
        if (enrollment.status != PlanEnrollmentStatus.CANCELLING) return false
        if (!isPlanPeriodScheduledComplete(enrollment)) return false
        PlanEnrollmentStore.setStatus(enrollmentId, PlanEnrollmentStatus.CANCELLED)
        return true
    }

    fun syncCancellingEnrollment(peopleGroupId: String) {
        val enrollment = PlanEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return
        if (enrollment.status == PlanEnrollmentStatus.CANCELLING) {
            tryCompleteCancellingEnrollment(enrollment.id)
        }
    }

    fun isPlanPeriodScheduledComplete(enrollment: PlanEnrollment): Boolean {
        if (!enrollment.planSnapshot.rolling) return false
        val planSize = enrollment.planSnapshot.lessonCount.coerceAtLeast(1)
        val scheduled = countScheduledPlanSessionsInPeriod(
            peopleGroupId = enrollment.peopleGroupId,
            periodStartedAtMillis = enrollment.planPeriodStartedAtMillis,
        )
        return scheduled >= planSize
    }
}
