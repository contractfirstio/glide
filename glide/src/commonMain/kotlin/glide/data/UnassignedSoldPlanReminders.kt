package glide.data

import glide.model.PlanEnrollment
import glide.model.PeopleGroupType
import glide.model.isOngoing

data class UnassignedSoldPlan(
    val peopleGroupId: String,
    val customerLabel: String,
    val planName: String,
)

fun findSoldPlansNotAssignedToClass(): List<UnassignedSoldPlan> =
    PlanEnrollmentStore.all
        .asSequence()
        .filter { it.status.isOngoing() }
        .mapNotNull { it.toUnassignedSoldPlanOrNull() }
        .sortedWith(compareBy({ it.customerLabel.lowercase() }, { it.planName.lowercase() }))
        .toList()

fun unassignedSoldPlansMessage(count: Int? = null): String {
    val planCount = count ?: findSoldPlansNotAssignedToClass().size
    return if (planCount == 1) {
        "1 sold plan is not assigned to a class."
    } else {
        "$planCount sold plans are not assigned to a class."
    }
}

fun openUnassignedSoldPlan(unassigned: UnassignedSoldPlan) {
    openSoldPlanClassAssignment(unassigned.peopleGroupId)
}

fun openSoldPlanClassAssignment(peopleGroupId: String) {
    AppViewState.switchTo(AppViewMode.SCHEDULING)
    SchedulePanelState.onSoldPlanSelected(peopleGroupId)
}

fun isSoldPlanAssignedToClass(peopleGroupId: String): Boolean =
    ScheduledClassStore.findClassContainingCustomerGroup(peopleGroupId) != null

fun soldPlanBlocksBillIssuance(peopleGroupId: String): Boolean {
    val enrollment = PlanEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return false
    if (!enrollment.status.isOngoing()) return false
    return !isSoldPlanAssignedToClass(peopleGroupId)
}

fun soldPlanBlocksBillIssuanceMessage(): String =
    "Assign this sold plan to a class before issuing bills."

private fun PlanEnrollment.toUnassignedSoldPlanOrNull(): UnassignedSoldPlan? {
    val group = PeopleGroupStore.findById(peopleGroupId) ?: return null
    if (group.type != PeopleGroupType.CUSTOMER) return null
    if (ScheduledClassStore.findClassContainingCustomerGroup(peopleGroupId) != null) return null
    val main = group.resolveMainClient()
    val customerLabel = main.name.ifBlank { "Customer" }
    val planName = planSnapshot.planName.takeIf { it.isNotBlank() } ?: "Plan"
    return UnassignedSoldPlan(
        peopleGroupId = peopleGroupId,
        customerLabel = customerLabel,
        planName = planName,
    )
}
