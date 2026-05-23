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
import glide.data.openPendingAttendanceSession
import glide.ui.scheduling.rememberPendingAttendanceSessions
import glide.data.PackEnrollmentStore
import glide.data.RollingPackBillingService
import glide.data.countScheduledPackSessionsInPeriod
import glide.data.PaymentStore
import glide.data.PeopleGroupStore
import glide.data.PlanStore
import glide.data.memberCount
import glide.data.resolveMainContact
import glide.model.Bill
import glide.model.BillStatus
import glide.model.PackEnrollment
import glide.model.displayDateMillis
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
    val enrollment = PackEnrollmentStore.forPeopleGroup(peopleGroupId)
    val bills = enrollment?.let { e ->
        BillStore.forEnrollment(e.id)
    } ?: emptyList()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    var selectedBillId by remember(peopleGroupId) { mutableStateOf<String?>(null) }
    var showPaymentDialog by remember(peopleGroupId) { mutableStateOf(false) }
    var paymentError by remember(peopleGroupId) { mutableStateOf<String?>(null) }
    var billingActionMessage by remember(peopleGroupId) { mutableStateOf<String?>(null) }

    val pendingAttendance = rememberPendingAttendanceSessions()
    val billingBlockedByAttendance = pendingAttendance.isNotEmpty()

    val spacing = GlideLayout.comfortable
    val outstanding = enrollment?.let { BillStore.outstandingMinorForEnrollment(it.id) } ?: 0L

    LaunchedEffect(enrollment?.id) {
        enrollment?.let { RollingPackBillingService.syncRollingPackBilling(it.peopleGroupId) }
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
                        BillingService.addRenewalBill(peopleGroupId)
                        selectedBillId = null
                    },
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
                        billingBlockedByAttendance = billingBlockedByAttendance,
                        onClick = { selectedBillId = bill.id },
                        onIssuedChange = { issued ->
                            if (issued && billingBlockedByAttendance) {
                                billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                                return@BillRow
                            }
                            if (!BillStore.setIssued(bill.id, issued)) {
                                if (issued && billingBlockedByAttendance) {
                                    billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
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
                            if (!BillStore.generateInvoice(bill.id)) {
                                billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                            } else {
                                billingActionMessage = null
                            }
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
                        billingBlockedByAttendance = billingBlockedByAttendance,
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
                            if (!BillStore.generateInvoice(selectedBill.id)) {
                                billingActionMessage = attendanceBlocksBillIssuanceMessage(pendingAttendance.size)
                            } else {
                                billingActionMessage = null
                            }
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
            Text(
                text = "Pack billing: $scheduled of $packSize scheduled classes · $remaining until renewal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    billingBlockedByAttendance: Boolean,
    onClick: () -> Unit,
    onIssuedChange: (Boolean) -> Unit,
    onGenerateInvoice: () -> Unit,
) {
    val canIssue = !billingBlockedByAttendance || bill.isIssuedToCustomer()
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
                text = formatMoney(bill.amountMinor, bill.currencyCode),
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
                        onCheckedChange = onIssuedChange,
                        enabled = (bill.status == BillStatus.SCHEDULED || bill.status == BillStatus.ISSUED) && canIssue,
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
    billingBlockedByAttendance: Boolean,
    onRecordPayment: () -> Unit,
    onVoid: () -> Unit,
    onGenerateInvoice: () -> Unit,
) {
    val canIssue = !billingBlockedByAttendance || bill.isIssuedToCustomer()
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
                    text = "Scheduled billing line — mark Issued when sent to the customer, then record payment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
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
