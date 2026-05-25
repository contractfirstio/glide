package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Cross-panel navigation — e.g. open a lead after cloning from a sold plan. */
object LeadNavigation {
    var pendingLeadId by mutableStateOf<String?>(null)
    var pendingSoldPlanId by mutableStateOf<String?>(null)

    fun openLead(leadId: String) {
        pendingLeadId = leadId
    }

    fun clearPendingLead() {
        pendingLeadId = null
    }

    fun openSoldPlan(soldPlanId: String) {
        pendingSoldPlanId = soldPlanId
    }

    fun clearPendingSoldPlan() {
        pendingSoldPlanId = null
    }
}
