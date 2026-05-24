package glide.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import glide.backup.DataBackupResult
import glide.backup.DataBackupService
import glide.data.AppSettingsStore
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.data.RollingPlanBillingService
import glide.ui.components.AppChrome
import glide.ui.components.FloatingPanelsHost
import glide.ui.billing.OverdueBillPaymentAlertBanner
import glide.ui.billing.PendingBillIssuanceAlertBanner
import glide.ui.billing.rememberOverdueBillPayments
import glide.ui.billing.rememberPendingBillsToIssue
import glide.ui.scheduling.PendingAttendanceAlertBanner
import glide.ui.scheduling.UnassignedSoldPlanAlertBanner
import glide.ui.scheduling.rememberPendingAttendanceSessions
import glide.ui.scheduling.rememberUnassignedSoldPlans
import glide.ui.theme.GlideCanvasBackground
import glide.ui.theme.GlideTheme

@Composable
fun App(
    closeRequested: Boolean = false,
    onCloseRequestHandled: () -> Unit = {},
    onExitApplication: () -> Unit = {},
) {
    GlideTheme {
        val appSettings by AppSettingsStore.settingsState
        var showCompanySettings by remember { mutableStateOf(false) }
        var backupErrorMessage by remember { mutableStateOf<String?>(null) }
        var showOpenBackupOffer by remember { mutableStateOf(false) }
        var showCloseBackupOffer by remember { mutableStateOf(false) }
        var exitAfterBackupErrorDismiss by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val viewMode = AppViewState.mode
        val pendingAttendance = rememberPendingAttendanceSessions()
        val pendingBillsToIssue = rememberPendingBillsToIssue()
        val overdueBillPayments = rememberOverdueBillPayments()
        val unassignedSoldPlans = rememberUnassignedSoldPlans()
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            RollingPlanBillingService.syncAllActiveRollingPlanBilling()
            if (AppSettingsStore.shouldOfferBackupPrompt()) {
                showOpenBackupOffer = true
            }
        }

        LaunchedEffect(closeRequested) {
            if (!closeRequested) return@LaunchedEffect
            onCloseRequestHandled()
            handleAppCloseRequest(
                onExitApplication = onExitApplication,
                onShowCloseBackupOffer = { showCloseBackupOffer = true },
            )
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
                AppChrome(
                    modifier = Modifier.fillMaxWidth(),
                    companySettingsNeedSetup = !appSettings.isConfigured,
                    companySettingsConfigured = appSettings.isConfigured,
                    onOpenCompanySettings = { showCompanySettings = true },
                    onEmailDataBackup = {
                        when (val result = DataBackupService.backupAndEmail()) {
                            is DataBackupResult.Success -> Unit
                            is DataBackupResult.Failure -> backupErrorMessage = result.message
                        }
                    },
                )
                if (viewMode != AppViewMode.SCHEDULING) {
                    PendingAttendanceAlertBanner(pending = pendingAttendance)
                }
                PendingBillIssuanceAlertBanner(pending = pendingBillsToIssue)
                OverdueBillPaymentAlertBanner(overdue = overdueBillPayments)
                UnassignedSoldPlanAlertBanner(unassigned = unassignedSoldPlans)
                FloatingPanelsHost(modifier = Modifier.weight(1f))
            }
        }

        if (showOpenBackupOffer) {
            BackupOfferDialog(
                title = "Create a data backup?",
                message = "Email a zip of your Glide data to ${appSettings.companyEmail}.",
                onCreateBackup = {
                    showOpenBackupOffer = false
                    runEmailDataBackup { backupErrorMessage = it }
                },
                onSkip = { showOpenBackupOffer = false },
            )
        }

        if (showCloseBackupOffer) {
            BackupOfferDialog(
                title = "Back up before closing?",
                message = "Email a zip of your Glide data to ${appSettings.companyEmail} before you quit.",
                onCreateBackup = {
                    showCloseBackupOffer = false
                    if (runEmailDataBackup { backupErrorMessage = it }) {
                        onExitApplication()
                    } else {
                        exitAfterBackupErrorDismiss = true
                    }
                },
                onSkip = {
                    showCloseBackupOffer = false
                    onExitApplication()
                },
            )
        }

        backupErrorMessage?.let { message ->
            BackupResultDialog(
                message = message,
                onDismiss = {
                    backupErrorMessage = null
                    if (exitAfterBackupErrorDismiss) {
                        exitAfterBackupErrorDismiss = false
                        onExitApplication()
                    }
                },
            )
        }

        if (!appSettings.isConfigured || showCompanySettings) {
            CompanySetupDialog(
                initial = appSettings,
                mode = if (!appSettings.isConfigured) {
                    CompanySetupDialogMode.FIRST_RUN
                } else {
                    CompanySetupDialogMode.EDIT
                },
                onConfirm = { settings ->
                    AppSettingsStore.save(settings)
                    showCompanySettings = false
                },
                onDismiss = { showCompanySettings = false },
            )
        }
    }
}
