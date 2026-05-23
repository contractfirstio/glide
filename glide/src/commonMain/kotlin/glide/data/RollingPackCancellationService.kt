package glide.data

import glide.model.PackEnrollment
import glide.model.PackEnrollmentStatus

object RollingPackCancellationService {
    /** Stops renewal; customer finishes the current pack period on the schedule. */
    fun cancelRenewal(enrollmentId: String): Boolean {
        val enrollment = PackEnrollmentStore.findById(enrollmentId) ?: return false
        if (!enrollment.planSnapshot.rolling) return false
        if (enrollment.status != PackEnrollmentStatus.ACTIVE) return false

        PackEnrollmentStore.setStatus(
            enrollmentId = enrollmentId,
            status = PackEnrollmentStatus.CANCELLING,
            renewalStoppedAtMillis = System.currentTimeMillis(),
        )
        BillStore.voidScheduledRenewalBills(enrollmentId)
        tryCompleteCancellingEnrollment(enrollmentId)
        return true
    }

    /** When all classes in the period are scheduled, mark the enrollment as fully cancelled. */
    fun tryCompleteCancellingEnrollment(enrollmentId: String): Boolean {
        val enrollment = PackEnrollmentStore.findById(enrollmentId) ?: return false
        if (enrollment.status != PackEnrollmentStatus.CANCELLING) return false
        if (!isPackPeriodScheduledComplete(enrollment)) return false
        PackEnrollmentStore.setStatus(enrollmentId, PackEnrollmentStatus.CANCELLED)
        return true
    }

    fun syncCancellingEnrollment(peopleGroupId: String) {
        val enrollment = PackEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return
        if (enrollment.status == PackEnrollmentStatus.CANCELLING) {
            tryCompleteCancellingEnrollment(enrollment.id)
        }
    }

    fun isPackPeriodScheduledComplete(enrollment: PackEnrollment): Boolean {
        if (!enrollment.planSnapshot.rolling) return false
        val packSize = enrollment.planSnapshot.lessonCount.coerceAtLeast(1)
        val scheduled = countScheduledPackSessionsInPeriod(
            peopleGroupId = enrollment.peopleGroupId,
            periodStartedAtMillis = enrollment.packPeriodStartedAtMillis,
        )
        return scheduled >= packSize
    }
}
