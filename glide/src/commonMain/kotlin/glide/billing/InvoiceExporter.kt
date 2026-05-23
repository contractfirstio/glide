package glide.billing

import glide.model.Bill

/** Platform hook: writes an invoice PDF for a bill. */
expect object InvoiceExporter {
    fun exportInvoice(bill: Bill)
}
