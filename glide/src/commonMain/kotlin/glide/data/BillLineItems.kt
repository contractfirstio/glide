package glide.data

import glide.model.Bill
import glide.model.BillLineItem
import glide.model.BillLineItemKind
import glide.model.BillLineItemSource
import glide.model.BillingCredit
import glide.model.IssuedInvoiceSnapshot
import glide.model.isBillingEditable
import glide.model.isIssuedToCustomer

fun BillLineItem.isEditableBeforeIssue(): Boolean =
    source != BillLineItemSource.ATTENDANCE_CREDIT && source != BillLineItemSource.LOCKED

fun Bill.ensureLineItems(): Bill {
    if (isIssuedToCustomer() && lineItems.isNotEmpty()) return this
    if (lineItems.isNotEmpty()) return this
    val packLine = BillLineItem(
        description = packLineDescription(),
        amountMinor = grossAmountMinorResolved(),
        kind = BillLineItemKind.DEBIT,
        source = BillLineItemSource.PACK,
    )
    return copy(lineItems = listOf(packLine) + attendanceCreditLineItems())
}

/** Line items and total for UI display; locked bills read from the issue-time snapshot. */
fun Bill.displayBillingLineItems(): List<BillLineItem> {
    if (isIssuedToCustomer()) {
        issuedInvoiceSnapshot?.toDisplayLineItems()?.let { return it }
        if (lineItems.isNotEmpty()) return lineItems
    }
    return ensureLineItems().lineItems
}

fun Bill.displayBillingTotalMinor(): Long =
    if (isIssuedToCustomer()) {
        issuedInvoiceSnapshot?.totalAmountMinor ?: amountMinor
    } else {
        ensureLineItems().amountMinor
    }

private fun IssuedInvoiceSnapshot.toDisplayLineItems(): List<BillLineItem> {
    val debits = debitLines.mapIndexed { index, line ->
        BillLineItem(
            id = "issued-debit-$index",
            description = line.description,
            amountMinor = line.amountMinor,
            kind = BillLineItemKind.DEBIT,
            source = BillLineItemSource.LOCKED,
        )
    }
    val credits = creditLines.mapIndexed { index, line ->
        BillLineItem(
            id = "issued-credit-$index",
            description = line.description,
            amountMinor = line.amountMinor,
            kind = BillLineItemKind.CREDIT,
            source = BillLineItemSource.LOCKED,
        )
    }
    return debits + credits
}

private fun Bill.attendanceCreditLineItems(): List<BillLineItem> =
    BillingCreditStore.appliedToBill(id).map { it.toBillLineItem() }

fun BillingCredit.toBillLineItem(): BillLineItem = BillLineItem(
    description = description,
    amountMinor = amountMinor,
    kind = BillLineItemKind.CREDIT,
    source = BillLineItemSource.ATTENDANCE_CREDIT,
    billingCreditId = id,
)

fun List<BillLineItem>.totalDebitsMinor(): Long =
    filter { it.kind == BillLineItemKind.DEBIT }.sumOf { it.amountMinor }

fun List<BillLineItem>.totalCreditsMinor(): Long =
    filter { it.kind == BillLineItemKind.CREDIT }.sumOf { it.amountMinor }

fun List<BillLineItem>.netAmountMinor(): Long =
    (totalDebitsMinor() - totalCreditsMinor()).coerceAtLeast(0)

/** Line items excluding attendance credits — used to compute how much credit can still be applied. */
fun Bill.baseLineItems(): List<BillLineItem> =
    ensureLineItems().lineItems.filter { it.source != BillLineItemSource.ATTENDANCE_CREDIT }

fun Bill.subtotalBeforeAttendanceCreditsMinor(): Long =
    baseLineItems().netAmountMinor()

fun Bill.recomputeFromLineItems(appliedCredits: List<BillingCredit>): Bill {
    val ensured = ensureLineItems()
    val baseItems = ensured.lineItems.filter { it.source != BillLineItemSource.ATTENDANCE_CREDIT }
    val attendanceItems = appliedCredits.map { it.toBillLineItem() }
    val allItems = baseItems + attendanceItems
    val primaryDebit = baseItems.firstOrNull { it.kind == BillLineItemKind.DEBIT }
    return copy(
        lineItems = allItems,
        grossAmountMinor = baseItems.totalDebitsMinor(),
        amountMinor = allItems.netAmountMinor(),
        description = primaryDebit?.description?.takeIf { it.isNotBlank() } ?: description,
    )
}
