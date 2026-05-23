package glide.ui.scheduling

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.SchedulingPanelSlots

@Composable
fun ScheduleFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Classes",
        slot = SchedulingPanelSlots.SCHEDULE,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        SchedulePanel(modifier = Modifier.fillMaxSize())
    }
}
