package glide.ui.scheduling

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
import glide.data.SoldPlanEnrollmentStore
import glide.data.SoldPlanStore
import glide.data.ClassStore
import glide.data.UnassignedSoldPlan
import glide.data.findSoldPlansNotAssignedToClass
import glide.data.openUnassignedSoldPlan
import glide.data.unassignedSoldPlansMessage
import glide.data.NotificationAlertKind
import glide.ui.alerts.NotificationAlertBannerActions
import glide.ui.alerts.shouldShowNotificationAlert

@Composable
fun rememberUnassignedSoldPlans(): List<UnassignedSoldPlan> {
    SoldPlanEnrollmentStore.all
    SoldPlanStore.all
    ClassStore.classes
    return findSoldPlansNotAssignedToClass()
}

@Composable
fun UnassignedSoldPlanAlertBanner(
    unassigned: List<UnassignedSoldPlan>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (!shouldShowNotificationAlert(NotificationAlertKind.UNASSIGNED_SOLD_PLAN)) return
    if (unassigned.isEmpty()) return
    val planCount = unassigned.size
    val first = unassigned.first()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = unassignedSoldPlansMessage(planCount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        if (!compact) {
            val preview = unassigned.take(3).joinToString(" · ") { item ->
                "${item.customerLabel} (${item.planName})"
            }
            val suffix = if (unassigned.size > 3) " · …" else ""
            Text(
                text = preview + suffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        NotificationAlertBannerActions(
            alertKind = NotificationAlertKind.UNASSIGNED_SOLD_PLAN,
            foreground = MaterialTheme.colorScheme.onTertiaryContainer,
            primaryLabel = if (compact) "Open" else "Open first sold plan",
            onPrimaryClick = { openUnassignedSoldPlan(first) },
            compact = compact,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
