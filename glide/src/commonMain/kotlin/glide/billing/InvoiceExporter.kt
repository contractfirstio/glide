package glide.billing

import glide.model.Bill

/** Platform hook: writes an invoice PDF and opens the default mail client to send it. */
expect object InvoiceExporter {
    fun exportInvoice(bill: Bill): InvoiceExportResult
}
