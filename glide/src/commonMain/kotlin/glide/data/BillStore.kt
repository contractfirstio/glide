package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.billing.InvoiceExporter
import glide.model.Bill
import glide.model.BillStatus
import glide.model.PackEnrollment
import glide.model.formatMoney

object BillStore {
    private val _bills = mutableStateListOf<Bill>()

    val all: List<Bill> get() = _bills

    fun forEnrollment(enrollmentId: String): List<Bill> =
        _bills.filter { it.enrollmentId == enrollmentId }.sortedByDescending { it.issuedAtMillis }

    fun forPeopleGroup(peopleGroupId: String): List<Bill> =
        _bills.filter { it.peopleGroupId == peopleGroupId }.sortedByDescending { it.issuedAtMillis }

    fun findById(id: String): Bill? = _bills.find { it.id == id }

    fun issueInitialPackBill(
        enrollment: PackEnrollment,
        exportInvoice: Boolean = true,
    ): Bill = addIssuedBill(
        enrollment = enrollment,
        description = enrollment.planSnapshot.planName,
        exportInvoice = exportInvoice,
    )

    fun issuePackBill(
        enrollment: PackEnrollment,
        exportInvoice: Boolean = true,
    ): Bill = addIssuedBill(
        enrollment = enrollment,
        description = "${enrollment.planSnapshot.planName} (renewal)",
        exportInvoice = exportInvoice,
    )

    private fun addIssuedBill(
        enrollment: PackEnrollment,
        description: String,
        exportInvoice: Boolean,
    ): Bill {
        val snapshot = enrollment.planSnapshot
        val householdSize = PeopleGroupStore.findById(enrollment.peopleGroupId)?.memberCount() ?: 1
        val grossMinor = snapshot.totalAmountMinor(householdSize)
        val bill = Bill(
            enrollmentId = enrollment.id,
            peopleGroupId = enrollment.peopleGroupId,
            description = description,
            amountMinor = grossMinor,
            currencyCode = snapshot.currencyCode,
        )
        _bills.add(bill)
        var issuedBill = bill
        val creditApplied = BillingCreditStore.consumeForBill(
            enrollmentId = enrollment.id,
            billId = bill.id,
            maxToApplyMinor = grossMinor,
        )
        if (creditApplied > 0) {
            val netMinor = (grossMinor - creditApplied).coerceAtLeast(0)
            val creditLabel = formatMoney(creditApplied, snapshot.currencyCode)
            issuedBill = bill.copy(
                amountMinor = netMinor,
                description = "$description ($creditLabel credit applied)",
            )
            _bills[_bills.lastIndex] = issuedBill
        }
        if (exportInvoice) {
            InvoiceExporter.onBillIssued(issuedBill)
        }
        return issuedBill
    }

    fun markPaid(billId: String, paidAtMillis: Long = System.currentTimeMillis()): Boolean {
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return false
        val bill = _bills[index]
        if (bill.status != BillStatus.ISSUED) return false
        _bills[index] = bill.copy(status = BillStatus.PAID, paidAtMillis = paidAtMillis)
        return true
    }

    fun voidBill(billId: String): Boolean {
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return false
        val bill = _bills[index]
        if (bill.status != BillStatus.ISSUED) return false
        _bills[index] = bill.copy(status = BillStatus.VOID)
        return true
    }

    fun outstandingMinorForEnrollment(enrollmentId: String): Long =
        forEnrollment(enrollmentId)
            .filter { it.status == BillStatus.ISSUED }
            .sumOf { it.amountMinor }
}
