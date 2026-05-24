package glide.billing

import glide.data.AppSettingsStore
import glide.data.BillStore
import glide.data.BillingCreditStore
import glide.data.PeopleGroupStore
import glide.data.grossAmountMinorResolved
import glide.data.packLineDescription
import glide.data.resolveMainContact
import glide.model.Bill
import glide.model.IssuedInvoiceClassSchedule
import glide.model.IssuedInvoiceCreditLine
import glide.model.IssuedInvoiceSnapshot
import glide.model.displayDateMillis
import glide.model.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InvoiceCreditLine(
    val description: String,
    val amountMinor: Long,
) {
    fun formattedAmount(currencyCode: String): String = "-${formatMoney(amountMinor, currencyCode)}"
}

data class InvoiceContent(
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
    val packLineDescription: String,
    val classSchedule: InvoiceClassSchedule?,
    val grossAmountMinor: Long,
    val creditLines: List<InvoiceCreditLine>,
    val totalAmountMinor: Long,
    val currencyCode: String,
) {
    val formattedGross: String get() = formatMoney(grossAmountMinor, currencyCode)

    val formattedTotal: String get() = formatMoney(totalAmountMinor, currencyCode)

    val issuedDateLabel: String get() = invoiceDateFormat.format(Date(issuedAtMillis))

    val dueDateLabel: String
        get() = dueAtMillis?.let { invoiceDateFormat.format(Date(it)) } ?: "On receipt"
}

private val invoiceDateFormat = SimpleDateFormat("d MMM yyyy", Locale.UK)

fun Bill.toInvoiceContent(): InvoiceContent? {
    issuedInvoiceSnapshot?.let { return it.toInvoiceContent() }
    val bill = BillStore.reconcileBillCredits(id) ?: this
    return bill.toLiveInvoiceContent()
}

/** Builds invoice content from current stores without reconciling credits or reading a stored snapshot. */
fun Bill.toLiveInvoiceContent(): InvoiceContent? {
    val group = PeopleGroupStore.findById(peopleGroupId) ?: return null
    val main = group.resolveMainContact()
    val gross = grossAmountMinorResolved()
    val creditLines = BillingCreditStore.appliedToBill(id).map { credit ->
        InvoiceCreditLine(
            description = credit.description,
            amountMinor = credit.amountMinor,
        )
    }
    return InvoiceContent(
        fromName = AppSettingsStore.legalCompanyName,
        fromEmail = AppSettingsStore.companyEmail,
        fromPhone = AppSettingsStore.companyPhone,
        fpsNumber = AppSettingsStore.fpsNumber,
        invoiceNumber = id.replace("-", "").take(8).uppercase(Locale.UK),
        issuedAtMillis = displayDateMillis(),
        dueAtMillis = dueAtMillis,
        billToName = main.name.ifBlank { "Customer" },
        billToEmail = main.email,
        billToPhone = main.phone,
        packLineDescription = packLineDescription(),
        classSchedule = toInvoiceClassSchedule(),
        grossAmountMinor = gross,
        creditLines = creditLines,
        totalAmountMinor = amountMinor,
        currencyCode = currencyCode,
    )
}

fun InvoiceContent.toIssuedInvoiceSnapshot(): IssuedInvoiceSnapshot = IssuedInvoiceSnapshot(
    fromName = fromName,
    fromEmail = fromEmail,
    fromPhone = fromPhone,
    fpsNumber = fpsNumber,
    invoiceNumber = invoiceNumber,
    issuedAtMillis = issuedAtMillis,
    dueAtMillis = dueAtMillis,
    billToName = billToName,
    billToEmail = billToEmail,
    billToPhone = billToPhone,
    packLineDescription = packLineDescription,
    classSchedule = classSchedule?.toIssuedInvoiceClassSchedule(),
    grossAmountMinor = grossAmountMinor,
    creditLines = creditLines.map { it.toIssuedInvoiceCreditLine() },
    totalAmountMinor = totalAmountMinor,
    currencyCode = currencyCode,
)

fun IssuedInvoiceSnapshot.toInvoiceContent(): InvoiceContent = InvoiceContent(
    fromName = fromName,
    fromEmail = fromEmail,
    fromPhone = fromPhone,
    fpsNumber = fpsNumber,
    invoiceNumber = invoiceNumber,
    issuedAtMillis = issuedAtMillis,
    dueAtMillis = dueAtMillis,
    billToName = billToName,
    billToEmail = billToEmail,
    billToPhone = billToPhone,
    packLineDescription = packLineDescription,
    classSchedule = classSchedule?.toInvoiceClassSchedule(),
    grossAmountMinor = grossAmountMinor,
    creditLines = creditLines.map { it.toInvoiceCreditLine() },
    totalAmountMinor = totalAmountMinor,
    currencyCode = currencyCode,
)

private fun InvoiceClassSchedule.toIssuedInvoiceClassSchedule(): IssuedInvoiceClassSchedule =
    IssuedInvoiceClassSchedule(
        className = className,
        classDetails = classDetails,
        locationAddressLines = locationAddressLines,
        studentNamesLabel = studentNamesLabel,
        billingWindowStartLabel = billingWindowStartLabel,
        scheduledSessionLabels = scheduledSessionLabels,
    )

private fun IssuedInvoiceClassSchedule.toInvoiceClassSchedule(): InvoiceClassSchedule =
    InvoiceClassSchedule(
        className = className,
        classDetails = classDetails,
        locationAddressLines = locationAddressLines,
        studentNamesLabel = studentNamesLabel,
        billingWindowStartLabel = billingWindowStartLabel,
        scheduledSessionLabels = scheduledSessionLabels,
    )

private fun InvoiceCreditLine.toIssuedInvoiceCreditLine(): IssuedInvoiceCreditLine =
    IssuedInvoiceCreditLine(description = description, amountMinor = amountMinor)

private fun IssuedInvoiceCreditLine.toInvoiceCreditLine(): InvoiceCreditLine =
    InvoiceCreditLine(description = description, amountMinor = amountMinor)
