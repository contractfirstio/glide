package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cross-panel state from the Clients panel — filters customer groups for [selectedClientId].
 */
object ClientsPanelState {
    var selectedClientId by mutableStateOf<String?>(null)
        private set

    fun onClientSelected(clientId: String) {
        if (ClientStore.findById(clientId) != null) {
            BillingPanelState.onCustomerGroupCleared()
            StudentsPanelState.clearStudentFilter()
            PlansPanelState.clearPlanFilter()
            selectedClientId = clientId
        }
    }

    /** Clears the customer-group filter. Does not change Clients panel selection. */
    fun clearClientFilter() {
        selectedClientId = null
    }

    fun onClientCleared() {
        clearClientFilter()
    }
}
