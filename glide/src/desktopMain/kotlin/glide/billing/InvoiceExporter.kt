package glide.billing

import glide.model.Bill

actual object InvoiceExporter {
    actual fun exportInvoice(bill: Bill): InvoiceExportResult {
        val content = bill.toInvoiceContent()
            ?: return InvoiceExportResult.Failure("Could not build invoice.")
        val file = runCatching { InvoicePdfWriter.write(content, bill.id) }.getOrElse {
            return InvoiceExportResult.Failure("Could not write invoice PDF.")
        }
        return InvoiceEmailComposer.compose(content, file)
    }
}
