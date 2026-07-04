package glide.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.data.BillStore
import glide.data.PendingBillIssuance
import glide.data.SoldPlanStore
import glide.data.billsNeedingIssuanceMessage
import glide.data.findBillsNeedingIssuance
import glide.data.openPendingBillIssuance
import glide.data.NotificationAlertKind
import glide.ui.alerts.NotificationAlertBannerActions
import glide.ui.alerts.shouldShowNotificationAlert

@Composable
fun rememberPendingBillsToIssue(): List<PendingBillIssuance> {
    BillStore.all
    SoldPlanStore.all
    return findBillsNeedingIssuance()
}

@Composable
fun PendingBillIssuanceAlertBanner(
    pending: List<PendingBillIssuance>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (!shouldShowNotificationAlert(NotificationAlertKind.PENDING_BILL_ISSUANCE)) return
    if (pending.isEmpty()) return
    val billCount = pending.size
    val first = pending.first()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = billsNeedingIssuanceMessage(billCount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        if (!compact) {
            val preview = pending.take(3).joinToString(" · ") { item ->
                "${item.customerLabel} (${item.billDescription}, ${item.formattedAmount})"
            }
            val suffix = if (pending.size > 3) " · …" else ""
            Text(
                text = preview + suffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        NotificationAlertBannerActions(
            alertKind = NotificationAlertKind.PENDING_BILL_ISSUANCE,
            foreground = MaterialTheme.colorScheme.onTertiaryContainer,
            primaryLabel = if (compact) "Open billing" else "Open first bill",
            onPrimaryClick = { openPendingBillIssuance(first) },
            compact = compact,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
