package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.debug.GlidePanelDebug
import glide.model.Plan

object PlanStore {
    private val _plans = mutableStateListOf<Plan>()
    val plans: List<Plan> get() = _plans

    fun create(plan: Plan) {
        _plans.add(plan)
        GlidePanelDebug.log(
            GlidePanelDebug.Panel.PLANS,
            "PlanStore.create",
            "id=${plan.id} name=${plan.name} totalPlans=${_plans.size} || ${GlidePanelDebug.globalSnapshot()}",
        )
        persistAppData()
    }

    internal fun replaceAll(plans: List<Plan>) {
        _plans.clear()
        _plans.addAll(plans)
    }

    fun update(plan: Plan): Boolean {
        if (!canEdit(plan.id)) return false
        val index = _plans.indexOfFirst { it.id == plan.id }
        if (index < 0) return false
        _plans[index] = plan
        persistAppData()
        return true
    }

    fun soldPlanCount(planId: String): Int =
        SoldPlanStore.all.count { it.planId == planId }

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
        persistAppData()
        return true
    }

    fun findById(id: String): Plan? = _plans.find { it.id == id }

    /**
     * Plans for the Plans panel — all plans, the plan on [soldPlanId], or plans used on
     * groups linked to [clientId] or [studentId].
     */
    fun forPlansPanel(
        soldPlanId: String? = null,
        clientId: String? = null,
        studentId: String? = null,
    ): List<Plan> {
        val planIds = when {
            soldPlanId != null ->
                findSoldPlanById(soldPlanId)
                    ?.planId
                    ?.let { listOf(it) }
                    ?: emptyList()
            clientId != null ->
                SoldPlanStore.all
                    .filter { it.mainClientId == clientId }
                    .mapNotNull { it.planId }
                    .distinct()
            studentId != null ->
                SoldPlanStore.all
                    .filter { studentId in it.studentIds }
                    .mapNotNull { it.planId }
                    .distinct()
            else -> return plans
        }
        return planIds.mapNotNull { findById(it) }
    }
}
