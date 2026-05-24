package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.billing.InvoiceExportResult
import glide.billing.InvoiceExporter
import glide.billing.toIssuedInvoiceSnapshot
import glide.billing.toLiveInvoiceContent
import glide.model.Bill
import glide.model.BillLineItem
import glide.model.BillLineItemKind
import glide.model.BillLineItemSource
import glide.model.BillStatus
import glide.model.PackEnrollment
import glide.model.billPaymentDueAtMillis
import glide.model.isBillingEditable

object BillStore {
    private val _bills = mutableStateListOf<Bill>()

    val all: List<Bill> get() = _bills

    fun forEnrollment(enrollmentId: String): List<Bill> =
        _bills.filter { it.enrollmentId == enrollmentId }.sortedByDescending { it.createdAtMillis }

    fun forPeopleGroup(peopleGroupId: String): List<Bill> =
        _bills.filter { it.peopleGroupId == peopleGroupId }.sortedByDescending { it.createdAtMillis }

    fun findById(id: String): Bill? = _bills.find { it.id == id }

    fun createInitialPackBill(enrollment: PackEnrollment): Bill =
        addPackBill(enrollment = enrollment, description = enrollment.planSnapshot.planName)

    fun createRenewalBill(enrollment: PackEnrollment): Bill =
        addPackBill(
            enrollment = enrollment,
            description = "${enrollment.planSnapshot.planName} (renewal)",
        )

    private fun addPackBill(enrollment: PackEnrollment, description: String): Bill {
        val snapshot = enrollment.planSnapshot
        val householdSize = PeopleGroupStore.findById(enrollment.peopleGroupId)?.memberCount() ?: 1
        val grossMinor = snapshot.totalAmountMinor(householdSize)
        val bill = Bill(
            enrollmentId = enrollment.id,
            peopleGroupId = enrollment.peopleGroupId,
            description = description,
            grossAmountMinor = grossMinor,
            amountMinor = grossMinor,
            currencyCode = snapshot.currencyCode,
            status = BillStatus.SCHEDULED,
            lineItems = listOf(
                BillLineItem(
                    description = description,
                    amountMinor = grossMinor,
                    kind = BillLineItemKind.DEBIT,
                    source = BillLineItemSource.PACK,
                ),
            ),
        )
        _bills.add(bill)
        return reconcileBillCredits(bill.id) ?: bill
    }

    /** Applies any unallocated credits to a scheduled bill and updates the net amount due. */
    fun reconcileBillCredits(billId: String): Bill? {
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return null
        val bill = _bills[index].ensureLineItems()
        if (!bill.isBillingEditable()) return bill

        val subtotal = bill.subtotalBeforeAttendanceCreditsMinor()
        if (subtotal > 0) {
            BillingCreditStore.consumeForBill(
                enrollmentId = bill.enrollmentId,
                billId = billId,
                maxToApplyMinor = subtotal,
            )
        }
        val applied = BillingCreditStore.appliedToBill(billId)
        val updated = bill.recomputeFromLineItems(applied)
        if (updated != _bills[index]) {
            _bills[index] = updated
        }
        return _bills[index]
    }

    fun addLineItem(
        billId: String,
        description: String,
        amountMinor: Long,
        kind: BillLineItemKind,
    ): Boolean = mutateLineItems(billId) { bill, items ->
        items + BillLineItem(
            description = description.trim(),
            amountMinor = amountMinor,
            kind = kind,
            source = BillLineItemSource.MANUAL,
        )
    }

    fun updateLineItem(
        billId: String,
        lineItemId: String,
        description: String,
        amountMinor: Long,
        kind: BillLineItemKind,
    ): Boolean = mutateLineItems(billId) { bill, items ->
        items.map { item ->
            if (item.id != lineItemId || !item.isEditableBeforeIssue()) item
            else item.copy(
                description = description.trim(),
                amountMinor = amountMinor,
                kind = kind,
            )
        }
    }

    fun removeLineItem(billId: String, lineItemId: String): Boolean = mutateLineItems(billId) { _, items ->
        if (items.none { it.id == lineItemId && it.isEditableBeforeIssue() }) null
        else items.filterNot { it.id == lineItemId && it.isEditableBeforeIssue() }
    }

    private inline fun mutateLineItems(
        billId: String,
        transform: (Bill, List<BillLineItem>) -> List<BillLineItem>?,
    ): Boolean {
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return false
        val bill = _bills[index]
        if (!bill.isBillingEditable()) return false
        val ensured = bill.ensureLineItems()
        val baseItems = ensured.lineItems.filter { it.source != BillLineItemSource.ATTENDANCE_CREDIT }
        val nextBaseItems = transform(ensured, baseItems) ?: return false
        if (nextBaseItems.isEmpty()) return false
        _bills[index] = ensured.copy(lineItems = nextBaseItems)
        reconcileBillCredits(billId)
        return true
    }

    fun applyPendingCreditsToOpenBills(enrollmentId: String) {
        forEnrollment(enrollmentId)
            .filter { it.isBillingEditable() }
            .forEach { reconcileBillCredits(it.id) }
    }

    fun setIssued(billId: String, issued: Boolean, issuedAtMillis: Long = System.currentTimeMillis()): Boolean {
        if (!issued) return false
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return false
        val bill = _bills[index]
        if (hasOutstandingAttendanceSubmissions()) return false
        if (soldPackBlocksBillIssuance(bill.peopleGroupId)) return false
        if (bill.status != BillStatus.SCHEDULED) return false
        val reconciled = reconcileBillCredits(billId) ?: return false
        val issuedBill = reconciled.copy(
            status = BillStatus.ISSUED,
            issuedAtMillis = issuedAtMillis,
            dueAtMillis = billPaymentDueAtMillis(issuedAtMillis),
        )
        val snapshot = issuedBill.toLiveInvoiceContent()?.toIssuedInvoiceSnapshot()
        _bills[index] = issuedBill.copy(issuedInvoiceSnapshot = snapshot)
        return true
    }

    /** Returns an error message on failure, or null on success. */
    fun generateInvoice(billId: String): String? {
        if (!AppSettingsStore.isConfigured) return "Company settings are not configured."
        val bill = findById(billId) ?: return "Bill not found."
        if (hasOutstandingAttendanceSubmissions()) return "Could not generate invoice."
        if (soldPackBlocksBillIssuance(bill.peopleGroupId)) return "Could not generate invoice."
        val exportBill = if (bill.issuedInvoiceSnapshot != null) {
            bill
        } else {
            reconcileBillCredits(billId) ?: return "Could not prepare invoice."
        }
        return when (val result = InvoiceExporter.exportInvoice(exportBill)) {
            is InvoiceExportResult.Success -> null
            is InvoiceExportResult.Failure -> result.message
        }
    }

    fun markPaid(billId: String, paidAtMillis: Long = System.currentTimeMillis()): Boolean {
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return false
        val bill = _bills[index]
        if (bill.status != BillStatus.ISSUED) return false
        _bills[index] = bill.copy(status = BillStatus.PAID, paidAtMillis = paidAtMillis)
        RollingPackBillingService.onPackBillPaid(bill.enrollmentId, paidAtMillis)
        return true
    }

    fun hasScheduledRenewalBill(enrollmentId: String): Boolean =
        forEnrollment(enrollmentId).any { it.status == BillStatus.SCHEDULED && it.isRenewalBill() }

    fun voidScheduledRenewalBills(enrollmentId: String): Int {
        var voided = 0
        forEnrollment(enrollmentId)
            .filter { it.status == BillStatus.SCHEDULED && it.isRenewalBill() }
            .forEach { bill ->
                if (voidBill(bill.id)) voided++
            }
        return voided
    }

    fun voidBill(billId: String): Boolean {
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return false
        val bill = _bills[index]
        if (bill.status != BillStatus.ISSUED && bill.status != BillStatus.SCHEDULED) return false
        _bills[index] = bill.copy(status = BillStatus.VOID)
        return true
    }

    fun outstandingMinorForEnrollment(enrollmentId: String): Long {
        applyPendingCreditsToOpenBills(enrollmentId)
        return forEnrollment(enrollmentId)
            .filter { it.status == BillStatus.SCHEDULED || it.status == BillStatus.ISSUED }
            .sumOf { it.amountMinor }
    }
}
