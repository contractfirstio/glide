package glide.ui.leads

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.PanelSlots
import glide.ui.peoplegroup.LeadsPeopleGroupPanel

@Composable
fun LeadsFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Leads",
        slot = PanelSlots.LEADS,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        LeadsPeopleGroupPanel(modifier = Modifier.fillMaxSize())
    }
}
