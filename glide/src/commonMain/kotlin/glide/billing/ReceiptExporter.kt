package glide.billing

import glide.model.Bill

/** Platform hook: writes a receipt PDF and opens the default mail client to send it. */
expect object ReceiptExporter {
    fun exportReceipt(bill: Bill): InvoiceExportResult
}
