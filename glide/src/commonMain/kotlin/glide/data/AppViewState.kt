package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.debug.GlidePanelDebug
import glide.debug.panelStateLog

enum class AppViewMode {
    CUSTOMER_MANAGEMENT,
    SCHEDULING,
}

object AppViewState {
    var mode by mutableStateOf(AppViewMode.CUSTOMER_MANAGEMENT)
        private set

    fun switchTo(newMode: AppViewMode) {
        if (mode == newMode) return
        panelStateLog(GlidePanelDebug.Panel.APP, "switchTo", "$mode -> $newMode")
        when (newMode) {
            AppViewMode.SCHEDULING -> {
                BillingPanelState.close()
                SchedulePanelState.clear()
            }
            AppViewMode.CUSTOMER_MANAGEMENT -> {
                AttendancePanelState.clear()
                SchedulePanelState.clear()
                if (BillingPanelState.soldPlanId != null) {
                    BillingPanelState.reopenForCurrentSoldPlan()
                }
            }
        }
        mode = newMode
    }
}
