package glide.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.data.RollingPackBillingService
import glide.ui.components.AppChrome
import glide.ui.components.FloatingPanelsHost
import glide.ui.billing.OverdueBillPaymentAlertBanner
import glide.ui.billing.PendingBillIssuanceAlertBanner
import glide.ui.billing.rememberOverdueBillPayments
import glide.ui.billing.rememberPendingBillsToIssue
import glide.ui.scheduling.PendingAttendanceAlertBanner
import glide.ui.scheduling.UnassignedSoldPackAlertBanner
import glide.ui.scheduling.rememberPendingAttendanceSessions
import glide.ui.scheduling.rememberUnassignedSoldPacks
import glide.ui.theme.GlideCanvasBackground
import glide.ui.theme.GlideTheme

@Composable
fun App() {
    GlideTheme {
        val focusRequester = remember { FocusRequester() }
        val viewMode = AppViewState.mode
        val pendingAttendance = rememberPendingAttendanceSessions()
        val pendingBillsToIssue = rememberPendingBillsToIssue()
        val overdueBillPayments = rememberOverdueBillPayments()
        val unassignedSoldPacks = rememberUnassignedSoldPacks()
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            RollingPackBillingService.syncAllActiveRollingPackBilling()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val shortcut = event.isMetaPressed || event.isCtrlPressed
                    if (!shortcut) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.One -> {
                            AppViewState.switchTo(AppViewMode.CUSTOMER_MANAGEMENT)
                            true
                        }
                        Key.Two -> {
                            AppViewState.switchTo(AppViewMode.SCHEDULING)
                            true
                        }
                        else -> false
                    }
                },
        ) {
            GlideCanvasBackground()
            Column(modifier = Modifier.fillMaxSize()) {
                AppChrome(modifier = Modifier.fillMaxWidth())
                if (viewMode != AppViewMode.SCHEDULING) {
                    PendingAttendanceAlertBanner(pending = pendingAttendance)
                }
                PendingBillIssuanceAlertBanner(pending = pendingBillsToIssue)
                OverdueBillPaymentAlertBanner(overdue = overdueBillPayments)
                UnassignedSoldPackAlertBanner(unassigned = unassignedSoldPacks)
                FloatingPanelsHost(modifier = Modifier.weight(1f))
            }
        }
    }
}
