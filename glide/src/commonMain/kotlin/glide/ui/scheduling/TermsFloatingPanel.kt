package glide.ui.scheduling

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.SchedulingPanelSlots

@Composable
fun TermsFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Terms",
        slot = SchedulingPanelSlots.TERMS,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        SchedulingPlaceholderPanel(
            title = "Terms and breaks",
            description = "Academic terms, school breaks, and per-term class runs will be configured here.",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
