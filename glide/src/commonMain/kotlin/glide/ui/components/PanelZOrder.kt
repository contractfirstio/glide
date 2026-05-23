package glide.ui.components

import androidx.compose.runtime.mutableStateListOf
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.ui.layout.PanelSlots
import glide.ui.layout.SchedulingPanelSlots

object PanelZOrder {
    private val customerManagementOrder = mutableStateListOf(
        PanelSlots.LEADS,
        PanelSlots.PLANS,
        PanelSlots.RELATED,
        PanelSlots.PEOPLE,
        PanelSlots.CUSTOMERS,
    )

    private val schedulingOrder = mutableStateListOf(
        SchedulingPanelSlots.TERMS,
        SchedulingPanelSlots.LOCATIONS,
        SchedulingPanelSlots.SCHEDULE,
        SchedulingPanelSlots.CALENDAR,
        SchedulingPanelSlots.CUSTOMER_GROUPS,
    )

    val order: List<Int>
        get() = when (AppViewState.mode) {
            AppViewMode.CUSTOMER_MANAGEMENT -> customerManagementOrder
            AppViewMode.SCHEDULING -> schedulingOrder
        }

    val focusedSlot: Int? get() = order.lastOrNull()

    fun isFocused(slot: Int): Boolean = order.lastOrNull() == slot

    fun bringToFront(slot: Int) {
        val list = when (AppViewState.mode) {
            AppViewMode.CUSTOMER_MANAGEMENT -> customerManagementOrder
            AppViewMode.SCHEDULING -> schedulingOrder
        }
        if (list.lastOrNull() == slot) return
        list.remove(slot)
        list.add(slot)
    }

    fun registerBilling() {
        if (PanelSlots.BILLING !in customerManagementOrder) {
            customerManagementOrder.add(PanelSlots.BILLING)
        }
        if (AppViewState.mode == AppViewMode.CUSTOMER_MANAGEMENT) {
            bringToFront(PanelSlots.BILLING)
        }
    }

    fun unregisterBilling() {
        customerManagementOrder.remove(PanelSlots.BILLING)
    }
}
