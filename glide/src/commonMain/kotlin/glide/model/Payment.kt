package glide.model

import java.util.UUID

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    BANK_TRANSFER("Bank transfer"),
    CARD("Card"),
    OTHER("Other"),
}

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val billId: String,
    val enrollmentId: String,
    val soldPlanId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val method: PaymentMethod,
    val reference: String = "",
    val receivedAtMillis: Long = System.currentTimeMillis(),
)
