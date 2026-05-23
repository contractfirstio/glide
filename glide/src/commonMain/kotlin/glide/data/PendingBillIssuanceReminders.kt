package glide.data

import glide.model.Bill
import glide.model.BillStatus
import glide.model.PeopleGroupType
import glide.model.formatMoney

data class PendingBillIssuance(
    val billId: String,
    val peopleGroupId: String,
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
    PeopleGroupNavigation.openCustomer(pending.peopleGroupId)
}

private fun Bill.toPendingBillIssuanceOrNull(): PendingBillIssuance? {
    val group = PeopleGroupStore.findById(peopleGroupId) ?: return null
    if (group.type != PeopleGroupType.CUSTOMER) return null
    val main = group.resolveMainContact()
    val customerLabel = main.name.ifBlank { "Customer" }
    return PendingBillIssuance(
        billId = id,
        peopleGroupId = peopleGroupId,
        customerLabel = customerLabel,
        billDescription = packLineDescription(),
        formattedAmount = formatMoney(amountMinor, currencyCode),
    )
}
