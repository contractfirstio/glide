package glide.data

import glide.model.PlanSnapshot
import glide.model.PeopleGroup
import glide.model.PeopleGroupType

object BillingService {
    /** Creates enrollment and first bill when a lead becomes a customer. */
    fun onCustomerConverted(peopleGroupId: String): Boolean {
        val group = PeopleGroupStore.findById(peopleGroupId) ?: return false
        if (group.type != PeopleGroupType.CUSTOMER) return false
        val planId = group.planId ?: return false
        val plan = PlanStore.findById(planId) ?: return false
        val enrollment = PackEnrollmentStore.createForCustomerGroup(
            group = group,
            planSnapshot = PlanSnapshot.from(plan),
        ) ?: return false
        if (BillStore.forEnrollment(enrollment.id).isEmpty()) {
            BillStore.issueInitialPackBill(enrollment)
        }
        return true
    }

    fun issueRenewalBill(peopleGroupId: String): Boolean {
        val enrollment = PackEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return false
        BillStore.issuePackBill(enrollment)
        return true
    }
}
