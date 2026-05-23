package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.ClassGroupAssignment
import glide.model.PeopleGroupType

sealed class AssignGroupResult {
    data object Success : AssignGroupResult()
    data object AlreadyAssigned : AssignGroupResult()
    data object ClassNotFound : AssignGroupResult()
    data object GroupNotFound : AssignGroupResult()
    data object NotACustomerGroup : AssignGroupResult()
    data class CapacityExceeded(
        val currentHeadcount: Int,
        val groupHeadcount: Int,
        val maxCapacity: Int,
    ) : AssignGroupResult()
}

object ClassGroupAssignmentStore {
    private val _assignments = mutableStateListOf<ClassGroupAssignment>()

    val assignments: List<ClassGroupAssignment> get() = _assignments

    fun assign(classId: String, peopleGroupId: String): AssignGroupResult {
        val scheduledClass = ScheduledClassStore.findById(classId) ?: return AssignGroupResult.ClassNotFound
        val group = PeopleGroupStore.findById(peopleGroupId) ?: return AssignGroupResult.GroupNotFound
        if (group.type != PeopleGroupType.CUSTOMER) return AssignGroupResult.NotACustomerGroup
        if (_assignments.any { it.classId == classId && it.peopleGroupId == peopleGroupId }) {
            return AssignGroupResult.AlreadyAssigned
        }

        val maxCapacity = scheduledClass.locationId?.let { LocationStore.findById(it)?.maxCapacity }
        if (maxCapacity != null) {
            val current = headcountForClass(classId)
            val adding = group.memberCount()
            if (current + adding > maxCapacity) {
                return AssignGroupResult.CapacityExceeded(
                    currentHeadcount = current,
                    groupHeadcount = adding,
                    maxCapacity = maxCapacity,
                )
            }
        }

        _assignments.add(
            ClassGroupAssignment(
                classId = classId,
                peopleGroupId = peopleGroupId,
            ),
        )
        return AssignGroupResult.Success
    }

    fun unassign(classId: String, peopleGroupId: String): Boolean {
        return _assignments.removeAll { it.classId == classId && it.peopleGroupId == peopleGroupId }
    }

    fun groupsForClass(classId: String): List<String> =
        _assignments.filter { it.classId == classId }.map { it.peopleGroupId }

    fun headcountForClass(classId: String): Int =
        groupsForClass(classId).sumOf { groupId ->
            PeopleGroupStore.findById(groupId)?.memberCount() ?: 0
        }

    fun clearForClass(classId: String) {
        _assignments.removeAll { it.classId == classId }
    }

    fun clearForGroup(peopleGroupId: String) {
        _assignments.removeAll { it.peopleGroupId == peopleGroupId }
    }

    internal fun seed(assignment: ClassGroupAssignment) {
        _assignments.add(assignment)
    }
}

fun AssignGroupResult.toUserMessage(): String = when (this) {
    AssignGroupResult.Success -> "Customer group added to class."
    AssignGroupResult.AlreadyAssigned -> "This group is already on the class."
    AssignGroupResult.ClassNotFound -> "Class not found."
    AssignGroupResult.GroupNotFound -> "Customer group not found."
    AssignGroupResult.NotACustomerGroup -> "Only customer groups can be assigned to classes."
    is AssignGroupResult.CapacityExceeded ->
        "Room capacity exceeded ($currentHeadcount + $groupHeadcount > $maxCapacity)."
}

