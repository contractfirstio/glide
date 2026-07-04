package glide.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.data.BillStore
import glide.data.OverdueBillPayment
import glide.data.SoldPlanStore
import glide.data.findOverdueBillPayments
import glide.data.millisUntilNextOverduePaymentCheck
import glide.data.openOverdueBillPayment
import glide.data.overdueBillPaymentsMessage
import glide.data.NotificationAlertKind
import glide.ui.alerts.NotificationAlertBannerActions
import glide.ui.alerts.shouldShowNotificationAlert
import kotlinx.coroutines.delay

@Composable
fun rememberOverdueBillPayments(): List<OverdueBillPayment> {
    var refreshTick by remember { mutableIntStateOf(0) }
    BillStore.all
    SoldPlanStore.all
    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilNextOverduePaymentCheck())
            refreshTick++
        }
    }
    refreshTick
    return findOverdueBillPayments()
}

@Composable
fun OverdueBillPaymentAlertBanner(
    overdue: List<OverdueBillPayment>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (!shouldShowNotificationAlert(NotificationAlertKind.OVERDUE_BILL_PAYMENT)) return
    if (overdue.isEmpty()) return
    val billCount = overdue.size
    val first = overdue.first()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = overdueBillPaymentsMessage(billCount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        if (!compact) {
            val preview = overdue.take(3).joinToString(" · ") { item ->
                "${item.customerLabel} (due ${item.dueDateLabel}, ${item.daysPastDue}d late, ${item.formattedAmount})"
            }
            val suffix = if (overdue.size > 3) " · …" else ""
            Text(
                text = preview + suffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        NotificationAlertBannerActions(
            alertKind = NotificationAlertKind.OVERDUE_BILL_PAYMENT,
            foreground = MaterialTheme.colorScheme.onErrorContainer,
            primaryLabel = if (compact) "Open billing" else "Open first overdue bill",
            onPrimaryClick = { openOverdueBillPayment(first) },
            compact = compact,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
