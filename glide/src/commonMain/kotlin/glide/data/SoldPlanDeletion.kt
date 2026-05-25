package glide.data

import glide.model.BillStatus

fun canDeleteSoldPlan(soldPlanId: String): Boolean =
    soldPlanDeletionBlockReason(soldPlanId) == null

fun soldPlanDeletionBlockReason(soldPlanId: String): String? {
    val group = findSoldPlanById(soldPlanId) ?: return "Sold plan not found."

    val blockers = buildList {
        if (isSoldPlanAssignedToClass(soldPlanId)) {
            add("assigned to a class")
        }
        if (hasIssuedOrPaidBill(soldPlanId)) {
            add("has an issued or paid bill")
        }
    }
    return when (blockers.size) {
        0 -> null
        1 -> "This sold plan cannot be deleted because it is ${blockers.single()}."
        else -> "This sold plan cannot be deleted because it is ${blockers.joinToString(" and ")}."
    }
}

fun hasIssuedOrPaidBill(soldPlanId: String): Boolean =
    BillStore.forSoldPlan(soldPlanId).any {
        it.status == BillStatus.ISSUED || it.status == BillStatus.PAID
    }

internal fun purgeSoldPlanData(soldPlanId: String) {
    ClassStore.clearSoldPlanReference(soldPlanId)
    SoldPlanClassScheduleStore.clearForSoldPlan(soldPlanId)

    val enrollmentIds = SoldPlanEnrollmentStore.all
        .filter { it.soldPlanId == soldPlanId }
        .map { it.id }
    enrollmentIds.forEach { enrollmentId ->
        AttendanceCreditStore.removeAllForEnrollment(enrollmentId)
    }
    SoldPlanEnrollmentStore.removeAllForSoldPlan(soldPlanId)
    BillStore.removeAllForSoldPlan(soldPlanId)
    PaymentStore.removeAllForSoldPlan(soldPlanId)

    if (BillingPanelState.soldPlanId == soldPlanId) {
        BillingPanelState.onSoldPlanCleared()
    }
    if (SchedulePanelState.selectedSoldPlanId == soldPlanId) {
        SchedulePanelState.onSoldPlanCleared()
    }
}
