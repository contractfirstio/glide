package glide.data

import glide.model.Bill

fun Bill.grossAmountMinorResolved(): Long =
    grossAmountMinor ?: (amountMinor + BillingCreditStore.appliedTotalMinorForBill(id))

/** Pack line label without legacy credit suffix in [Bill.description]. */
fun Bill.packLineDescription(): String {
    val marker = " ("
    val creditSuffix = "credit applied)"
    if (description.contains(creditSuffix)) {
        return description.substringBefore(marker).trimEnd()
    }
    return description
}

fun Bill.creditAppliedMinor(): Long =
    (grossAmountMinorResolved() - amountMinor).coerceAtLeast(0)
