package glide.ui.related

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.PanelSlots

@Composable
fun RelatedPeopleFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Related",
        slot = PanelSlots.RELATED,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        RelatedPeoplePanel(modifier = Modifier.fillMaxSize())
    }
}
