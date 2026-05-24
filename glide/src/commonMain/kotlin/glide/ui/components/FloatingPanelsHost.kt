package glide.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.data.AttendancePanelState
import glide.data.BillingPanelState
import glide.ui.billing.BillingFloatingPanel
import glide.ui.scheduling.PendingAttendanceAlertBanner
import glide.ui.scheduling.rememberPendingAttendanceSessions
import glide.ui.scheduling.AttendanceFloatingPanel
import glide.ui.customers.CustomersFloatingPanel
import glide.ui.leads.LeadsFloatingPanel
import glide.ui.clients.ClientsFloatingPanel
import glide.ui.plans.PlansFloatingPanel
import glide.ui.students.StudentsFloatingPanel
import glide.ui.layout.PanelSlots
import glide.ui.layout.SchedulingPanelSlots
import glide.ui.scheduling.LocationsFloatingPanel
import glide.ui.scheduling.ScheduleFloatingPanel
import glide.ui.scheduling.SchedulingCustomerGroupsFloatingPanel
import glide.ui.scheduling.TermCalendarFloatingPanel
import glide.ui.scheduling.TermsFloatingPanel

@Composable
fun FloatingPanelsHost(modifier: Modifier = Modifier) {
    val viewMode = AppViewState.mode
    val pendingAttendance = rememberPendingAttendanceSessions()
    Column(modifier = modifier.fillMaxSize()) {
        if (viewMode == AppViewMode.SCHEDULING) {
            PendingAttendanceAlertBanner(
                pending = pendingAttendance,
                compact = true,
            )
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxSize()) {
        val windowWidthPx = constraints.maxWidth
        val windowHeightPx = constraints.maxHeight

        val billingVisible = BillingPanelState.visible && viewMode == AppViewMode.CUSTOMER_MANAGEMENT
        val billingSoldPlanId = BillingPanelState.soldPlanId

        val attendanceVisible = AttendancePanelState.visible && viewMode == AppViewMode.SCHEDULING
        val attendanceSession = AttendancePanelState.sessionKey

        LaunchedEffect(billingVisible) {
            if (billingVisible) {
                PanelZOrder.registerBilling()
            } else {
                PanelZOrder.unregisterBilling()
            }
        }

        LaunchedEffect(attendanceVisible) {
            if (attendanceVisible) {
                PanelZOrder.registerAttendance()
            } else {
                PanelZOrder.unregisterAttendance()
            }
        }

        PanelZOrder.order.forEach { slot ->
            if (viewMode == AppViewMode.CUSTOMER_MANAGEMENT && slot == PanelSlots.BILLING) {
                return@forEach
            }
            if (viewMode == AppViewMode.SCHEDULING && slot == SchedulingPanelSlots.ATTENDANCE) {
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
                        PanelSlots.STUDENTS -> StudentsFloatingPanel(
                            windowWidthPx = windowWidthPx,
                            windowHeightPx = windowHeightPx,
                        )
                        PanelSlots.CLIENTS -> ClientsFloatingPanel(
                            windowWidthPx = windowWidthPx,
                            windowHeightPx = windowHeightPx,
                        )
                        PanelSlots.SOLD_PLANS -> CustomersFloatingPanel(
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
                        SchedulingPanelSlots.SOLD_PLANS -> SchedulingCustomerGroupsFloatingPanel(
                            windowWidthPx = windowWidthPx,
                            windowHeightPx = windowHeightPx,
                        )
                        SchedulingPanelSlots.LOCATIONS -> LocationsFloatingPanel(
                            windowWidthPx = windowWidthPx,
                            windowHeightPx = windowHeightPx,
                        )
                        SchedulingPanelSlots.CALENDAR -> TermCalendarFloatingPanel(
                            windowWidthPx = windowWidthPx,
                            windowHeightPx = windowHeightPx,
                        )
                        else -> Unit
                    }
                }
            }
        }

        if (billingVisible && billingSoldPlanId != null) {
            key(billingSoldPlanId) {
                BillingFloatingPanel(
                    soldPlanId = billingSoldPlanId,
                    windowWidthPx = windowWidthPx,
                    windowHeightPx = windowHeightPx,
                )
            }
        }

        if (attendanceVisible && attendanceSession != null) {
            key(attendanceSession.classId, attendanceSession.sessionDate) {
                AttendanceFloatingPanel(
                    session = attendanceSession,
                    windowWidthPx = windowWidthPx,
                    windowHeightPx = windowHeightPx,
                )
            }
        }
        }
    }
}
