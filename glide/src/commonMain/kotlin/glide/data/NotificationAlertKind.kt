package glide.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId

@Serializable
enum class NotificationAlertKind {
    PENDING_ATTENDANCE,
    PENDING_BILL_ISSUANCE,
    OVERDUE_BILL_PAYMENT,
    ROLLING_TERM_COVERAGE,
    UNASSIGNED_SOLD_PLAN,
}

@Serializable
data class NotificationDeferrals(
    val remindOnByKind: Map<String, String> = emptyMap(),
)

fun isNotificationAlertDeferred(
    kind: NotificationAlertKind,
    deferrals: NotificationDeferrals = AppSettingsStore.settings.notificationDeferrals,
    today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
): Boolean {
    val iso = deferrals.remindOnByKind[kind.name]?.takeIf { it.isNotBlank() } ?: return false
    return try {
        today.isBefore(LocalDate.parse(iso))
    } catch (_: Exception) {
        false
    }
}
