package glide.billing

import glide.model.Bill
import java.awt.Desktop
import java.io.File

actual object InvoiceExporter {
    actual fun onBillIssued(bill: Bill) {
        val content = bill.toInvoiceContent() ?: return
        val file = runCatching { InvoicePdfWriter.write(content, bill.id) }.getOrNull() ?: return
        if (Desktop.isDesktopSupported()) {
            runCatching { Desktop.getDesktop().open(file) }
        }
    }
}
