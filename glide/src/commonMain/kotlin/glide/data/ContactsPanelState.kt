package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cross-panel state from the Contacts panel — filters customer groups for [selectedContactId].
 */
object ContactsPanelState {
    var selectedContactId by mutableStateOf<String?>(null)
        private set

    fun onContactSelected(contactId: String) {
        if (ContactStore.findById(contactId) != null) {
            BillingPanelState.onCustomerGroupCleared()
            selectedContactId = contactId
        }
    }

    /** Clears the customer-group filter. Does not change Contacts panel selection. */
    fun clearContactFilter() {
        selectedContactId = null
    }

    fun onContactCleared() {
        clearContactFilter()
    }
}
