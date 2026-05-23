package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Cross-panel navigation — e.g. open a lead after cloning from a customer group. */
object PeopleGroupNavigation {
    var pendingLeadId by mutableStateOf<String?>(null)

    fun openLead(leadId: String) {
        pendingLeadId = leadId
    }

    fun clearPendingLead() {
        pendingLeadId = null
    }
}
