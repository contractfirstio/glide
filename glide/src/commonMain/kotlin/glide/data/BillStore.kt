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
            amountMinor = grossMinor,
            currencyCode = snapshot.currencyCode,
            status = BillStatus.SCHEDULED,
        )
        _bills.add(bill)
        var result = bill
        val creditApplied = BillingCreditStore.consumeForBill(
            enrollmentId = enrollment.id,
            billId = bill.id,
            maxToApplyMinor = grossMinor,
        )
        if (creditApplied > 0) {
            val netMinor = (grossMinor - creditApplied).coerceAtLeast(0)
            val creditLabel = formatMoney(creditApplied, snapshot.currencyCode)
            result = bill.copy(
                amountMinor = netMinor,
                description = "$description ($creditLabel credit applied)",
            )
            _bills[_bills.lastIndex] = result
        }
        return result
    }

    fun setIssued(billId: String, issued: Boolean, issuedAtMillis: Long = System.currentTimeMillis()): Boolean {
        val index = _bills.indexOfFirst { it.id == billId }
        if (index < 0) return false
        val bill = _bills[index]
        return if (issued) {
            if (bill.status != BillStatus.SCHEDULED) return false
            _bills[index] = bill.copy(status = BillStatus.ISSUED, issuedAtMillis = issuedAtMillis)
            true
        } else {
            if (bill.status != BillStatus.ISSUED) return false
            _bills[index] = bill.copy(status = BillStatus.SCHEDULED, issuedAtMillis = null)
            true
        }
    }

    fun generateInvoice(billId: String): Boolean {
        val bill = findById(billId) ?: return false
        InvoiceExporter.exportInvoice(bill)
        return true
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
        if (bill.status != BillStatus.ISSUED && bill.status != BillStatus.SCHEDULED) return false
        _bills[index] = bill.copy(status = BillStatus.VOID)
        return true
    }

    fun outstandingMinorForEnrollment(enrollmentId: String): Long =
        forEnrollment(enrollmentId)
            .filter { it.status == BillStatus.SCHEDULED || it.status == BillStatus.ISSUED }
            .sumOf { it.amountMinor }
}
