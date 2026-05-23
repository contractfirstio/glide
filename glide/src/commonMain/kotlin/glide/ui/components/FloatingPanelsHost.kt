package glide.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import glide.ui.customers.CustomersFloatingPanel
import glide.ui.leads.LeadsFloatingPanel
import glide.ui.people.PeopleFloatingPanel
import glide.ui.plans.PlansFloatingPanel
import glide.ui.related.RelatedPeopleFloatingPanel
import glide.ui.layout.PanelSlots

@Composable
fun FloatingPanelsHost(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowWidthPx = constraints.maxWidth
        val windowHeightPx = constraints.maxHeight

        PanelZOrder.order.forEach { slot ->
            key(slot) {
                when (slot) {
                    PanelSlots.LEADS -> LeadsFloatingPanel(
                        windowWidthPx = windowWidthPx,
                        windowHeightPx = windowHeightPx,
                    )
                    PanelSlots.PLANS -> PlansFloatingPanel(
                        windowWidthPx = windowWidthPx,
                        windowHeightPx = windowHeightPx,
                    )
                    PanelSlots.RELATED -> RelatedPeopleFloatingPanel(
                        windowWidthPx = windowWidthPx,
                        windowHeightPx = windowHeightPx,
                    )
                    PanelSlots.PEOPLE -> PeopleFloatingPanel(
                        windowWidthPx = windowWidthPx,
                        windowHeightPx = windowHeightPx,
                    )
                    PanelSlots.CUSTOMERS -> CustomersFloatingPanel(
                        windowWidthPx = windowWidthPx,
                        windowHeightPx = windowHeightPx,
                    )
                }
            }
        }
    }
}
