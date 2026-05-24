package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Student

object StudentStore {
    private val _people = mutableStateListOf<Student>()

    val all: List<Student> get() = _people

    /** Students linked to at least one sold plan. */
    val onSoldPlans: List<Student> get() =
        _people.filter { isOnSoldPlan(it.id) }

    /**
     * Students for the Students panel — all on customer plans, those in
     * [soldPlanId] when a sold plan is selected, or everyone linked to [clientId]
     * across that client's leads and sold plans.
     */
    fun forStudentsPanel(
        soldPlanId: String? = null,
        clientId: String? = null,
        planId: String? = null,
        studentId: String? = null,
    ): List<Student> =
        when {
            soldPlanId != null ->
                findSoldPlanById(soldPlanId)
                    
                    ?.resolveStudents()
                    ?: emptyList()
            clientId != null -> studentsForMainClient(clientId)
            planId != null -> studentsForPlan(planId)
            studentId != null ->
                findById(studentId)?.let { listOf(it) } ?: emptyList()
            else -> onSoldPlans
        }

    private fun studentsForPlan(planId: String): List<Student> =
        SoldPlanStore.all
            .filter { it.planId == planId }
            .flatMap { it.studentIds }
            .distinct()
            .mapNotNull { findById(it) }

    private fun studentsForMainClient(clientId: String): List<Student> =
        SoldPlanStore.all
            .filter { it.mainClientId == clientId }
            .flatMap { it.studentIds }
            .distinct()
            .mapNotNull { findById(it) }

    fun isOnSoldPlan(personId: String): Boolean =
        SoldPlanStore.all.any { personId in it.studentIds }

    fun canDelete(personId: String): Boolean = !isOnSoldPlan(personId)

    fun soldPlanCount(personId: String): Int =
        SoldPlanStore.all.count { personId in it.studentIds }

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
        if (SoldPlanStore.all.any { id in it.studentIds }) return
        _people.removeAll { it.id == id }
        LeadStore.removeStudentFromAllLeads(id)
    }

    fun findById(id: String): Student? = _people.find { it.id == id }

    fun soldPlansFor(personId: String): List<String> =
        SoldPlanStore.all
            .filter { personId in it.studentIds }
            .map { it.id }
}
