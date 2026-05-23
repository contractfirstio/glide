package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Plan

object PlanStore {
    private val _plans = mutableStateListOf<Plan>()
    val plans: List<Plan> get() = _plans

    fun create(plan: Plan) {
        _plans.add(plan)
    }

    fun update(plan: Plan) {
        val index = _plans.indexOfFirst { it.id == plan.id }
        if (index >= 0) {
            _plans[index] = plan
        }
    }

    fun delete(id: String) {
        _plans.removeAll { it.id == id }
    }

    fun findById(id: String): Plan? = _plans.find { it.id == id }

    /**
     * Plans for the Plans panel — all plans, the plan on [customerGroupId], or plans used on
     * groups linked to [contactId] or [relatedPersonId].
     */
    fun forPlansPanel(
        customerGroupId: String? = null,
        contactId: String? = null,
        relatedPersonId: String? = null,
    ): List<Plan> {
        val planIds = when {
            customerGroupId != null ->
                PeopleGroupStore.findById(customerGroupId)
                    ?.planId
                    ?.let { listOf(it) }
                    ?: emptyList()
            contactId != null ->
                PeopleGroupStore.all
                    .filter { it.mainContactId == contactId }
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
