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

    fun bringToFront(slot: Int) {
        if (_order.lastOrNull() == slot) return
        _order.remove(slot)
        _order.add(slot)
    }
}
