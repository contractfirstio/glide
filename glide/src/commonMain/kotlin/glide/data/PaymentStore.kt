package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Bill
import glide.model.Payment
import glide.model.PaymentMethod

object PaymentStore {
    private val _payments = mutableStateListOf<Payment>()

    val all: List<Payment> get() = _payments

    fun forBill(billId: String): Payment? = _payments.find { it.billId == billId }

    fun removeAllForSoldPlan(soldPlanId: String) {
        _payments.removeAll { it.soldPlanId == soldPlanId }
    }

    fun recordFullPayment(
        bill: Bill,
        method: PaymentMethod,
        reference: String = "",
        receivedAtMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (bill.status != glide.model.BillStatus.ISSUED) return false
        if (forBill(bill.id) != null) return false
        _payments.add(
            Payment(
                billId = bill.id,
                enrollmentId = bill.enrollmentId,
                soldPlanId = bill.soldPlanId,
                amountMinor = bill.amountMinor,
                currencyCode = bill.currencyCode,
                method = method,
                reference = reference.trim(),
                receivedAtMillis = receivedAtMillis,
            ),
        )
        return BillStore.markPaid(bill.id, receivedAtMillis)
    }
}
