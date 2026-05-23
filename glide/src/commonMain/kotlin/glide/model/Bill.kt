package glide.model

import java.util.UUID

enum class BillStatus(val label: String) {
    /** Billing line scheduled; counts toward outstanding. */
    SCHEDULED("Scheduled"),
    /** Issued to the customer; awaiting payment. */
    ISSUED("Issued"),
    PAID("Paid"),
    VOID("Void"),
}

data class Bill(
    val id: String = UUID.randomUUID().toString(),
    val enrollmentId: String,
    val peopleGroupId: String,
    val description: String,
    /** Pack total before attendance credits; null on older bills (inferred from credits). */
    val grossAmountMinor: Long? = null,
    /** Amount due after credits applied. */
    val amountMinor: Long,
    val currencyCode: String,
    val status: BillStatus = BillStatus.SCHEDULED,
    val createdAtMillis: Long = System.currentTimeMillis(),
    /** Set when the bill is issued to the customer; null while [status] is [BillStatus.SCHEDULED]. */
    val issuedAtMillis: Long? = null,
    val dueAtMillis: Long? = null,
    val paidAtMillis: Long? = null,
)

fun Bill.isIssuedToCustomer(): Boolean =
    status == BillStatus.ISSUED || status == BillStatus.PAID

fun Bill.displayDateMillis(): Long = issuedAtMillis ?: createdAtMillis
