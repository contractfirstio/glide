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
import glide.data.ClassStore
import glide.data.RollingTermCoverageAlert
import glide.data.SoldPlanEnrollmentStore
import glide.data.SoldPlanStore
import glide.data.TermStore
import glide.data.findRollingTermCoverageAlerts
import glide.data.openSoldPlanClassAssignment
import glide.data.rollingTermCoverageWarningMessage
import glide.data.rollingTermsBlockBillIssuanceMessage
import glide.data.NotificationAlertKind
import glide.ui.alerts.NotificationAlertBannerActions
import glide.ui.alerts.shouldShowNotificationAlert

@Composable
fun rememberRollingTermCoverageAlerts(): List<RollingTermCoverageAlert> {
    SoldPlanEnrollmentStore.all
    SoldPlanStore.all
    ClassStore.classes
    TermStore.terms
    return findRollingTermCoverageAlerts()
}

@Composable
fun RollingTermCoverageAlertBanner(
    alerts: List<RollingTermCoverageAlert>,
    modifier: Modifier = Modifier,
) {
    if (!shouldShowNotificationAlert(NotificationAlertKind.ROLLING_TERM_COVERAGE)) return
    if (alerts.isEmpty()) return
    val blocking = alerts.filter { it.blocksBilling }
    val warningOnly = alerts.filterNot { it.blocksBilling }
    val first = alerts.first()

    val headline = when {
        blocking.isNotEmpty() -> {
            if (blocking.size == 1) {
                "1 rolling plan cannot be billed — add future terms."
            } else {
                "${blocking.size} rolling plans cannot be billed — add future terms."
            }
        }
        warningOnly.size == 1 -> "1 rolling plan is within 15 weeks of term exhaustion."
        else -> "${warningOnly.size} rolling plans are within 15 weeks of term exhaustion."
    }
    val detail = when {
        blocking.isNotEmpty() && warningOnly.isNotEmpty() ->
            "${rollingTermsBlockBillIssuanceMessage()} " +
                "${warningOnly.size} more plan${if (warningOnly.size == 1) "" else "s"} need new terms soon."
        blocking.isNotEmpty() -> rollingTermsBlockBillIssuanceMessage()
        else -> rollingTermCoverageWarningMessage(first.weeksRemaining)
    }

    val background = if (blocking.isNotEmpty()) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val foreground = if (blocking.isNotEmpty()) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            modifier = Modifier.padding(top = 2.dp),
        )
        NotificationAlertBannerActions(
            alertKind = NotificationAlertKind.ROLLING_TERM_COVERAGE,
            foreground = foreground,
            primaryLabel = "Open first sold plan",
            onPrimaryClick = { openSoldPlanClassAssignment(first.soldPlanId) },
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
