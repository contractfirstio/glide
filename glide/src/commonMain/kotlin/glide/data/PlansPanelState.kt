package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.debug.GlidePanelDebug
import glide.debug.panelStateLog

/**
 * Cross-panel state from the Plans panel — filters sold plans, clients, and students
 * people for [selectedPlanId].
 */
object PlansPanelState {
    var selectedPlanId by mutableStateOf<String?>(null)
        private set

    fun onPlanSelected(planId: String) {
        if (PlanStore.findById(planId) != null) {
            panelStateLog(
                GlidePanelDebug.Panel.PLANS,
                "onPlanSelected",
                "planId=$planId (clears billing/client/student filters, sets outbound plan filter)",
            )
            BillingPanelState.onSoldPlanCleared()
            ClientsPanelState.clearClientFilter()
            StudentsPanelState.clearStudentFilter()
            selectedPlanId = planId
        } else {
            panelStateLog(GlidePanelDebug.Panel.PLANS, "onPlanSelected.miss", "planId=$planId")
        }
    }

    /** Clears the cross-panel filter. Does not change Plans panel selection. */
    fun clearPlanFilter() {
        panelStateLog(GlidePanelDebug.Panel.PLANS, "clearPlanFilter", "was planId=$selectedPlanId")
        selectedPlanId = null
    }
}
