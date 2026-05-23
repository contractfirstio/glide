package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cross-panel state from the Related panel — filters customer groups and contacts for
 * [selectedRelatedPersonId].
 */
object RelatedPanelState {
    var selectedRelatedPersonId by mutableStateOf<String?>(null)
        private set

    fun onRelatedPersonSelected(personId: String) {
        if (RelatedPersonStore.findById(personId) != null) {
            BillingPanelState.onCustomerGroupCleared()
            ContactsPanelState.clearContactFilter()
            PlansPanelState.clearPlanFilter()
            selectedRelatedPersonId = personId
        }
    }

    /** Clears the cross-panel filter. Does not change Related panel selection. */
    fun clearRelatedPersonFilter() {
        selectedRelatedPersonId = null
    }
}
