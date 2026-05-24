package glide.billing

import glide.model.Bill

actual object ReceiptExporter {
    actual fun exportReceipt(bill: Bill): InvoiceExportResult {
        val content = bill.toReceiptContent()
            ?: return InvoiceExportResult.Failure("Could not build receipt.")
        val file = runCatching { ReceiptPdfWriter.write(content, bill.id) }.getOrElse {
            return InvoiceExportResult.Failure("Could not write receipt PDF.")
        }
        return ReceiptEmailComposer.compose(content, file)
    }
}
