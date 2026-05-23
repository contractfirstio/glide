package glide.model

import java.util.UUID

enum class BillStatus(val label: String) {
    ISSUED("Issued"),
    PAID("Paid"),
    VOID("Void"),
}

data class Bill(
    val id: String = UUID.randomUUID().toString(),
    val enrollmentId: String,
    val peopleGroupId: String,
    val description: String,
    val amountMinor: Long,
    val currencyCode: String,
    val status: BillStatus = BillStatus.ISSUED,
    val issuedAtMillis: Long = System.currentTimeMillis(),
    val dueAtMillis: Long? = null,
    val paidAtMillis: Long? = null,
)
