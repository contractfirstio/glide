package glide.ui.billing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.data.BillingPanelState
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.PanelSlots

@Composable
fun BillingFloatingPanel(
    peopleGroupId: String,
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    if (!BillingPanelState.visible || BillingPanelState.peopleGroupId != peopleGroupId) return

    val title = billingPanelTitle(peopleGroupId)

    FloatingPanelShell(
        title = title,
        slot = PanelSlots.BILLING,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
        onClose = { BillingPanelState.close() },
    ) {
        BillingPanel(
            peopleGroupId = peopleGroupId,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
