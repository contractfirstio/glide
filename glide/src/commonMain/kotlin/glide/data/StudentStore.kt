package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.PeopleGroupType
import glide.model.Student

object StudentStore {
    private val _people = mutableStateListOf<Student>()

    val all: List<Student> get() = _people

    /** Students linked to at least one sold plan. */
    val onSoldPlans: List<Student> get() =
        _people.filter { isOnSoldPlan(it.id) }

    /**
     * Students for the Students panel — all on customer plans, those in
     * [customerGroupId] when a customer group is selected, or everyone linked to [clientId]
     * across that client's leads and customer groups.
     */
    fun forStudentsPanel(
        customerGroupId: String? = null,
        clientId: String? = null,
        planId: String? = null,
        studentId: String? = null,
    ): List<Student> =
        when {
            customerGroupId != null ->
                PeopleGroupStore.findById(customerGroupId)
                    ?.takeIf { it.type == PeopleGroupType.CUSTOMER }
                    ?.resolveStudents()
                    ?: emptyList()
            clientId != null -> studentsForMainClient(clientId)
            planId != null -> studentsForPlan(planId)
            studentId != null ->
                findById(studentId)?.let { listOf(it) } ?: emptyList()
            else -> onSoldPlans
        }

    private fun studentsForPlan(planId: String): List<Student> =
        PeopleGroupStore.all
            .filter { it.planId == planId }
            .flatMap { it.studentIds }
            .distinct()
            .mapNotNull { findById(it) }

    private fun studentsForMainClient(clientId: String): List<Student> =
        PeopleGroupStore.all
            .filter { it.mainClientId == clientId }
            .flatMap { it.studentIds }
            .distinct()
            .mapNotNull { findById(it) }

    fun isOnSoldPlan(personId: String): Boolean =
        PeopleGroupStore.customers.any { personId in it.studentIds }

    fun canDelete(personId: String): Boolean = !isOnSoldPlan(personId)

    fun soldPlanCount(personId: String): Int =
        PeopleGroupStore.customers.count { personId in it.studentIds }

    fun create(person: Student) {
        _people.add(person)
    }

    fun update(person: Student) {
        val index = _people.indexOfFirst { it.id == person.id }
        if (index >= 0) {
            _people[index] = person
        }
    }

    fun delete(id: String) {
        if (PeopleGroupStore.customers.any { id in it.studentIds }) return
        _people.removeAll { it.id == id }
        PeopleGroupStore.removeStudentFromAllGroups(id)
    }

    fun findById(id: String): Student? = _people.find { it.id == id }

    fun peopleGroupsFor(personId: String): List<String> =
        PeopleGroupStore.all
            .filter { personId in it.studentIds }
            .map { it.id }
}
