package glide.billing

sealed class InvoiceExportResult {
    data object Success : InvoiceExportResult()

    data class Failure(val message: String) : InvoiceExportResult()
}
