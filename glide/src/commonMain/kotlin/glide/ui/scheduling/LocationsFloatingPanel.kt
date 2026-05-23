package glide.ui.scheduling

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.SchedulingPanelSlots

@Composable
fun LocationsFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Locations",
        slot = SchedulingPanelSlots.LOCATIONS,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
        initiallyCollapsed = false,
    ) {
        LocationsPanel(modifier = Modifier.fillMaxSize())
    }
}
