package glide.ui.billing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.data.BillingPanelState
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.PanelSlots

@Composable
fun BillingFloatingPanel(
    soldPlanId: String,
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    if (!BillingPanelState.visible || BillingPanelState.soldPlanId != soldPlanId) return

    val title = billingPanelTitle(soldPlanId)

    FloatingPanelShell(
        title = title,
        slot = PanelSlots.BILLING,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
        onClose = { BillingPanelState.close() },
    ) {
        BillingPanel(
            soldPlanId = soldPlanId,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
