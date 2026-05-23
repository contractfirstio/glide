package glide.billing

import glide.data.PeopleGroupStore
import glide.data.resolveMainContact
import glide.model.Bill
import glide.model.BillStatus
import glide.model.displayDateMillis
import glide.model.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val INVOICE_FROM_NAME = "Glide Dance Studio"

data class InvoiceContent(
    val invoiceNumber: String,
    val issuedAtMillis: Long,
    val dueAtMillis: Long?,
    val billToName: String,
    val billToEmail: String,
    val billToPhone: String,
    val lineDescription: String,
    val amountMinor: Long,
    val currencyCode: String,
    val status: BillStatus,
) {
    val formattedAmount: String get() = formatMoney(amountMinor, currencyCode)

    val issuedDateLabel: String get() = invoiceDateFormat.format(Date(issuedAtMillis))

    val dueDateLabel: String
        get() = dueAtMillis?.let { invoiceDateFormat.format(Date(it)) } ?: "On receipt"
}

private val invoiceDateFormat = SimpleDateFormat("d MMM yyyy", Locale.UK)

fun Bill.toInvoiceContent(): InvoiceContent? {
    val group = PeopleGroupStore.findById(peopleGroupId) ?: return null
    val main = group.resolveMainContact()
    return InvoiceContent(
        invoiceNumber = id.replace("-", "").take(8).uppercase(Locale.UK),
        issuedAtMillis = displayDateMillis(),
        dueAtMillis = dueAtMillis,
        billToName = main.name.ifBlank { "Customer" },
        billToEmail = main.email,
        billToPhone = main.phone,
        lineDescription = description,
        amountMinor = amountMinor,
        currencyCode = currencyCode,
        status = status,
    )
}
