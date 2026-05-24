package glide.data

import glide.model.Bill
import glide.model.BillStatus
import glide.model.PAYMENT_OVERDUE_ALERT_DAYS
import glide.model.PeopleGroupType
import glide.model.daysPastPaymentDue
import glide.model.effectivePaymentDueAtMillis
import glide.model.formatMoney
import glide.model.isPaymentOverdueByAtLeastDays
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale

data class OverdueBillPayment(
    val billId: String,
    val peopleGroupId: String,
    val customerLabel: String,
    val billDescription: String,
    val formattedAmount: String,
    val daysPastDue: Int,
    val dueDateLabel: String,
)

private val dueDateFormat = SimpleDateFormat("d MMM yyyy", Locale.UK)

fun findOverdueBillPayments(
    overdueThresholdDays: Long = PAYMENT_OVERDUE_ALERT_DAYS,
    nowMillis: Long = System.currentTimeMillis(),
): List<OverdueBillPayment> =
    BillStore.all
        .asSequence()
        .filter { it.isPaymentOverdueByAtLeastDays(overdueThresholdDays, nowMillis) }
        .mapNotNull { it.toOverdueBillPaymentOrNull(nowMillis) }
        .sortedWith(
            compareByDescending<OverdueBillPayment> { it.daysPastDue }
                .thenBy { it.customerLabel.lowercase() },
        )
        .toList()

fun overdueBillPaymentsMessage(count: Int? = null): String {
    val billCount = count ?: findOverdueBillPayments().size
    return if (billCount == 1) {
        "1 issued bill is more than $PAYMENT_OVERDUE_ALERT_DAYS days overdue for payment."
    } else {
        "$billCount issued bills are more than $PAYMENT_OVERDUE_ALERT_DAYS days overdue for payment."
    }
}

fun openOverdueBillPayment(overdue: OverdueBillPayment) {
    AppViewState.switchTo(AppViewMode.CUSTOMER_MANAGEMENT)
    PeopleGroupNavigation.openCustomer(overdue.peopleGroupId)
}

/** Wake after local midnight so overdue thresholds update daily. */
fun millisUntilNextOverduePaymentCheck(): Long {
    val now = LocalDateTime.now()
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
    return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(60_000L)
}

private fun Bill.toOverdueBillPaymentOrNull(nowMillis: Long): OverdueBillPayment? {
    if (status != BillStatus.ISSUED) return null
    val daysPastDue = daysPastPaymentDue(nowMillis) ?: return null
    if (daysPastDue < PAYMENT_OVERDUE_ALERT_DAYS) return null
    val group = PeopleGroupStore.findById(peopleGroupId) ?: return null
    if (group.type != PeopleGroupType.CUSTOMER) return null
    val main = group.resolveMainClient()
    val dueMillis = effectivePaymentDueAtMillis() ?: return null
    return OverdueBillPayment(
        billId = id,
        peopleGroupId = peopleGroupId,
        customerLabel = main.name.ifBlank { "Customer" },
        billDescription = planLineDescription(),
        formattedAmount = formatMoney(amountMinor, currencyCode),
        daysPastDue = daysPastDue,
        dueDateLabel = dueDateFormat.format(Date(dueMillis)),
    )
}
