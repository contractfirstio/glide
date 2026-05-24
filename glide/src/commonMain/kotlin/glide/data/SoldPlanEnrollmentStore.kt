package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.SoldPlanEnrollment
import glide.model.SoldPlanEnrollmentStatus
import glide.model.isOngoing
import glide.model.PlanSnapshot
import glide.model.parseIsoLocalDate
import glide.model.SoldPlan
import java.time.ZoneId

object SoldPlanEnrollmentStore {
    private val _enrollments = mutableStateListOf<SoldPlanEnrollment>()

    val all: List<SoldPlanEnrollment> get() = _enrollments

    fun findById(id: String): SoldPlanEnrollment? = _enrollments.find { it.id == id }

    fun forSoldPlan(soldPlanId: String): SoldPlanEnrollment? =
        _enrollments.find { it.soldPlanId == soldPlanId && it.status.isOngoing() }

    /** Ongoing enrollment, or the most recent one when the plan has finished or been cancelled. */
    fun displayForSoldPlan(soldPlanId: String): SoldPlanEnrollment? =
        forSoldPlan(soldPlanId)
            ?: _enrollments
                .filter { it.soldPlanId == soldPlanId }
                .maxByOrNull { it.startedAtMillis }

    fun create(enrollment: SoldPlanEnrollment) {
        require(forSoldPlan(enrollment.soldPlanId) == null) {
            "This sold plan already has an ongoing enrollment."
        }
        _enrollments.add(enrollment)
    }

    fun setStatus(
        enrollmentId: String,
        status: SoldPlanEnrollmentStatus,
        renewalStoppedAtMillis: Long? = null,
    ): Boolean {
        val index = _enrollments.indexOfFirst { it.id == enrollmentId }
        if (index < 0) return false
        val current = _enrollments[index]
        _enrollments[index] = current.copy(
            status = status,
            renewalStoppedAtMillis = renewalStoppedAtMillis ?: current.renewalStoppedAtMillis,
        )
        return true
    }

    fun createForSoldPlan(soldPlan: SoldPlan, planSnapshot: PlanSnapshot): SoldPlanEnrollment? {
        if (forSoldPlan(soldPlan.id) != null) return forSoldPlan(soldPlan.id)
        val startMillis = parseIsoLocalDate(soldPlan.planStartDate)
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
            ?: System.currentTimeMillis()
        val enrollment = SoldPlanEnrollment(
            soldPlanId = soldPlan.id,
            planSnapshot = planSnapshot,
            startedAtMillis = startMillis,
            planPeriodStartedAtMillis = startMillis,
        )
        create(enrollment)
        return enrollment
    }

    fun resetPlanPeriod(enrollmentId: String, startMillis: Long = System.currentTimeMillis()) {
        val index = _enrollments.indexOfFirst { it.id == enrollmentId }
        if (index < 0) return
        _enrollments[index] = _enrollments[index].copy(planPeriodStartedAtMillis = startMillis)
    }

    fun removeAllForSoldPlan(soldPlanId: String) {
        _enrollments.removeAll { it.soldPlanId == soldPlanId }
    }
}
