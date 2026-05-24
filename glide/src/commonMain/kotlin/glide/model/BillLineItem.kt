package glide.model

import java.util.UUID

enum class BillLineItemKind(val label: String) {
    DEBIT("Debit"),
    CREDIT("Credit"),
}

enum class BillLineItemSource {
    /** Plan charge seeded when the bill is created. */
    PLAN,
    /** Auto-synced from an attendance [BillingCredit]. */
    ATTENDANCE_CREDIT,
    /** Added or edited manually before issue. */
    MANUAL,
    /** Frozen at issue; display only. */
    LOCKED,
}

data class BillLineItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    /** Always positive; [kind] determines whether this adds to or reduces the bill total. */
    val amountMinor: Long,
    val kind: BillLineItemKind,
    val source: BillLineItemSource = BillLineItemSource.MANUAL,
    /** Set when [source] is [BillLineItemSource.ATTENDANCE_CREDIT]. */
    val billingCreditId: String? = null,
)
