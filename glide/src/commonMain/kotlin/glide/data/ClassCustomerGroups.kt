package glide.data

import glide.model.PeopleGroupType
import glide.model.ScheduledClass

sealed class AddCustomerGroupResult {
    data object Success : AddCustomerGroupResult()
    data object AlreadyAssigned : AddCustomerGroupResult()
    data object GroupNotFound : AddCustomerGroupResult()
    data object NotACustomerGroup : AddCustomerGroupResult()
    data class CapacityExceeded(
        val currentHeadcount: Int,
        val groupHeadcount: Int,
        val maxCapacity: Int,
    ) : AddCustomerGroupResult()
}

fun headcountForCustomerGroups(customerGroupIds: List<String>): Int =
    customerGroupIds.sumOf { groupId ->
        PeopleGroupStore.findById(groupId)?.classAttendeeCount() ?: 0
    }

fun ScheduledClass.enrolledHeadcount(): Int = headcountForCustomerGroups(customerGroupIds)

fun tryAddCustomerGroup(
    currentGroupIds: List<String>,
    groupId: String,
    locationId: String?,
): AddCustomerGroupResult {
    if (groupId in currentGroupIds) return AddCustomerGroupResult.AlreadyAssigned
    val group = PeopleGroupStore.findById(groupId) ?: return AddCustomerGroupResult.GroupNotFound
    if (group.type != PeopleGroupType.CUSTOMER) return AddCustomerGroupResult.NotACustomerGroup

    val maxCapacity = locationId?.let { LocationStore.findById(it)?.maxCapacity }
    if (maxCapacity != null) {
        val current = headcountForCustomerGroups(currentGroupIds)
        val adding = group.classAttendeeCount()
        if (current + adding > maxCapacity) {
            return AddCustomerGroupResult.CapacityExceeded(
                currentHeadcount = current,
                groupHeadcount = adding,
                maxCapacity = maxCapacity,
            )
        }
    }
    return AddCustomerGroupResult.Success
}

fun AddCustomerGroupResult.toUserMessage(): String = when (this) {
    AddCustomerGroupResult.Success -> "Customer group added to class."
    AddCustomerGroupResult.AlreadyAssigned -> "This group is already on the class."
    AddCustomerGroupResult.GroupNotFound -> "Customer group not found."
    AddCustomerGroupResult.NotACustomerGroup -> "Only customer groups can be assigned to classes."
    is AddCustomerGroupResult.CapacityExceeded ->
        "Room capacity exceeded ($currentHeadcount + $groupHeadcount > $maxCapacity)."
}
