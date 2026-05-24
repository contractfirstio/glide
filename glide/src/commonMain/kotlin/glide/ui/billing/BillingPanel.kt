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
import glide.data.BillingCreditStore
import glide.data.attendanceBlocksBillIssuanceMessage
import glide.data.creditAppliedMinor
import glide.data.BillingService
import glide.data.isEditableBeforeIssue
import glide.data.displayBillingLineItems
import glide.data.displayBillingTotalMinor
import glide.data.openPendingAttendanceSession
import glide.data.openSoldPackClassAssignment
import glide.data.soldPackBlocksBillIssuance
import glide.data.soldPackBlocksBillIssuanceMessage
import glide.ui.scheduling.rememberPendingAttendanceSessions
import glide.data.PackEnrollmentStore
import glide.data.RollingPackBillingService
import glide.data.RollingPackCancellationService
import glide.data.ScheduledClassStore
import glide.data.countScheduledPackSessionsInPeriod
import glide.model.PackEnrollmentStatus
import glide.data.PaymentStore
import glide.data.PeopleGroupStore
import glide.data.PlanStore
import glide.data.memberCount
import glide.data.resolveMainContact
import glide.model.Bill
import glide.model.BillLineItem
import glide.model.BillLineItemKind
import glide.model.BillStatus
import glide.model.majorToMinor
import glide.model.minorToMajorString
import glide.model.parseMajorAmount
import glide.model.PackEnrollment
import glide.model.displayDateMillis
import glide.model.isBillingEditable
import glide.model.isIssuedToCustomer
import glide.model.PaymentMethod
import glide.model.PeopleGroupType
import glide.model.formatMoney
import glide.ui.layout.GlideLayout
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

fun billingPanelTitle(peopleGroupId: String): String {
    val group = PeopleGroupStore.findById(peopleGroupId) ?: return "Billing"
    val main = group.resolveMainContact()
    val packName = group.planId?.let { PlanStore.findById(it)?.name }
    return when {
        packName != null -> "Billing — $packName"
        main.name.isNotBlank() -> "Billing — ${main.name}"
        else -> "Billing"
    }
}

@Composable
fun BillingPanel(
    peopleGroupId: String,
    modifier: Modifier = Modifier,
) {
    val group = PeopleGroupStore.findById(peopleGroupId)
    val enrollment = PackEnrollmentStore.displayForPeopleGroup(peopleGroupId)
    val ongoingEnrollment = PackEnrollmentStore.forPeopleGroup(peopleGroupId)
    ScheduledClassStore.classes
    val bills = enrollment?.let { e ->
        BillStore.forEnrollment(e.id)
    } ?: emptyList()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    var selectedBillId by remember(peopleGroupId) { mutableStateOf<String?>(null) }
    var showPaymentDialog by remember(peopleGroupId) { mutableStateOf(false) }
    var paymentError by remember(peopleGroupId) { mutableStateOf<String?>(null) }
    var billingActionMessage by remember(peopleGroupId) { mutableStateOf<String?>(null) }
    var showCancelPackDialog by remember(peopleGroupId) { mutableStateOf(false) }

    val pendingAttendance = rememberPendingAttendanceSessions()
    val billingBlockedByAttendance = pendingAttendance.isNotEmpty()
    val billingBlockedByUnassignedClass = soldPackBlocksBillIssuance(peopleGroupId)
    val billingBlocked = billingBlockedByAttendance || billingBlockedByUnassignedClass

    val spacing = GlideLayout.comfortable
    val outstanding = enrollment?.let { BillStore.outstandingMinorForEnrollment(it.id) } ?: 0L

    LaunchedEffect(ongoingEnrollment?.id, ongoingEnrollment?.status) {
        ongoingEnrollment?.let { RollingPackBillingService.syncRollingPackBilling(it.peopleGroupId) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.outer)
            .verticalScroll(rememberScrollState()),
    ) {
        if (group == null || group.type != PeopleGroupType.CUSTOMER) {
            Text(
                text = "Select a customer group in the Customers panel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val main = group.resolveMainContact()
        Text(
            text = buildString {
                append(main.name.ifBlank { "Customer group" })
                group.planId?.let { PlanStore.findById(it)?.name }?.let { append(" · $it") }
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(spacing.section))

        if (enrollment == null) {
            Text(
                text = "No billing enrollment for this pack yet. Enrollment is created when a lead becomes a customer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        FormPanelSection(
            title = "Pack enrollment",
            description = "Agreed plan, pricing, and billing period for this customer group.",
            spacing = spacing,
            role = FormPanelSectionRole.Primary,
        ) {
            EnrollmentSummary(enrollment = enrollment, dateFormat = dateFormat, showBackground = false)

            if (
                ongoingEnrollment != null &&
                enrollment.planSnapshot.rolling &&
                enrollment.status == PackEnrollmentStatus.ACTIVE
            ) {
                Spacer(modifier = Modifier.height(spacing.field))
                GlideOutlinedButton(
                    onClick = { showCancelPackDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel pack", color = MaterialTheme.colorScheme.error)
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
                    text = soldPackBlocksBillIssuanceMessage(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                GlideTextButton(onClick = { openSoldPackClassAssignment(peopleGroupId) }) {
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
                        if (!BillingService.addRenewalBill(peopleGroupId)) {
                            billingActionMessage = when (ongoingEnrollment?.status) {
                                PackEnrollmentStatus.CANCELLING ->
                                    "Renewal is stopped while this pack finishes."
                                else -> "Could not add a renewal bill."
                            }
                        } else {
                            billingActionMessage = null
                        }
                        selectedBillId = null
                    },
                    enabled = ongoingEnrollment?.status == PackEnrollmentStatus.ACTIVE,
                ) {
                    Text("Add bill")
                }
            }
        }

        FormPanelSectionsDivider(label = "Bill history", spacing = spacing)

        FormPanelSection(
            title = "Bills",
            description = "Scheduled, issued, and paid billing lines for this pack.",
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
                        onClick = { selectedBillId = bill.id },
                        onIssuedChange = { issued ->
                            if (issued && billingBlockedByAttendance) {
                                billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                                return@BillRow
                            }
                            if (issued && billingBlockedByUnassignedClass) {
                                billingActionMessage = soldPackBlocksBillIssuanceMessage()
                                return@BillRow
                            }
                            if (!BillStore.setIssued(bill.id, issued = true)) {
                                if (issued && billingBlockedByAttendance) {
                                    billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                                } else if (issued && billingBlockedByUnassignedClass) {
                                    billingActionMessage = soldPackBlocksBillIssuanceMessage()
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
                                billingActionMessage = soldPackBlocksBillIssuanceMessage()
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
                        onRecordPayment = {
                            paymentError = null
                            showPaymentDialog = true
                        },
                        onVoid = {
                            BillStore.voidBill(selectedBill.id)
                            selectedBillId = null
                        },
                        onGenerateInvoice = {
                            if (billingBlockedByAttendance) {
                                billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                                return@BillDetailActions
                            }
                            if (billingBlockedByUnassignedClass) {
                                billingActionMessage = soldPackBlocksBillIssuanceMessage()
                                return@BillDetailActions
                            }
                            billingActionMessage = BillStore.generateInvoice(selectedBill.id)
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
                    }
                },
            )
        }
    }

    if (showCancelPackDialog && enrollment != null) {
        CancelRollingPackDialog(
            enrollment = enrollment,
            onDismiss = { showCancelPackDialog = false },
            onConfirm = {
                if (RollingPackCancellationService.cancelRenewal(enrollment.id)) {
                    billingActionMessage = null
                    showCancelPackDialog = false
                } else {
                    billingActionMessage = "Could not cancel pack renewal."
                }
            },
        )
    }
}

@Composable
private fun EnrollmentSummary(
    enrollment: PackEnrollment,
    dateFormat: SimpleDateFormat,
    showBackground: Boolean = true,
) {
    val snapshot = enrollment.planSnapshot
    val householdSize = PeopleGroupStore.findById(enrollment.peopleGroupId)?.memberCount() ?: 1
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
                glide.model.PlanKind.MULTI_LESSON_PACK -> {
                    val rolling = if (snapshot.rolling) "Rolling" else "Fixed"
                    "${snapshot.lessonCount} classes · $rolling"
                }
                glide.model.PlanKind.SINGLE_LESSON_PACK -> "1 class"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Agreed price: ${formatMoney(snapshot.priceAmountMinor, snapshot.currencyCode)} per person",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Pack total: ${formatMoney(snapshot.totalAmountMinor(householdSize), snapshot.currencyCode)} " +
                "($householdSize ${if (householdSize == 1) "person" else "people"})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Started ${dateFormat.format(Date(enrollment.startedAtMillis))} · ${enrollment.status.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val pendingCredit = BillingCreditStore.unappliedTotalMinor(enrollment.id)
        if (pendingCredit > 0) {
            Text(
                text = "Credit on next bill: ${formatMoney(pendingCredit, snapshot.currencyCode)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (snapshot.rolling) {
            val packSize = snapshot.lessonCount.coerceAtLeast(1)
            val scheduled = countScheduledPackSessionsInPeriod(
                peopleGroupId = enrollment.peopleGroupId,
                periodStartedAtMillis = enrollment.packPeriodStartedAtMillis,
            )
            val remaining = (packSize - scheduled).coerceAtLeast(0)
            val billingLine = when (enrollment.status) {
                PackEnrollmentStatus.CANCELLING ->
                    "Finishing current pack: $scheduled of $packSize scheduled classes · renewal stopped"
                PackEnrollmentStatus.CANCELLED ->
                    "Pack completed: $scheduled of $packSize scheduled classes in last period"
                else ->
                    "Pack billing: $scheduled of $packSize scheduled classes · $remaining until renewal"
            }
            Text(
                text = billingLine,
                style = MaterialTheme.typography.labelSmall,
                color = when (enrollment.status) {
                    PackEnrollmentStatus.CANCELLING -> MaterialTheme.colorScheme.primary
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
    onClick: () -> Unit,
    onIssuedChange: (Boolean) -> Unit,
    onGenerateInvoice: () -> Unit,
) {
    val canIssue = !billingBlocked || bill.isIssuedToCustomer()
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
                    onClick = onGenerateInvoice,
                    enabled = canIssue,
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
}

@Composable
private fun BillDetailActions(
    bill: Bill,
    billingBlocked: Boolean,
    onRecordPayment: () -> Unit,
    onVoid: () -> Unit,
    onGenerateInvoice: () -> Unit,
) {
    val canIssue = !billingBlocked || bill.isIssuedToCustomer()
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
                    onClick = onGenerateInvoice,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canIssue,
                ) {
                    Text("Generate invoice PDF")
                }
                Spacer(modifier = Modifier.height(4.dp))
                GlideOutlinedButton(
                    onClick = onVoid,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Void bill", color = MaterialTheme.colorScheme.error)
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
                    onClick = onGenerateInvoice,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canIssue,
                ) {
                    Text("Generate invoice PDF")
                }
                Spacer(modifier = Modifier.height(4.dp))
                GlideOutlinedButton(
                    onClick = onVoid,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Void bill", color = MaterialTheme.colorScheme.error)
                }
            }
            BillStatus.PAID -> {
                BillLineItemsSection(bill = bill)
                Spacer(modifier = Modifier.height(8.dp))
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
                    onDelete = {
                        if (!BillStore.removeLineItem(bill.id, item.id)) {
                            lineItemError = "Could not remove line item."
                        } else {
                            lineItemError = null
                            if (editingLineItemId == item.id) editingLineItemId = null
                        }
                    },
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
private fun CancelRollingPackDialog(
    enrollment: PackEnrollment,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val snapshot = enrollment.planSnapshot
    val packSize = snapshot.lessonCount.coerceAtLeast(1)
    val scheduled = countScheduledPackSessionsInPeriod(
        peopleGroupId = enrollment.peopleGroupId,
        periodStartedAtMillis = enrollment.packPeriodStartedAtMillis,
    )
    val remaining = (packSize - scheduled).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel pack") },
        text = {
            Column {
                Text(
                    text = "${snapshot.planName} will not renew.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append("This customer will finish their current pack ")
                        append("($scheduled of $packSize classes scheduled")
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
                Text("Cancel pack", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            GlideTextButton(onClick = onDismiss) {
                Text("Keep pack")
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
