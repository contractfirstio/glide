package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cross-panel state from the Plans panel — filters customer groups, contacts, and related
 * people for [selectedPlanId].
 */
object PlansPanelState {
    var selectedPlanId by mutableStateOf<String?>(null)
        private set

    fun onPlanSelected(planId: String) {
        if (PlanStore.findById(planId) != null) {
            BillingPanelState.onCustomerGroupCleared()
            ContactsPanelState.clearContactFilter()
            RelatedPanelState.clearRelatedPersonFilter()
            selectedPlanId = planId
        }
    }

    /** Clears the cross-panel filter. Does not change Plans panel selection. */
    fun clearPlanFilter() {
        selectedPlanId = null
    }
}
