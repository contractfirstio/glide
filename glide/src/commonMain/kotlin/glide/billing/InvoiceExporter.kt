package glide.billing

import glide.model.Bill

/** Platform hook: writes an invoice PDF when a bill is issued. */
expect object InvoiceExporter {
    fun onBillIssued(bill: Bill)
}
