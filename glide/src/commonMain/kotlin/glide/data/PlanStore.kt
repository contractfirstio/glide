package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Plan

object PlanStore {
    private val _plans = mutableStateListOf<Plan>()
    val plans: List<Plan> get() = _plans

    fun create(plan: Plan) {
        _plans.add(plan)
    }

    fun update(plan: Plan): Boolean {
        if (!canEdit(plan.id)) return false
        val index = _plans.indexOfFirst { it.id == plan.id }
        if (index < 0) return false
        _plans[index] = plan
        return true
    }

    fun soldPlanCount(planId: String): Int =
        PeopleGroupStore.customers.count { it.planId == planId }

    fun canEdit(planId: String): Boolean = soldPlanCount(planId) == 0

    fun canDelete(planId: String): Boolean = canEdit(planId)

    private fun planSoldBlockReason(planId: String, action: String): String? {
        val soldCount = soldPlanCount(planId)
        return if (soldCount == 0) {
            null
        } else {
            "This plan has been sold on $soldCount sold plan${if (soldCount == 1) "" else "s"} " +
                "and cannot be $action."
        }
    }

    fun planEditBlockReason(planId: String): String? = planSoldBlockReason(planId, "edited")

    fun planDeletionBlockReason(planId: String): String? = planSoldBlockReason(planId, "deleted")

    fun delete(id: String): Boolean {
        if (!canDelete(id)) return false
        _plans.removeAll { it.id == id }
        return true
    }

    fun findById(id: String): Plan? = _plans.find { it.id == id }

    /**
     * Plans for the Plans panel — all plans, the plan on [customerGroupId], or plans used on
     * groups linked to [clientId] or [relatedPersonId].
     */
    fun forPlansPanel(
        customerGroupId: String? = null,
        clientId: String? = null,
        relatedPersonId: String? = null,
    ): List<Plan> {
        val planIds = when {
            customerGroupId != null ->
                PeopleGroupStore.findById(customerGroupId)
                    ?.planId
                    ?.let { listOf(it) }
                    ?: emptyList()
            clientId != null ->
                PeopleGroupStore.all
                    .filter { it.mainClientId == clientId }
                    .mapNotNull { it.planId }
                    .distinct()
            relatedPersonId != null ->
                PeopleGroupStore.all
                    .filter { relatedPersonId in it.relatedPersonIds }
                    .mapNotNull { it.planId }
                    .distinct()
            else -> return plans
        }
        return planIds.mapNotNull { findById(it) }
    }
}
