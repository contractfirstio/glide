package glide.ui.scheduling

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.SchedulingPanelSlots
import glide.ui.peoplegroup.SoldPlansPanel

@Composable
fun SchedulingCustomerGroupsFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Sold Plans",
        slot = SchedulingPanelSlots.SOLD_PLANS,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        SoldPlansPanel(modifier = Modifier.fillMaxSize())
    }
}
