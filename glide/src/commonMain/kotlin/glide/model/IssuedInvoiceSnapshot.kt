package glide.model

/** Frozen invoice fields captured when a bill is issued — survives later edits to clients, classes, and settings. */
data class IssuedInvoiceDebitLine(
    val description: String,
    val amountMinor: Long,
)

data class IssuedInvoiceCreditLine(
    val description: String,
    val amountMinor: Long,
)

data class IssuedInvoiceClassSchedule(
    val className: String,
    val classDetails: String,
    val locationAddressLines: List<String>,
    val studentNamesLabel: String,
    val billingWindowStartLabel: String,
    val scheduledSessionLabels: List<String>,
)

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
