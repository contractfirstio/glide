package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.model.PeopleGroupType

/**
 * Optional Billing panel — only driven from the Customers panel when a customer group is selected.
 */
object BillingPanelState {
    var visible by mutableStateOf(false)
        private set
    var peopleGroupId by mutableStateOf<String?>(null)
        private set

    /** Call when a customer group is selected in the Customers panel. */
    fun onCustomerGroupSelected(groupId: String) {
        val group = PeopleGroupStore.findById(groupId) ?: return
        if (group.type != PeopleGroupType.CUSTOMER) {
            clear()
            return
        }
        ContactsPanelState.clearContactFilter()
        RelatedPanelState.clearRelatedPersonFilter()
        peopleGroupId = groupId
        visible = true
    }

    /** Call when customer selection is cleared in the Customers panel. */
    fun onCustomerGroupCleared() {
        clear()
    }

    /** Re-open billing for the currently selected customer group (e.g. after Close). */
    fun reopenForCurrentGroup() {
        val id = peopleGroupId ?: return
        onCustomerGroupSelected(id)
    }

    fun close() {
        visible = false
    }

    private fun clear() {
        peopleGroupId = null
        visible = false
    }
}
