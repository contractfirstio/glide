package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.PlanEnrollment
import glide.model.PlanEnrollmentStatus
import glide.model.isOngoing
import glide.model.PlanSnapshot
import glide.model.parseIsoLocalDate
import glide.model.PeopleGroup
import glide.model.PeopleGroupType
import java.time.ZoneId

object PlanEnrollmentStore {
    private val _enrollments = mutableStateListOf<PlanEnrollment>()

    val all: List<PlanEnrollment> get() = _enrollments

    fun findById(id: String): PlanEnrollment? = _enrollments.find { it.id == id }

    fun forPeopleGroup(peopleGroupId: String): PlanEnrollment? =
        _enrollments.find { it.peopleGroupId == peopleGroupId && it.status.isOngoing() }

    /** Ongoing enrollment, or the most recent one when the plan has finished or been cancelled. */
    fun displayForPeopleGroup(peopleGroupId: String): PlanEnrollment? =
        forPeopleGroup(peopleGroupId)
            ?: _enrollments
                .filter { it.peopleGroupId == peopleGroupId }
                .maxByOrNull { it.startedAtMillis }

    fun create(enrollment: PlanEnrollment) {
        require(forPeopleGroup(enrollment.peopleGroupId) == null) {
            "This customer plan already has an ongoing enrollment."
        }
        _enrollments.add(enrollment)
    }

    fun setStatus(
        enrollmentId: String,
        status: PlanEnrollmentStatus,
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

    fun createForCustomerGroup(group: PeopleGroup, planSnapshot: PlanSnapshot): PlanEnrollment? {
        if (group.type != PeopleGroupType.CUSTOMER) return null
        if (forPeopleGroup(group.id) != null) return forPeopleGroup(group.id)
        val startMillis = parseIsoLocalDate(group.planStartDate)
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
            ?: System.currentTimeMillis()
        val enrollment = PlanEnrollment(
            peopleGroupId = group.id,
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

    fun removeAllForPeopleGroup(peopleGroupId: String) {
        _enrollments.removeAll { it.peopleGroupId == peopleGroupId }
    }
}
