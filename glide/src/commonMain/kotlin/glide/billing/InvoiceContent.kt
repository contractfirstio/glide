package glide.billing

import glide.data.AppSettingsStore
import glide.data.BillStore
import glide.data.BillingCreditStore
import glide.data.PeopleGroupStore
import glide.data.grossAmountMinorResolved
import glide.data.packLineDescription
import glide.data.resolveMainContact
import glide.model.Bill
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
    val bill = BillStore.reconcileBillCredits(id) ?: this
    val group = PeopleGroupStore.findById(bill.peopleGroupId) ?: return null
    val main = group.resolveMainContact()
    val gross = bill.grossAmountMinorResolved()
    val creditLines = BillingCreditStore.appliedToBill(bill.id).map { credit ->
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
        invoiceNumber = bill.id.replace("-", "").take(8).uppercase(Locale.UK),
        issuedAtMillis = bill.displayDateMillis(),
        dueAtMillis = bill.dueAtMillis,
        billToName = main.name.ifBlank { "Customer" },
        billToEmail = main.email,
        billToPhone = main.phone,
        packLineDescription = bill.packLineDescription(),
        classSchedule = bill.toInvoiceClassSchedule(),
        grossAmountMinor = gross,
        creditLines = creditLines,
        totalAmountMinor = bill.amountMinor,
        currencyCode = bill.currencyCode,
    )
}
