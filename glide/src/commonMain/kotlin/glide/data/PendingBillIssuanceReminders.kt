package glide.data

import glide.model.Bill
import glide.model.BillStatus
import glide.model.formatMoney

data class PendingBillIssuance(
    val billId: String,
    val soldPlanId: String,
    val customerLabel: String,
    val billDescription: String,
    val formattedAmount: String,
)

fun findBillsNeedingIssuance(): List<PendingBillIssuance> =
    BillStore.all
        .asSequence()
        .filter { it.status == BillStatus.SCHEDULED }
        .mapNotNull { bill -> bill.toPendingBillIssuanceOrNull() }
        .sortedWith(compareBy({ it.customerLabel.lowercase() }, { it.billDescription.lowercase() }))
        .toList()

fun billsNeedingIssuanceMessage(count: Int? = null): String {
    val billCount = count ?: findBillsNeedingIssuance().size
    return if (billCount == 1) {
        "1 bill is scheduled and needs to be issued."
    } else {
        "$billCount bills are scheduled and need to be issued."
    }
}

fun openPendingBillIssuance(pending: PendingBillIssuance) {
    AppViewState.switchTo(AppViewMode.CUSTOMER_MANAGEMENT)
    LeadNavigation.openSoldPlan(pending.soldPlanId)
}

private fun Bill.toPendingBillIssuanceOrNull(): PendingBillIssuance? {
    val group = findSoldPlanById(soldPlanId) ?: return null
    if (soldPlanBlocksBillIssuance(soldPlanId)) return null
    val main = group.resolveMainClient()
    val customerLabel = main?.name?.ifBlank { "Customer" } ?: "Customer"
    return PendingBillIssuance(
        billId = id,
        soldPlanId = soldPlanId,
        customerLabel = customerLabel,
        billDescription = planLineDescription(),
        formattedAmount = formatMoney(amountMinor, currencyCode),
    )
}
