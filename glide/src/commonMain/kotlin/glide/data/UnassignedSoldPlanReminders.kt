package glide.data

import glide.model.SoldPlanEnrollment
import glide.model.isOngoing

data class UnassignedSoldPlan(
    val soldPlanId: String,
    val customerLabel: String,
    val planName: String,
)

fun findSoldPlansNotAssignedToClass(): List<UnassignedSoldPlan> =
    SoldPlanEnrollmentStore.all
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
    openSoldPlanClassAssignment(unassigned.soldPlanId)
}

fun openSoldPlanClassAssignment(soldPlanId: String) {
    AppViewState.switchTo(AppViewMode.SCHEDULING)
    SchedulePanelState.onSoldPlanSelected(soldPlanId)
}

fun isSoldPlanAssignedToClass(soldPlanId: String): Boolean =
    ClassStore.findClassContainingSoldPlan(soldPlanId) != null

fun soldPlanBlocksBillIssuance(soldPlanId: String): Boolean {
    val enrollment = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId) ?: return false
    if (!enrollment.status.isOngoing()) return false
    return !isSoldPlanAssignedToClass(soldPlanId)
}

fun soldPlanBlocksBillIssuanceMessage(): String =
    "Assign this sold plan to a class before issuing bills."

private fun SoldPlanEnrollment.toUnassignedSoldPlanOrNull(): UnassignedSoldPlan? {
    val group = findSoldPlanById(soldPlanId) ?: return null
    if (ClassStore.findClassContainingSoldPlan(soldPlanId) != null) return null
    val main = group.resolveMainClient()
    val customerLabel = main?.name?.ifBlank { "Customer" } ?: "Customer"
    val planName = planSnapshot.planName.takeIf { it.isNotBlank() } ?: "Plan"
    return UnassignedSoldPlan(
        soldPlanId = soldPlanId,
        customerLabel = customerLabel,
        planName = planName,
    )
}
