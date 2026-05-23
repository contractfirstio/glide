package glide.ui.people

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.PanelSlots

@Composable
fun PeopleFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "People",
        slot = PanelSlots.PEOPLE,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        PeoplePanel(modifier = Modifier.fillMaxSize())
    }
}
