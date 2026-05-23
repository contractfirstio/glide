package glide.ui.scheduling

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.SchedulingPanelSlots
import glide.ui.peoplegroup.CustomersPeopleGroupPanel

@Composable
fun SchedulingCustomerGroupsFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Customer groups",
        slot = SchedulingPanelSlots.CUSTOMER_GROUPS,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        CustomersPeopleGroupPanel(modifier = Modifier.fillMaxSize())
    }
}
