package glide.ui.clients

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.PanelSlots

@Composable
fun ClientsFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Clients",
        slot = PanelSlots.CLIENTS,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        ClientsPanel(modifier = Modifier.fillMaxSize())
    }
}
