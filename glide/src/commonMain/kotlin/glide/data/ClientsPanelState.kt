package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.debug.GlidePanelDebug
import glide.debug.panelStateLog

/**
 * Cross-panel state from the Clients panel — filters sold plans for [selectedClientId].
 */
object ClientsPanelState {
    var selectedClientId by mutableStateOf<String?>(null)
        private set

    fun onClientSelected(clientId: String) {
        if (ClientStore.findById(clientId) != null) {
            panelStateLog(GlidePanelDebug.Panel.CLIENTS, "onClientSelected", "clientId=$clientId")
            BillingPanelState.onSoldPlanCleared()
            StudentsPanelState.clearStudentFilter()
            PlansPanelState.clearPlanFilter()
            selectedClientId = clientId
        } else {
            panelStateLog(GlidePanelDebug.Panel.CLIENTS, "onClientSelected.miss", "clientId=$clientId")
        }
    }

    /** Clears the customer-group filter. Does not change Clients panel selection. */
    fun clearClientFilter() {
        panelStateLog(GlidePanelDebug.Panel.CLIENTS, "clearClientFilter", "was clientId=$selectedClientId")
        selectedClientId = null
    }

    fun onClientCleared() {
        clearClientFilter()
    }
}
