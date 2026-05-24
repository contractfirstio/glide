package glide.billing

import glide.data.PaymentStore
import glide.model.Bill
import glide.model.BillStatus
import glide.model.Payment
import glide.model.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReceiptContent(
    val fromName: String,
    val fromEmail: String,
    val fromPhone: String,
    val receiptNumber: String,
    val invoiceNumber: String,
    val issuedAtMillis: Long,
    val paidAtMillis: Long,
    val billToName: String,
    val billToEmail: String,
    val billToPhone: String,
    val classSchedule: InvoiceClassSchedule?,
    val debitLines: List<InvoiceDebitLine>,
    val creditLines: List<InvoiceCreditLine>,
    val totalAmountMinor: Long,
    val currencyCode: String,
    val paymentMethodLabel: String,
    val paymentReference: String,
) {
    val formattedTotal: String get() = formatMoney(totalAmountMinor, currencyCode)

    val issuedDateLabel: String get() = receiptDateFormat.format(Date(issuedAtMillis))

    val paidDateLabel: String get() = receiptDateFormat.format(Date(paidAtMillis))
}

private val receiptDateFormat = SimpleDateFormat("d MMM yyyy", Locale.UK)

fun Bill.toReceiptContent(): ReceiptContent? {
    if (status != BillStatus.PAID) return null
    val invoice = toInvoiceContent() ?: return null
    val payment = PaymentStore.forBill(id) ?: return null
    return invoice.toReceiptContent(payment)
}

fun InvoiceContent.toReceiptContent(payment: Payment): ReceiptContent = ReceiptContent(
    fromName = fromName,
    fromEmail = fromEmail,
    fromPhone = fromPhone,
    receiptNumber = invoiceNumber,
    invoiceNumber = invoiceNumber,
    issuedAtMillis = issuedAtMillis,
    paidAtMillis = payment.receivedAtMillis,
    billToName = billToName,
    billToEmail = billToEmail,
    billToPhone = billToPhone,
    classSchedule = classSchedule,
    debitLines = debitLines,
    creditLines = creditLines,
    totalAmountMinor = payment.amountMinor,
    currencyCode = currencyCode,
    paymentMethodLabel = payment.method.label,
    paymentReference = payment.reference,
)
