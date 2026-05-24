package glide.data

import glide.model.SoldPlanEnrollmentStatus
import glide.model.PlanSnapshot

object BillingService {
    /** Creates enrollment and a scheduled bill when a lead becomes a customer. */
    fun onCustomerConverted(soldPlanId: String): Boolean {
        val group = findSoldPlanById(soldPlanId) ?: return false
        val planId = group.planId ?: return false
        val plan = PlanStore.findById(planId) ?: return false
        val enrollment = SoldPlanEnrollmentStore.createForSoldPlan(
            soldPlan = group,
            planSnapshot = PlanSnapshot.from(plan),
        ) ?: return false
        if (BillStore.forEnrollment(enrollment.id).isEmpty()) {
            BillStore.createInitialPlanBill(enrollment)
        }
        return true
    }

    fun addRenewalBill(soldPlanId: String): Boolean {
        val enrollment = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId) ?: return false
        if (enrollment.status != SoldPlanEnrollmentStatus.ACTIVE) return false
        BillStore.createRenewalBill(enrollment)
        return true
    }
}
