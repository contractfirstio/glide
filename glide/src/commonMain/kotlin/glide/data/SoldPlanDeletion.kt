package glide.data

import glide.model.BillStatus
import glide.model.PeopleGroupType

fun canDeleteSoldPlan(peopleGroupId: String): Boolean =
    soldPlanDeletionBlockReason(peopleGroupId) == null

fun soldPlanDeletionBlockReason(peopleGroupId: String): String? {
    val group = PeopleGroupStore.findById(peopleGroupId) ?: return "Sold plan not found."
    if (group.type != PeopleGroupType.CUSTOMER) return null

    val blockers = buildList {
        if (isSoldPackAssignedToClass(peopleGroupId)) {
            add("assigned to a class")
        }
        if (hasIssuedOrPaidBill(peopleGroupId)) {
            add("has an issued or paid bill")
        }
    }
    return when (blockers.size) {
        0 -> null
        1 -> "This sold plan cannot be deleted because it is ${blockers.single()}."
        else -> "This sold plan cannot be deleted because it is ${blockers.joinToString(" and ")}."
    }
}

fun hasIssuedOrPaidBill(peopleGroupId: String): Boolean =
    BillStore.forPeopleGroup(peopleGroupId).any {
        it.status == BillStatus.ISSUED || it.status == BillStatus.PAID
    }

internal fun purgeSoldPlanData(peopleGroupId: String) {
    ScheduledClassStore.clearCustomerGroupReference(peopleGroupId)
    PackClassScheduleStore.clearForGroup(peopleGroupId)

    val enrollmentIds = PackEnrollmentStore.all
        .filter { it.peopleGroupId == peopleGroupId }
        .map { it.id }
    enrollmentIds.forEach { enrollmentId ->
        BillingCreditStore.removeAllForEnrollment(enrollmentId)
    }
    PackEnrollmentStore.removeAllForPeopleGroup(peopleGroupId)
    BillStore.removeAllForPeopleGroup(peopleGroupId)
    PaymentStore.removeAllForPeopleGroup(peopleGroupId)

    if (BillingPanelState.peopleGroupId == peopleGroupId) {
        BillingPanelState.onCustomerGroupCleared()
    }
    if (SchedulePanelState.selectedSoldPlanId == peopleGroupId) {
        SchedulePanelState.onSoldPlanCleared()
    }
}
