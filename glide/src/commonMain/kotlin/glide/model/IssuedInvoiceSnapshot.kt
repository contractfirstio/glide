package glide.model

import kotlinx.serialization.Serializable
/** Frozen invoice fields captured when a bill is issued — survives later edits to clients, classes, and settings. */
@Serializable
data class IssuedInvoiceDebitLine(
    val description: String,
    val amountMinor: Long,
)

@Serializable
data class IssuedInvoiceCreditLine(
    val description: String,
    val amountMinor: Long,
)

@Serializable
data class IssuedInvoiceClassSchedule(
    val className: String,
    val classDetails: String,
    val locationAddressLines: List<String>,
    val studentNamesLabel: String,
    val billingWindowStartLabel: String,
    val scheduledSessionLabels: List<String>,
)

@Serializable
data class IssuedInvoiceSnapshot(
    val fromName: String,
    val fromEmail: String,
    val fromPhone: String,
    val fpsNumber: String,
    val invoiceNumber: String,
    val issuedAtMillis: Long,
    val dueAtMillis: Long?,
    val billToName: String,
    val billToEmail: String,
    val billToPhone: String,
    val classSchedule: IssuedInvoiceClassSchedule?,
    val debitLines: List<IssuedInvoiceDebitLine>,
    val creditLines: List<IssuedInvoiceCreditLine>,
    val totalAmountMinor: Long,
    val currencyCode: String,
)