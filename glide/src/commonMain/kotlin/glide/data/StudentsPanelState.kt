package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cross-panel state from the Students panel — filters customer groups and clients for
 * [selectedStudentId].
 */
object StudentsPanelState {
    var selectedStudentId by mutableStateOf<String?>(null)
        private set

    fun onStudentSelected(personId: String) {
        if (StudentStore.findById(personId) != null) {
            BillingPanelState.onCustomerGroupCleared()
            ClientsPanelState.clearClientFilter()
            PlansPanelState.clearPlanFilter()
            selectedStudentId = personId
        }
    }

    /** Clears the cross-panel filter. Does not change Students panel selection. */
    fun clearStudentFilter() {
        selectedStudentId = null
    }
}
