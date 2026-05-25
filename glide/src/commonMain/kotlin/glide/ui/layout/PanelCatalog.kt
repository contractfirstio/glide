package glide.ui.layout

import glide.data.AppViewMode

object PanelCatalog {
    fun primarySlots(mode: AppViewMode): List<Int> = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> listOf(
            PanelSlots.LEADS,
            PanelSlots.PLANS,
            PanelSlots.CLIENTS,
            PanelSlots.SOLD_PLANS,
            PanelSlots.STUDENTS,
        )
        AppViewMode.SCHEDULING -> listOf(
            SchedulingPanelSlots.SCHEDULE,
            SchedulingPanelSlots.CALENDAR,
            SchedulingPanelSlots.SOLD_PLANS,
            SchedulingPanelSlots.TERMS,
            SchedulingPanelSlots.LOCATIONS,
        )
    }

    fun label(slot: Int, mode: AppViewMode = glide.data.AppViewState.mode): String = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> when (slot) {
            PanelSlots.LEADS -> "Leads"
            PanelSlots.PLANS -> "Plans"
            PanelSlots.CLIENTS -> "Clients"
            PanelSlots.SOLD_PLANS -> "Sold Plans"
            PanelSlots.STUDENTS -> "Students"
            PanelSlots.BILLING -> "Billing"
            else -> "Panel"
        }
        AppViewMode.SCHEDULING -> when (slot) {
            SchedulingPanelSlots.SCHEDULE -> "Classes"
            SchedulingPanelSlots.CALENDAR -> "Term calendar"
            SchedulingPanelSlots.SOLD_PLANS -> "Sold Plans"
            SchedulingPanelSlots.TERMS -> "Terms"
            SchedulingPanelSlots.LOCATIONS -> "Locations"
            SchedulingPanelSlots.ATTENDANCE -> "Attendance"
            else -> "Panel"
        }
    }

    /** Short labels for compact chrome and the collapsed dock. */
    fun shortLabel(slot: Int, mode: AppViewMode = glide.data.AppViewState.mode): String = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> when (slot) {
            PanelSlots.LEADS -> "Leads"
            PanelSlots.PLANS -> "Plans"
            PanelSlots.CLIENTS -> "Clients"
            PanelSlots.SOLD_PLANS -> "Sold"
            PanelSlots.STUDENTS -> "Students"
            PanelSlots.BILLING -> "Billing"
            else -> "Panel"
        }
        AppViewMode.SCHEDULING -> when (slot) {
            SchedulingPanelSlots.SCHEDULE -> "Classes"
            SchedulingPanelSlots.CALENDAR -> "Calendar"
            SchedulingPanelSlots.SOLD_PLANS -> "Sold"
            SchedulingPanelSlots.TERMS -> "Terms"
            SchedulingPanelSlots.LOCATIONS -> "Locations"
            SchedulingPanelSlots.ATTENDANCE -> "Attendance"
            else -> "Panel"
        }
    }

    /** Panel index 0–4 maps to keyboard shortcuts ⌘⇧3–7. */
    fun slotAtShortcutIndex(index: Int, mode: AppViewMode): Int? =
        primarySlots(mode).getOrNull(index)
}
