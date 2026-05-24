package glide.ui.customers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.PanelSlots
import glide.ui.peoplegroup.CustomersPeopleGroupPanel

@Composable
fun CustomersFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Sold Plans",
        slot = PanelSlots.CUSTOMERS,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        CustomersPeopleGroupPanel(modifier = Modifier.fillMaxSize())
    }
}
