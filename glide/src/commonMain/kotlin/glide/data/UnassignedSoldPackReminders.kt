package glide.data

import glide.model.PackEnrollment
import glide.model.PeopleGroupType
import glide.model.isOngoing

data class UnassignedSoldPack(
    val peopleGroupId: String,
    val customerLabel: String,
    val packName: String,
)

fun findSoldPacksNotAssignedToClass(): List<UnassignedSoldPack> =
    PackEnrollmentStore.all
        .asSequence()
        .filter { it.status.isOngoing() }
        .mapNotNull { it.toUnassignedSoldPackOrNull() }
        .sortedWith(compareBy({ it.customerLabel.lowercase() }, { it.packName.lowercase() }))
        .toList()

fun unassignedSoldPacksMessage(count: Int? = null): String {
    val packCount = count ?: findSoldPacksNotAssignedToClass().size
    return if (packCount == 1) {
        "1 sold pack is not assigned to a class."
    } else {
        "$packCount sold packs are not assigned to a class."
    }
}

fun openUnassignedSoldPack(unassigned: UnassignedSoldPack) {
    openSoldPackClassAssignment(unassigned.peopleGroupId)
}

fun openSoldPackClassAssignment(peopleGroupId: String) {
    AppViewState.switchTo(AppViewMode.SCHEDULING)
    SchedulePanelState.onSoldPlanSelected(peopleGroupId)
}

fun isSoldPackAssignedToClass(peopleGroupId: String): Boolean =
    ScheduledClassStore.findClassContainingCustomerGroup(peopleGroupId) != null

fun soldPackBlocksBillIssuance(peopleGroupId: String): Boolean {
    val enrollment = PackEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return false
    if (!enrollment.status.isOngoing()) return false
    return !isSoldPackAssignedToClass(peopleGroupId)
}

fun soldPackBlocksBillIssuanceMessage(): String =
    "Assign this sold pack to a class before issuing bills."

private fun PackEnrollment.toUnassignedSoldPackOrNull(): UnassignedSoldPack? {
    val group = PeopleGroupStore.findById(peopleGroupId) ?: return null
    if (group.type != PeopleGroupType.CUSTOMER) return null
    if (ScheduledClassStore.findClassContainingCustomerGroup(peopleGroupId) != null) return null
    val main = group.resolveMainContact()
    val customerLabel = main.name.ifBlank { "Customer" }
    val packName = planSnapshot.planName.takeIf { it.isNotBlank() } ?: "Pack"
    return UnassignedSoldPack(
        peopleGroupId = peopleGroupId,
        customerLabel = customerLabel,
        packName = packName,
    )
}
