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
}
