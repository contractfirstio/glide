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
        title = "Schedule",
        slot = SchedulingPanelSlots.SCHEDULE,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        SchedulingPlaceholderPanel(
            title = "Class schedule",
            description = "Week and day views, session roster, and multi-group bookings will appear here.",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
