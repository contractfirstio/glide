package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
        if (findSoldPlanById(soldPlanId) == null) return
        ClientsPanelState.clearClientFilter()
        StudentsPanelState.clearStudentFilter()
        PlansPanelState.clearPlanFilter()
        this.soldPlanId = soldPlanId
        visible = AppViewState.mode == AppViewMode.CUSTOMER_MANAGEMENT
    }

    /** Call when sold plan selection is cleared in the Sold Plans panel. */
    fun onSoldPlanCleared() {
        clear()
    }

    /** Re-open billing for the currently selected sold plan (e.g. after Close). */
    fun reopenForCurrentSoldPlan() {
        val id = soldPlanId ?: return
        onSoldPlanSelected(id)
    }

    fun close() {
        visible = false
    }

    private fun clear() {
        soldPlanId = null
        visible = false
    }
}
