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
    /** Plan total before attendance credits; null on older bills (inferred from credits). */
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
    /** Frozen invoice content captured at issue time; used for identical PDF regeneration. */
    val issuedInvoiceSnapshot: IssuedInvoiceSnapshot? = null,
    /** Editable charge and credit lines while [status] is [BillStatus.SCHEDULED]. */
    val lineItems: List<BillLineItem> = emptyList(),
)

fun Bill.isIssuedToCustomer(): Boolean =
    status == BillStatus.ISSUED || status == BillStatus.PAID

/** Billing line items and amounts can only be edited while scheduled. */
fun Bill.isBillingEditable(): Boolean = status == BillStatus.SCHEDULED

/** Only scheduled bills can be voided (removed). */
fun Bill.canBeVoided(): Boolean = status == BillStatus.SCHEDULED

fun Bill.displayDateMillis(): Long = issuedAtMillis ?: createdAtMillis

/** When payment is expected; [dueAtMillis] or issue date plus [PAYMENT_DUE_DAYS_AFTER_ISSUE]. */
fun Bill.effectivePaymentDueAtMillis(): Long? =
    when (status) {
        BillStatus.ISSUED -> dueAtMillis ?: issuedAtMillis?.let { billPaymentDueAtMillis(it) }
        else -> null
    }

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Payment is due this many whole days after the bill is issued. */
const val PAYMENT_DUE_DAYS_AFTER_ISSUE = 2L

fun billPaymentDueAtMillis(issuedAtMillis: Long): Long =
    issuedAtMillis + PAYMENT_DUE_DAYS_AFTER_ISSUE * DAY_MILLIS

/** Whole days after [effectivePaymentDueAtMillis]; null if not issued or not yet due. */
fun Bill.daysPastPaymentDue(nowMillis: Long = System.currentTimeMillis()): Int? {
    if (status != BillStatus.ISSUED || amountMinor <= 0) return null
    val due = effectivePaymentDueAtMillis() ?: return null
    if (nowMillis < due) return null
    return ((nowMillis - due) / DAY_MILLIS).toInt()
}

fun Bill.isPaymentOverdueByAtLeastDays(
    days: Long = PAYMENT_OVERDUE_ALERT_DAYS,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean = (daysPastPaymentDue(nowMillis) ?: 0) >= days

const val PAYMENT_OVERDUE_ALERT_DAYS = 2L
