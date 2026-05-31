package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Student

object StudentStore {
    private val _people = mutableStateListOf<Student>()

    val all: List<Student> get() = _people

    private fun syntheticNamePattern(prefix: String): Regex =
        Regex("^${Regex.escape(prefix)} Student (\\d+)$")

    /** Next available placeholder names for a main contact, e.g. Jane Smith Student 1. */
    fun nextSyntheticNames(count: Int, mainContactName: String): List<String> {
        require(count > 0) { "count must be positive" }
        val prefix = mainContactName.trim().replace(Regex("\\s+"), " ")
        require(prefix.isNotBlank()) { "mainContactName must not be blank" }
        val pattern = syntheticNamePattern(prefix)
        val usedNumbers = all.mapNotNull { person ->
            pattern.matchEntire(person.name.trim())?.groupValues?.get(1)?.toIntOrNull()
        }.toMutableSet()
        val names = mutableListOf<String>()
        var candidate = 1
        while (names.size < count) {
            if (candidate !in usedNumbers) {
                names.add("$prefix Student $candidate")
                usedNumbers.add(candidate)
            }
            candidate++
        }
        return names
    }

    /** Creates [count] students with generated placeholder names and returns them. */
    fun createSynthetic(count: Int, mainContactName: String): List<Student> =
        nextSyntheticNames(count, mainContactName).map { name ->
            Student(name = name).also(::create)
        }

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
        persistAppData()
    }

    internal fun replaceAll(students: List<Student>) {
        _people.clear()
        _people.addAll(students)
    }

    fun update(person: Student) {
        val index = _people.indexOfFirst { it.id == person.id }
        if (index >= 0) {
            _people[index] = person
            persistAppData()
        }
    }

    fun delete(id: String) {
        if (SoldPlanStore.all.any { id in it.studentIds }) return
        val removed = _people.removeAll { it.id == id }
        if (removed) {
            LeadStore.removeStudentFromAllLeads(id)
            persistAppData()
        }
    }

    fun findById(id: String): Student? = _people.find { it.id == id }

    fun soldPlansFor(personId: String): List<String> =
        SoldPlanStore.all
            .filter { personId in it.studentIds }
            .map { it.id }
}
