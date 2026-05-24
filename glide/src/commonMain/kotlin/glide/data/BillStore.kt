package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.billing.InvoiceExporter
import glide.model.Bill
import glide.model.BillStatus
import glide.model.PackEnrollment

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
        )
        _bills.add(bill)
        return reconcileBillCredits(bill.id) ?: bill
    }

    /** Applies any unallocated credits to an open bill and updates the net amount due. */
    fun reconcileBillCredits(billId: String): Bill? {
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return null
        val bill = _bills[index]
        if (bill.status == BillStatus.PAID || bill.status == BillStatus.VOID) return bill

        val gross = bill.grossAmountMinorResolved()
        val remainingDue = bill.amountMinor
        val extraApplied = if (remainingDue > 0) {
            BillingCreditStore.consumeForBill(
                enrollmentId = bill.enrollmentId,
                billId = billId,
                maxToApplyMinor = remainingDue,
            )
        } else {
            0L
        }

        val updated = bill.copy(
            grossAmountMinor = bill.grossAmountMinor ?: gross,
            amountMinor = (remainingDue - extraApplied).coerceAtLeast(0),
        )
        if (updated != bill) {
            _bills[index] = updated
        }
        return _bills[index]
    }

    fun applyPendingCreditsToOpenBills(enrollmentId: String) {
        forEnrollment(enrollmentId)
            .filter { it.status == BillStatus.SCHEDULED || it.status == BillStatus.ISSUED }
            .forEach { reconcileBillCredits(it.id) }
    }

    fun setIssued(billId: String, issued: Boolean, issuedAtMillis: Long = System.currentTimeMillis()): Boolean {
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return false
        val bill = _bills[index]
        return if (issued) {
            if (hasOutstandingAttendanceSubmissions()) return false
            if (soldPackBlocksBillIssuance(bill.peopleGroupId)) return false
            if (bill.status != BillStatus.SCHEDULED) return false
            _bills[index] = bill.copy(
                status = BillStatus.ISSUED,
                issuedAtMillis = issuedAtMillis,
                dueAtMillis = bill.dueAtMillis ?: issuedAtMillis,
            )
            true
        } else {
            if (bill.status != BillStatus.ISSUED) return false
            _bills[index] = bill.copy(
                status = BillStatus.SCHEDULED,
                issuedAtMillis = null,
                dueAtMillis = null,
            )
            true
        }
    }

    fun generateInvoice(billId: String): Boolean {
        if (!AppSettingsStore.isConfigured) return false
        val bill = findById(billId) ?: return false
        if (hasOutstandingAttendanceSubmissions()) return false
        if (soldPackBlocksBillIssuance(bill.peopleGroupId)) return false
        val reconciled = reconcileBillCredits(billId) ?: return false
        InvoiceExporter.exportInvoice(reconciled)
        return true
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
