package glide.data

import glide.model.PackEnrollmentStatus
import glide.model.PlanSnapshot
import glide.model.PeopleGroupType

object BillingService {
    /** Creates enrollment and a scheduled bill when a lead becomes a customer. */
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
            BillStore.createInitialPackBill(enrollment)
        }
        return true
    }

    fun addRenewalBill(peopleGroupId: String): Boolean {
        val enrollment = PackEnrollmentStore.forPeopleGroup(peopleGroupId) ?: return false
        if (enrollment.status != PackEnrollmentStatus.ACTIVE) return false
        BillStore.createRenewalBill(enrollment)
        return true
    }
}
