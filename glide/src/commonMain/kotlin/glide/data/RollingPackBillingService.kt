package glide.data

import glide.model.Bill
import glide.model.BillStatus
import glide.model.PackEnrollment
import glide.model.PackEnrollmentStatus

object RollingPackBillingService {
    /**
     * Schedules a renewal bill when the pack is down to the last scheduled class in the period.
     * Based on calendar class count (plan lesson count), not attendance.
     */
    fun ensureRenewalBillIfPackEnding(peopleGroupId: String): Boolean {
        val enrollment = PackEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return false
        if (!enrollment.planSnapshot.rolling) return false
        return ensureRenewalBillIfPackEnding(enrollment)
    }

    fun ensureRenewalBillIfPackEnding(enrollment: PackEnrollment): Boolean {
        if (!enrollment.planSnapshot.rolling) return false
        val packSize = enrollment.planSnapshot.lessonCount.coerceAtLeast(1)
        val elapsed = countScheduledPackSessionsInPeriod(
            peopleGroupId = enrollment.peopleGroupId,
            periodStartedAtMillis = enrollment.packPeriodStartedAtMillis,
        )
        val remaining = packSize - elapsed
        if (remaining > 1) return false
        if (BillStore.hasScheduledRenewalBill(enrollment.id)) return false
        BillStore.createRenewalBill(enrollment)
        return true
    }

    fun syncRollingPackBilling(peopleGroupId: String) {
        ensureRenewalBillIfPackEnding(peopleGroupId)
    }

    fun syncAllActiveRollingPackBilling() {
        PackEnrollmentStore.all
            .filter { it.status == PackEnrollmentStatus.ACTIVE && it.planSnapshot.rolling }
            .forEach { ensureRenewalBillIfPackEnding(it) }
    }

    fun onPackBillPaid(enrollmentId: String, paidAtMillis: Long) {
        PackEnrollmentStore.resetPackPeriod(enrollmentId, paidAtMillis)
    }
}

fun Bill.isRenewalBill(): Boolean = description.contains("(renewal)", ignoreCase = true)
