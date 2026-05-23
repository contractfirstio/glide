package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.PackEnrollment
import glide.model.PackEnrollmentStatus
import glide.model.PlanSnapshot
import glide.model.parseIsoLocalDate
import glide.model.PeopleGroup
import glide.model.PeopleGroupType
import java.time.ZoneId

object PackEnrollmentStore {
    private val _enrollments = mutableStateListOf<PackEnrollment>()

    val all: List<PackEnrollment> get() = _enrollments

    fun findById(id: String): PackEnrollment? = _enrollments.find { it.id == id }

    fun forPeopleGroup(peopleGroupId: String): PackEnrollment? =
        _enrollments.find { it.peopleGroupId == peopleGroupId && it.status == PackEnrollmentStatus.ACTIVE }

    fun create(enrollment: PackEnrollment) {
        require(forPeopleGroup(enrollment.peopleGroupId) == null) {
            "This customer pack already has an active enrollment."
        }
        _enrollments.add(enrollment)
    }

    fun createForCustomerGroup(group: PeopleGroup, planSnapshot: PlanSnapshot): PackEnrollment? {
        if (group.type != PeopleGroupType.CUSTOMER) return null
        if (forPeopleGroup(group.id) != null) return forPeopleGroup(group.id)
        val startMillis = parseIsoLocalDate(group.planStartDate)
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
            ?: System.currentTimeMillis()
        val enrollment = PackEnrollment(
            peopleGroupId = group.id,
            planSnapshot = planSnapshot,
            startedAtMillis = startMillis,
            packPeriodStartedAtMillis = startMillis,
        )
        create(enrollment)
        return enrollment
    }

    fun resetPackPeriod(enrollmentId: String, startMillis: Long = System.currentTimeMillis()) {
        val index = _enrollments.indexOfFirst { it.id == enrollmentId }
        if (index < 0) return
        _enrollments[index] = _enrollments[index].copy(packPeriodStartedAtMillis = startMillis)
    }
}
