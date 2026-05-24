package glide.ui.customers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.PanelSlots
import glide.ui.peoplegroup.SoldPlansPanel

@Composable
fun CustomersFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Sold Plans",
        slot = PanelSlots.SOLD_PLANS,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        SoldPlansPanel(modifier = Modifier.fillMaxSize())
    }
}
