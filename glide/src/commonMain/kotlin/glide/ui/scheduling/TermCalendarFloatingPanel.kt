package glide.ui.scheduling

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.SchedulingPanelSlots

@Composable
fun TermCalendarFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Term calendar",
        slot = SchedulingPanelSlots.CALENDAR,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
        initiallyCollapsed = false,
    ) {
        TermCalendarPanel(modifier = Modifier.fillMaxSize())
    }
}
