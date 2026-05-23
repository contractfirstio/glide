package glide.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.data.BillingPanelState
import glide.ui.billing.BillingFloatingPanel
import glide.ui.customers.CustomersFloatingPanel
import glide.ui.leads.LeadsFloatingPanel
import glide.ui.people.PeopleFloatingPanel
import glide.ui.plans.PlansFloatingPanel
import glide.ui.related.RelatedPeopleFloatingPanel
import glide.ui.layout.PanelSlots
import glide.ui.layout.SchedulingPanelSlots
import glide.ui.scheduling.ScheduleFloatingPanel
import glide.ui.scheduling.SchedulingCustomerGroupsFloatingPanel
import glide.ui.scheduling.TermsFloatingPanel

@Composable
fun FloatingPanelsHost(modifier: Modifier = Modifier) {
    val viewMode = AppViewState.mode
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowWidthPx = constraints.maxWidth
        val windowHeightPx = constraints.maxHeight

        val billingVisible = BillingPanelState.visible && viewMode == AppViewMode.CUSTOMER_MANAGEMENT
        val billingPeopleGroupId = BillingPanelState.peopleGroupId

        LaunchedEffect(billingVisible) {
            if (billingVisible) {
                PanelZOrder.registerBilling()
            } else {
                PanelZOrder.unregisterBilling()
            }
        }

        PanelZOrder.order.forEach { slot ->
            if (viewMode == AppViewMode.CUSTOMER_MANAGEMENT && slot == PanelSlots.BILLING) {
                return@forEach
            }
            key(viewMode, slot) {
                when (viewMode) {
                    AppViewMode.CUSTOMER_MANAGEMENT -> when (slot) {
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
                        else -> Unit
                    }
                    AppViewMode.SCHEDULING -> when (slot) {
                        SchedulingPanelSlots.SCHEDULE -> ScheduleFloatingPanel(
                            windowWidthPx = windowWidthPx,
                            windowHeightPx = windowHeightPx,
                        )
                        SchedulingPanelSlots.TERMS -> TermsFloatingPanel(
                            windowWidthPx = windowWidthPx,
                            windowHeightPx = windowHeightPx,
                        )
                        SchedulingPanelSlots.CUSTOMER_GROUPS -> SchedulingCustomerGroupsFloatingPanel(
                            windowWidthPx = windowWidthPx,
                            windowHeightPx = windowHeightPx,
                        )
                        else -> Unit
                    }
                }
            }
        }

        if (billingVisible && billingPeopleGroupId != null) {
            key(billingPeopleGroupId) {
                BillingFloatingPanel(
                    peopleGroupId = billingPeopleGroupId,
                    windowWidthPx = windowWidthPx,
                    windowHeightPx = windowHeightPx,
                )
            }
        }
    }
}
