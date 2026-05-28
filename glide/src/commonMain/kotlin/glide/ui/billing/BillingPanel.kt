package glide.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.data.BillStore
import glide.debug.GlidePanelDebug
import glide.debug.PanelDebugStateEffect
import glide.data.AttendanceCreditStore
import glide.data.attendanceBlocksBillIssuanceMessage
import glide.data.creditAppliedMinor
import glide.data.BillingService
import glide.data.isEditableBeforeIssue
import glide.data.displayBillingLineItems
import glide.data.displayBillingTotalMinor
import glide.data.openPendingAttendanceSession
import glide.data.openSoldPlanClassAssignment
import glide.data.soldPlanBlocksBillIssuance
import glide.data.soldPlanBlocksBillIssuanceMessage
import glide.ui.scheduling.rememberPendingAttendanceSessions
import glide.data.SoldPlanEnrollmentStore
import glide.data.RollingPlanBillingService
import glide.data.RollingPlanCancellationService
import glide.data.ClassStore
import glide.data.classAttendeeCount
import glide.data.countScheduledPlanSessionsInPeriod
import glide.data.countSubmittedPlanSessionsInPeriod
import glide.model.SoldPlanEnrollmentStatus
import glide.data.PaymentStore
import glide.data.SoldPlanStore
import glide.data.PlanStore
import glide.data.resolveMainClient
import glide.data.findSoldPlanById
import glide.model.Bill
import glide.model.BillLineItem
import glide.model.BillLineItemKind
import glide.model.BillStatus
import glide.model.majorToMinor
import glide.model.minorToMajorString
import glide.model.parseMajorAmount
import glide.model.SoldPlanEnrollment
import glide.model.displayDateMillis
import glide.model.canBeVoided
import glide.model.isBillingEditable
import glide.model.isIssuedToCustomer
import glide.model.PaymentMethod
import glide.model.formatMoney
import glide.ui.layout.GlideLayout
import glide.ui.shared.DeleteConfirmDialog
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.FormPanelSummaryCard
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.glideListItemTitleColor
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun billingPanelTitle(soldPlanId: String): String {
    val group = findSoldPlanById(soldPlanId) ?: return "Billing"
    val main = group.resolveMainClient()
    val planName = group.planId?.let { PlanStore.findById(it)?.name }
    return when {
        planName != null -> "Billing — $planName"
        main?.name?.isNotBlank() == true -> "Billing — ${main.name}"
        else -> "Billing"
    }
}

@Composable
fun BillingPanel(
    soldPlanId: String,
    modifier: Modifier = Modifier,
) {
    val group = findSoldPlanById(soldPlanId)
    val enrollment = SoldPlanEnrollmentStore.displayForSoldPlan(soldPlanId)
    val ongoingEnrollment = SoldPlanEnrollmentStore.forSoldPlan(soldPlanId)
    ClassStore.classes
    val bills = enrollment?.let { e ->
        BillStore.forEnrollment(e.id)
    } ?: emptyList()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    var selectedBillId by remember(soldPlanId) { mutableStateOf<String?>(null) }
    var showPaymentDialog by remember(soldPlanId) { mutableStateOf(false) }
    var paymentError by remember(soldPlanId) { mutableStateOf<String?>(null) }
    var billingActionMessage by remember(soldPlanId) { mutableStateOf<String?>(null) }
    var showCancelPlanDialog by remember(soldPlanId) { mutableStateOf(false) }
    var showVoidBillConfirm by remember(soldPlanId) { mutableStateOf(false) }

    val pendingAttendance = rememberPendingAttendanceSessions()
    val billingBlockedByAttendance = pendingAttendance.isNotEmpty()
    val billingBlockedByUnassignedClass = soldPlanBlocksBillIssuance(soldPlanId)
    val billingBlocked = billingBlockedByAttendance || billingBlockedByUnassignedClass
    val invoiceBlockedReason = when {
        billingBlockedByAttendance -> attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
        billingBlockedByUnassignedClass -> soldPlanBlocksBillIssuanceMessage()
        else -> null
    }

    val spacing = GlideLayout.comfortable
    val outstanding = enrollment?.let { BillStore.outstandingMinorForEnrollment(it.id) } ?: 0L

    PanelDebugStateEffect(
        GlidePanelDebug.Panel.BILLING,
        soldPlanId,
        group?.id,
        enrollment?.id,
        bills.size,
        selectedBillId,
        outstanding,
        billingBlocked,
    ) {
        "soldPlan=$soldPlanId enrollment=${enrollment?.id} bills=${bills.size} " +
            "selectedBill=$selectedBillId outstanding=$outstanding blocked=$billingBlocked || " +
            GlidePanelDebug.globalSnapshot()
    }

    LaunchedEffect(ongoingEnrollment?.id, ongoingEnrollment?.status) {
        ongoingEnrollment?.let { RollingPlanBillingService.syncRollingPlanBilling(it.soldPlanId) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.outer)
            .verticalScroll(rememberScrollState()),
    ) {
        if (group == null) {
            Text(
                text = "Select a sold plan in the Sold Plans panel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val main = group.resolveMainClient()
        Text(
            text = buildString {
                append(main?.name?.ifBlank { "Sold plan" } ?: "Sold plan")
                group.planId?.let { PlanStore.findById(it)?.name }?.let { append(" · $it") }
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(spacing.section))

        if (enrollment == null) {
            Text(
                text = "No billing enrollment for this plan yet. Enrollment is created when a lead becomes a customer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        FormPanelSection(
            title = "Plan enrollment",
            description = "Agreed plan, pricing, and billing period for this sold plan.",
            spacing = spacing,
            role = FormPanelSectionRole.Primary,
        ) {
            EnrollmentSummary(enrollment = enrollment, dateFormat = dateFormat, showBackground = false)

            if (
                ongoingEnrollment != null &&
                enrollment.planSnapshot.rolling &&
                enrollment.status == SoldPlanEnrollmentStatus.ACTIVE
            ) {
                Spacer(modifier = Modifier.height(spacing.field))
                GlideOutlinedButton(
                    onClick = { showCancelPlanDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel plan", color = MaterialTheme.colorScheme.error)
                }
            }

            if (billingBlockedByAttendance) {
                Spacer(modifier = Modifier.height(spacing.field))
                Text(
                    text = attendanceBlocksBillIssuanceMessage(pendingAttendance.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                GlideTextButton(onClick = { openPendingAttendanceSession(pendingAttendance.first()) }) {
                    Text("Open attendance")
                }
            }

            if (billingBlockedByUnassignedClass) {
                Spacer(modifier = Modifier.height(spacing.field))
                Text(
                    text = soldPlanBlocksBillIssuanceMessage(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                GlideTextButton(onClick = { openSoldPlanClassAssignment(soldPlanId) }) {
                    Text("Assign to class")
                }
            }

            billingActionMessage?.let { message ->
                Spacer(modifier = Modifier.height(spacing.field))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        FormPanelSectionsDivider(label = "Balance & billing", spacing = spacing)

        FormPanelSection(
            title = "Outstanding balance",
            description = "Current amount due and quick actions.",
            spacing = spacing,
            role = FormPanelSectionRole.Secondary,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Outstanding",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = formatMoney(outstanding, enrollment.planSnapshot.currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (outstanding > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                GlideOutlinedButton(
                    onClick = {
                        if (!BillingService.addRenewalBill(soldPlanId)) {
                            billingActionMessage = when (ongoingEnrollment?.status) {
                                SoldPlanEnrollmentStatus.CANCELLING ->
                                    "Renewal is stopped while this plan finishes."
                                else -> "Could not add a renewal bill."
                            }
                        } else {
                            billingActionMessage = null
                        }
                        selectedBillId = null
                    },
                    enabled = ongoingEnrollment?.status == SoldPlanEnrollmentStatus.ACTIVE,
                ) {
                    Text("Add bill")
                }
            }
        }

        FormPanelSectionsDivider(label = "Bill history", spacing = spacing)

        FormPanelSection(
            title = "Bills",
            description = "Scheduled, issued, and paid billing lines for this plan.",
            spacing = spacing,
            role = FormPanelSectionRole.Tertiary,
        ) {
            if (bills.isEmpty()) {
                Text(
                    text = "No bills yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                bills.forEach { bill ->
                    BillRow(
                        bill = bill,
                        dateFormat = dateFormat,
                        selected = bill.id == selectedBillId,
                        payment = PaymentStore.forBill(bill.id),
                        billingBlocked = billingBlocked,
                        invoiceBlockedReason = invoiceBlockedReason,
                        onClick = { selectedBillId = bill.id },
                        onIssuedChange = { issued ->
                            if (issued && billingBlockedByAttendance) {
                                billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                                return@BillRow
                            }
                            if (issued && billingBlockedByUnassignedClass) {
                                billingActionMessage = soldPlanBlocksBillIssuanceMessage()
                                return@BillRow
                            }
                            if (!BillStore.setIssued(bill.id, issued = true)) {
                                if (issued && billingBlockedByAttendance) {
                                    billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                                } else if (issued && billingBlockedByUnassignedClass) {
                                    billingActionMessage = soldPlanBlocksBillIssuanceMessage()
                                }
                                return@BillRow
                            }
                            billingActionMessage = null
                        },
                        onGenerateInvoice = {
                            if (billingBlockedByAttendance) {
                                billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                                return@BillRow
                            }
                            if (billingBlockedByUnassignedClass) {
                                billingActionMessage = soldPlanBlocksBillIssuanceMessage()
                                return@BillRow
                            }
                            billingActionMessage = BillStore.generateInvoice(bill.id)
                        },
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }

            val selectedBill = selectedBillId?.let { BillStore.findById(it) }
            if (selectedBill != null) {
                Spacer(modifier = Modifier.height(spacing.section))
                FormPanelSummaryCard(role = FormPanelSectionRole.Tertiary) {
                    BillDetailActions(
                        bill = selectedBill,
                        billingBlocked = billingBlocked,
                        invoiceBlockedReason = invoiceBlockedReason,
                        onRecordPayment = {
                            paymentError = null
                            showPaymentDialog = true
                        },
                        onVoid = { showVoidBillConfirm = true },
                        onGenerateInvoice = {
                            if (billingBlockedByAttendance) {
                                billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                                return@BillDetailActions
                            }
                            if (billingBlockedByUnassignedClass) {
                                billingActionMessage = soldPlanBlocksBillIssuanceMessage()
                                return@BillDetailActions
                            }
                            billingActionMessage = BillStore.generateInvoice(selectedBill.id)
                        },
                        onGenerateReceipt = {
                            billingActionMessage = BillStore.generateReceipt(selectedBill.id)
                        },
                    )
                }
            }
        }

        paymentError?.let { error ->
            Spacer(modifier = Modifier.height(spacing.field))
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showPaymentDialog && selectedBillId != null) {
        val bill = BillStore.findById(selectedBillId!!)
        if (bill != null && bill.status == BillStatus.ISSUED) {
            RecordPaymentDialog(
                bill = bill,
                onDismiss = { showPaymentDialog = false },
                onConfirm = { method, reference ->
                    if (!PaymentStore.recordFullPayment(bill, method, reference)) {
                        paymentError = "Could not record payment."
                    } else {
                        paymentError = null
                        showPaymentDialog = false
                        billingActionMessage = BillStore.generateReceipt(bill.id)
                    }
                },
            )
        }
    }

    if (showVoidBillConfirm && selectedBillId != null) {
        val voidBlockReason = BillStore.billVoidBlockReason(selectedBillId!!)
        DeleteConfirmDialog(
            title = "Void bill?",
            message = voidBlockReason ?: "This scheduled bill will be voided and cannot be issued or paid.",
            onDismiss = { showVoidBillConfirm = false },
            onContinue = {
                if (BillStore.voidBill(selectedBillId!!)) {
                    selectedBillId = null
                    showVoidBillConfirm = false
                } else {
                    showVoidBillConfirm = false
                    billingActionMessage = voidBlockReason
                        ?: "This bill cannot be voided."
                }
            },
            continueEnabled = voidBlockReason == null,
        )
    }

    if (showCancelPlanDialog && enrollment != null) {
        CancelRollingPlanDialog(
            enrollment = enrollment,
            onDismiss = { showCancelPlanDialog = false },
            onConfirm = {
                if (RollingPlanCancellationService.cancelRenewal(enrollment.id)) {
                    billingActionMessage = null
                    showCancelPlanDialog = false
                } else {
                    billingActionMessage = "Could not cancel plan renewal."
                }
            },
        )
    }
}

@Composable
private fun EnrollmentSummary(
    enrollment: SoldPlanEnrollment,
    dateFormat: SimpleDateFormat,
    showBackground: Boolean = true,
) {
    val snapshot = enrollment.planSnapshot
    val participantCount =
        findSoldPlanById(enrollment.soldPlanId)?.classAttendeeCount() ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showBackground) {
                    Modifier.background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        MaterialTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (showBackground) {
                    Modifier.padding(8.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        Text(
            text = snapshot.planName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = when (snapshot.kind) {
                glide.model.PlanKind.MULTI_LESSON_PLAN -> {
                    val rolling = if (snapshot.rolling) "Rolling" else "Fixed"
                    "${snapshot.lessonCount} classes · $rolling"
                }
                glide.model.PlanKind.SINGLE_LESSON_PLAN -> "1 class"
                glide.model.PlanKind.CAMP -> "${snapshot.lessonCount} days"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Agreed price: ${formatMoney(snapshot.priceAmountMinor, snapshot.currencyCode)} per person",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Plan total: ${formatMoney(snapshot.totalAmountMinor(participantCount), snapshot.currencyCode)} " +
                "($participantCount class ${if (participantCount == 1) "participant" else "participants"})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Started ${dateFormat.format(Date(enrollment.startedAtMillis))} · ${enrollment.status.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val pendingCredit = AttendanceCreditStore.unappliedTotalMinor(enrollment.id)
        if (pendingCredit > 0) {
            Text(
                text = "Credit on next bill: ${formatMoney(pendingCredit, snapshot.currencyCode)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (snapshot.rolling) {
            val planSize = snapshot.lessonCount.coerceAtLeast(1)
            val submitted = countSubmittedPlanSessionsInPeriod(
                soldPlanId = enrollment.soldPlanId,
                periodStartedAtMillis = enrollment.planPeriodStartedAtMillis,
            )
            val remaining = (planSize - submitted).coerceAtLeast(0)
            val billingLine = when (enrollment.status) {
                SoldPlanEnrollmentStatus.CANCELLING ->
                    "Finishing current plan: $submitted of $planSize classes with attendance · renewal stopped"
                SoldPlanEnrollmentStatus.CANCELLED ->
                    "Plan completed: $submitted of $planSize classes with attendance in last period"
                else ->
                    "Plan billing: $submitted of $planSize classes with attendance · $remaining until renewal bill"
            }
            Text(
                text = billingLine,
                style = MaterialTheme.typography.labelSmall,
                color = when (enrollment.status) {
                    SoldPlanEnrollmentStatus.CANCELLING -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun BillRow(
    bill: Bill,
    dateFormat: SimpleDateFormat,
    selected: Boolean,
    payment: glide.model.Payment?,
    billingBlocked: Boolean,
    invoiceBlockedReason: String?,
    onClick: () -> Unit,
    onIssuedChange: (Boolean) -> Unit,
    onGenerateInvoice: () -> Unit,
) {
    val canIssue = !billingBlocked || bill.isIssuedToCustomer()
    var showInvoiceBlockedDialog by remember(bill.id) { mutableStateOf(false) }
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = bill.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = glideListItemTitleColor(selected),
            )
            Text(
                text = formatMoney(bill.displayBillingTotalMinor(), bill.currencyCode),
                style = MaterialTheme.typography.bodyMedium,
                color = glideListItemTitleColor(selected),
            )
        }
        val creditApplied = bill.creditAppliedMinor()
        if (creditApplied > 0) {
            Text(
                text = "Includes ${formatMoney(creditApplied, bill.currencyCode)} credit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bill.status.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = dateFormat.format(Date(bill.displayDateMillis())),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (bill.status != BillStatus.VOID && bill.status != BillStatus.PAID) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = bill.isIssuedToCustomer(),
                        onCheckedChange = { issued ->
                            if (issued && bill.status == BillStatus.SCHEDULED) {
                                onIssuedChange(true)
                            }
                        },
                        enabled = bill.status == BillStatus.SCHEDULED && canIssue,
                    )
                    Text(
                        text = "Issued",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                GlideTextButton(
                    onClick = {
                        if (!canIssue) {
                            showInvoiceBlockedDialog = true
                        } else {
                            onGenerateInvoice()
                        }
                    },
                    enabled = true,
                ) {
                    Text("Invoice")
                }
            }
        }
        payment?.let {
            Text(
                text = "Paid via ${it.method.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showInvoiceBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showInvoiceBlockedDialog = false },
            title = { Text("Cannot generate invoice") },
            text = {
                Text(
                    invoiceBlockedReason
                        ?: "This invoice cannot be generated right now.",
                )
            },
            confirmButton = {
                GlideTextButton(onClick = { showInvoiceBlockedDialog = false }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun BillDetailActions(
    bill: Bill,
    billingBlocked: Boolean,
    invoiceBlockedReason: String?,
    onRecordPayment: () -> Unit,
    onVoid: () -> Unit,
    onGenerateInvoice: () -> Unit,
    onGenerateReceipt: () -> Unit,
) {
    val canIssue = !billingBlocked || bill.isIssuedToCustomer()
    var showInvoiceBlockedDialog by remember(bill.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Selected bill",
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        when (bill.status) {
            BillStatus.SCHEDULED -> {
                Text(
                    text = "Scheduled billing line — edit charges below, then mark Issued when sent to the customer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                BillLineItemsSection(bill = bill)
                Spacer(modifier = Modifier.height(8.dp))
                GlideOutlinedButton(
                    onClick = {
                        if (!canIssue) {
                            showInvoiceBlockedDialog = true
                        } else {
                            onGenerateInvoice()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                ) {
                    Text("Generate invoice PDF")
                }
                if (bill.canBeVoided()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    GlideOutlinedButton(
                        onClick = onVoid,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Void bill", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            BillStatus.ISSUED -> {
                Text(
                    text = "Issued to customer — billing details are locked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                BillLineItemsSection(bill = bill)
                Spacer(modifier = Modifier.height(8.dp))
                GlideButton(
                    onClick = onRecordPayment,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Record payment")
                }
                Spacer(modifier = Modifier.height(4.dp))
                GlideOutlinedButton(
                    onClick = {
                        if (!canIssue) {
                            showInvoiceBlockedDialog = true
                        } else {
                            onGenerateInvoice()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                ) {
                    Text("Generate invoice PDF")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This bill has been issued and cannot be voided.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BillStatus.PAID -> {
                BillLineItemsSection(bill = bill)
                Spacer(modifier = Modifier.height(8.dp))
                PaymentStore.forBill(bill.id)?.let { payment ->
                    Text(
                        text = "Paid ${formatMoney(payment.amountMinor, payment.currencyCode)} via ${payment.method.label}" +
                            payment.reference.takeIf { it.isNotBlank() }?.let { " (ref: $it)" }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                GlideOutlinedButton(
                    onClick = onGenerateReceipt,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Generate receipt PDF")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This bill is paid.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BillStatus.VOID -> {
                Text(
                    text = "This bill was voided.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (showInvoiceBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showInvoiceBlockedDialog = false },
            title = { Text("Cannot generate invoice") },
            text = {
                Text(
                    invoiceBlockedReason
                        ?: "This invoice cannot be generated right now.",
                )
            },
            confirmButton = {
                GlideTextButton(onClick = { showInvoiceBlockedDialog = false }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun BillLineItemsSection(
    bill: Bill,
) {
    val editable = bill.isBillingEditable()
    val lineItems = bill.displayBillingLineItems()
    val totalMinor = bill.displayBillingTotalMinor()
    var lineItemError by remember(bill.id, bill.status) { mutableStateOf<String?>(null) }
    var editingLineItemId by remember(bill.id, bill.status) { mutableStateOf<String?>(null) }
    var showAddDialog by remember(bill.id, bill.status) { mutableStateOf(false) }
    var addKind by remember(bill.id, bill.status) { mutableStateOf(BillLineItemKind.DEBIT) }
    var pendingRemoveLineItemId by remember(bill.id, bill.status) { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (editable) "Bill items" else "Issued bill items",
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(4.dp))
        lineItems.forEach { item ->
            if (editable && editingLineItemId == item.id && item.isEditableBeforeIssue()) {
                BillLineItemEditor(
                    item = item,
                    currencyCode = bill.currencyCode,
                    onSave = { description, amountMajor, kind ->
                        val amountMinor = parseMajorAmount(amountMajor)?.let { majorToMinor(it) }
                        if (description.isBlank() || amountMinor == null || amountMinor <= 0) {
                            lineItemError = "Enter a description and amount greater than zero."
                            return@BillLineItemEditor
                        }
                        if (!BillStore.updateLineItem(bill.id, item.id, description, amountMinor, kind)) {
                            lineItemError = "Could not update line item."
                            return@BillLineItemEditor
                        }
                        lineItemError = null
                        editingLineItemId = null
                    },
                    onCancel = { editingLineItemId = null },
                )
            } else {
                BillLineItemRow(
                    item = item,
                    currencyCode = bill.currencyCode,
                    editable = editable && item.isEditableBeforeIssue(),
                    onEdit = { editingLineItemId = item.id },
                    onDelete = { pendingRemoveLineItemId = item.id },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Total due",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatMoney(totalMinor, bill.currencyCode),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        if (editable) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlideOutlinedButton(
                    onClick = {
                        addKind = BillLineItemKind.DEBIT
                        showAddDialog = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Add debit")
                }
                GlideOutlinedButton(
                    onClick = {
                        addKind = BillLineItemKind.CREDIT
                        showAddDialog = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Add credit")
                }
            }
        }
        lineItemError?.let { error ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    pendingRemoveLineItemId?.let { lineItemId ->
        val item = lineItems.find { it.id == lineItemId }
        DeleteConfirmDialog(
            title = "Remove line item?",
            message = item?.let { "\"${it.description}\" will be removed from this bill." }
                ?: "This line item will be removed from the bill.",
            onDismiss = { pendingRemoveLineItemId = null },
            onContinue = {
                if (!BillStore.removeLineItem(bill.id, lineItemId)) {
                    lineItemError = "Could not remove line item."
                } else {
                    lineItemError = null
                    if (editingLineItemId == lineItemId) editingLineItemId = null
                }
                pendingRemoveLineItemId = null
            },
        )
    }

    if (showAddDialog) {
        AddBillLineItemDialog(
            kind = addKind,
            currencyCode = bill.currencyCode,
            onDismiss = { showAddDialog = false },
            onConfirm = { description, amountMajor, kind ->
                val amountMinor = parseMajorAmount(amountMajor)?.let { majorToMinor(it) }
                if (description.isBlank() || amountMinor == null || amountMinor <= 0) {
                    lineItemError = "Enter a description and amount greater than zero."
                    return@AddBillLineItemDialog
                }
                if (!BillStore.addLineItem(bill.id, description, amountMinor, kind)) {
                    lineItemError = "Could not add line item."
                    return@AddBillLineItemDialog
                }
                lineItemError = null
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun BillLineItemRow(
    item: BillLineItem,
    currencyCode: String,
    editable: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val amountPrefix = if (item.kind == BillLineItemKind.CREDIT) "−" else ""
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = buildString {
                    append(item.kind.label)
                    when (item.source) {
                        glide.model.BillLineItemSource.ATTENDANCE_CREDIT -> append(" · attendance")
                        glide.model.BillLineItemSource.LOCKED -> append(" · issued")
                        else -> Unit
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$amountPrefix${formatMoney(item.amountMinor, currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (editable) {
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                GlideTextButton(onClick = onEdit) { Text("Edit") }
                GlideTextButton(onClick = onDelete) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillLineItemEditor(
    item: BillLineItem,
    currencyCode: String,
    onSave: (description: String, amountMajor: String, kind: BillLineItemKind) -> Unit,
    onCancel: () -> Unit,
) {
    var description by remember(item.id) { mutableStateOf(item.description) }
    var amountMajor by remember(item.id) { mutableStateOf(minorToMajorString(item.amountMinor)) }
    var kind by remember(item.id) { mutableStateOf(item.kind) }
    var kindExpanded by remember(item.id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        GlideOutlinedField(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        GlideOutlinedField(
            value = amountMajor,
            onValueChange = { amountMajor = it },
            label = "Amount ($currencyCode)",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        GlideFieldLabel("Type")
        Spacer(modifier = Modifier.height(2.dp))
        ExposedDropdownMenuBox(
            expanded = kindExpanded,
            onExpandedChange = { kindExpanded = it },
        ) {
            OutlinedTextField(
                value = kind.label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(),
            )
            ExposedDropdownMenu(
                expanded = kindExpanded,
                onDismissRequest = { kindExpanded = false },
            ) {
                BillLineItemKind.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.label) },
                        onClick = {
                            kind = entry
                            kindExpanded = false
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlideButton(
                onClick = { onSave(description, amountMajor, kind) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Save")
            }
            GlideOutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillLineItemDialog(
    kind: BillLineItemKind,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amountMajor: String, kind: BillLineItemKind) -> Unit,
) {
    var description by remember(kind) { mutableStateOf("") }
    var amountMajor by remember(kind) { mutableStateOf("") }
    var selectedKind by remember(kind) { mutableStateOf(kind) }
    var kindExpanded by remember(kind) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${selectedKind.label.lowercase()}") },
        text = {
            Column {
                GlideOutlinedField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlideOutlinedField(
                    value = amountMajor,
                    onValueChange = { amountMajor = it },
                    label = "Amount ($currencyCode)",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlideFieldLabel("Type")
                Spacer(modifier = Modifier.height(2.dp))
                ExposedDropdownMenuBox(
                    expanded = kindExpanded,
                    onExpandedChange = { kindExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedKind.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(),
                    )
                    ExposedDropdownMenu(
                        expanded = kindExpanded,
                        onDismissRequest = { kindExpanded = false },
                    ) {
                        BillLineItemKind.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.label) },
                                onClick = {
                                    selectedKind = entry
                                    kindExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            GlideTextButton(onClick = { onConfirm(description, amountMajor, selectedKind) }) {
                Text("Add")
            }
        },
        dismissButton = {
            GlideTextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun CancelRollingPlanDialog(
    enrollment: SoldPlanEnrollment,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val snapshot = enrollment.planSnapshot
    val planSize = snapshot.lessonCount.coerceAtLeast(1)
    val scheduled = countScheduledPlanSessionsInPeriod(
        soldPlanId = enrollment.soldPlanId,
        periodStartedAtMillis = enrollment.planPeriodStartedAtMillis,
    )
    val remaining = (planSize - scheduled).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel plan") },
        text = {
            Column {
                Text(
                    text = "${snapshot.planName} will not renew.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append("This customer will finish their current plan ")
                        append("($scheduled of $planSize classes scheduled")
                        if (remaining > 0) {
                            append(", $remaining more to schedule in this period")
                        }
                        append("). Any scheduled renewal bill will be voided.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            GlideTextButton(onClick = onConfirm) {
                Text("Cancel plan", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            GlideTextButton(onClick = onDismiss) {
                Text("Keep plan")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordPaymentDialog(
    bill: Bill,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod, String) -> Unit,
) {
    var method by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER) }
    var reference by remember { mutableStateOf("") }
    var methodExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column {
                Text(
                    text = "Amount: ${formatMoney(bill.amountMinor, bill.currencyCode)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlideFieldLabel("Payment method")
                Spacer(modifier = Modifier.height(2.dp))
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = it },
                ) {
                    OutlinedTextField(
                        value = method.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(),
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false },
                    ) {
                        PaymentMethod.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.label) },
                                onClick = {
                                    method = entry
                                    methodExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                GlideOutlinedField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = "Reference (optional)",
                    placeholder = "e.g. bank ref",
                )
            }
        },
        confirmButton = {
            GlideTextButton(onClick = { onConfirm(method, reference) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            GlideTextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
