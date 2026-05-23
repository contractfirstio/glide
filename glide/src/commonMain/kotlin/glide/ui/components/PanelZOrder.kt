package glide.ui.components

import androidx.compose.runtime.mutableStateListOf
import glide.ui.layout.PanelSlots

object PanelZOrder {
    private val _order = mutableStateListOf(
        PanelSlots.LEADS,
        PanelSlots.PLANS,
        PanelSlots.RELATED,
        PanelSlots.PEOPLE,
        PanelSlots.CUSTOMERS,
    )

    val order: List<Int> get() = _order

    val focusedSlot: Int? get() = _order.lastOrNull()

    fun isFocused(slot: Int): Boolean = _order.lastOrNull() == slot

    fun bringToFront(slot: Int) {
        if (_order.lastOrNull() == slot) return
        _order.remove(slot)
        _order.add(slot)
    }

    fun registerBilling() {
        if (PanelSlots.BILLING !in _order) {
            _order.add(PanelSlots.BILLING)
        }
        bringToFront(PanelSlots.BILLING)
    }

    fun unregisterBilling() {
        _order.remove(PanelSlots.BILLING)
    }
}
