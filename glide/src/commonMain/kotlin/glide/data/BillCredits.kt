package glide.data

import glide.model.Bill
import glide.model.BillLineItemKind
import glide.model.isIssuedToCustomer

fun Bill.grossAmountMinorResolved(): Long =
    grossAmountMinor ?: (amountMinor + AttendanceCreditStore.appliedTotalMinorForBill(id))

/** Plan line label without legacy credit suffix in [Bill.description]. */
fun Bill.planLineDescription(): String {
    val marker = " ("
    val creditSuffix = "credit applied)"
    if (description.contains(creditSuffix)) {
        return description.substringBefore(marker).trimEnd()
    }
    return description
}

fun Bill.creditAppliedMinor(): Long =
    if (isIssuedToCustomer()) {
        issuedInvoiceSnapshot?.creditLines?.sumOf { it.amountMinor }
            ?: if (lineItems.isNotEmpty()) {
                lineItems.filter { it.kind == BillLineItemKind.CREDIT }.sumOf { it.amountMinor }
            } else {
                (grossAmountMinorResolved() - amountMinor).coerceAtLeast(0)
            }
    } else if (lineItems.isNotEmpty()) {
        lineItems.filter { it.kind == BillLineItemKind.CREDIT }.sumOf { it.amountMinor }
    } else {
        (grossAmountMinorResolved() - amountMinor).coerceAtLeast(0)
    }
