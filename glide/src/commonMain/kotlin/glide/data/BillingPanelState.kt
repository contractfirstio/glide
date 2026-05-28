package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.debug.GlidePanelDebug
import glide.debug.panelStateLog

/**
 * Optional Billing panel — opened from the Sold Plans panel when a sold plan is selected.
 */
object BillingPanelState {
    var visible by mutableStateOf(false)
        private set
    var soldPlanId by mutableStateOf<String?>(null)
        private set

    /** Call when a sold plan is selected in the Sold Plans panel. */
    fun onSoldPlanSelected(soldPlanId: String) {
        if (findSoldPlanById(soldPlanId) == null) {
            panelStateLog(GlidePanelDebug.Panel.BILLING, "onSoldPlanSelected.miss", "soldPlanId=$soldPlanId")
            return
        }
        panelStateLog(GlidePanelDebug.Panel.BILLING, "onSoldPlanSelected", "soldPlanId=$soldPlanId")
        ClientsPanelState.clearClientFilter()
        StudentsPanelState.clearStudentFilter()
        PlansPanelState.clearPlanFilter()
        this.soldPlanId = soldPlanId
        visible = AppViewState.mode == AppViewMode.CUSTOMER_MANAGEMENT
    }

    /** Call when sold plan selection is cleared in the Sold Plans panel. */
    fun onSoldPlanCleared() {
        panelStateLog(GlidePanelDebug.Panel.BILLING, "onSoldPlanCleared")
        clear()
    }

    /** Re-open billing for the currently selected sold plan (e.g. after Close). */
    fun reopenForCurrentSoldPlan() {
        val id = soldPlanId ?: return
        panelStateLog(GlidePanelDebug.Panel.BILLING, "reopenForCurrentSoldPlan", "soldPlanId=$id")
        onSoldPlanSelected(id)
    }

    fun close() {
        panelStateLog(GlidePanelDebug.Panel.BILLING, "close", "soldPlanId=$soldPlanId")
        visible = false
    }

    private fun clear() {
        panelStateLog(GlidePanelDebug.Panel.BILLING, "clear", "soldPlanId was $soldPlanId")
        soldPlanId = null
        visible = false
    }
}
