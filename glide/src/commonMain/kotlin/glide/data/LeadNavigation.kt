package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.debug.GlidePanelDebug
import glide.debug.panelStateLog

/** Cross-panel navigation — e.g. open a lead after cloning from a sold plan. */
object LeadNavigation {
    var pendingLeadId by mutableStateOf<String?>(null)
    var pendingSoldPlanId by mutableStateOf<String?>(null)

    fun openLead(leadId: String) {
        panelStateLog(GlidePanelDebug.Panel.NAV, "openLead", "leadId=$leadId")
        pendingLeadId = leadId
    }

    fun clearPendingLead() {
        panelStateLog(GlidePanelDebug.Panel.NAV, "clearPendingLead", "was leadId=$pendingLeadId")
        pendingLeadId = null
    }

    fun openSoldPlan(soldPlanId: String) {
        panelStateLog(GlidePanelDebug.Panel.NAV, "openSoldPlan", "soldPlanId=$soldPlanId")
        pendingSoldPlanId = soldPlanId
    }

    fun clearPendingSoldPlan() {
        panelStateLog(GlidePanelDebug.Panel.NAV, "clearPendingSoldPlan", "was soldPlanId=$pendingSoldPlanId")
        pendingSoldPlanId = null
    }
}
