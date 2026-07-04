package glide.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import glide.data.AppSettingsStore
import glide.data.NotificationAlertKind
import glide.data.isNotificationAlertDeferred
import glide.data.millisUntilNextNotificationDeferCheck
import glide.ui.theme.GlideTextButton
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf

@Composable
fun rememberNotificationDeferRefreshTick() {
    var refreshTick by remember { mutableIntStateOf(0) }
    AppSettingsStore.settingsState.value.notificationDeferrals
    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilNextNotificationDeferCheck())
            refreshTick++
        }
    }
    refreshTick
}

@Composable
fun shouldShowNotificationAlert(kind: NotificationAlertKind): Boolean {
    rememberNotificationDeferRefreshTick()
    AppSettingsStore.settingsState.value
    return !isNotificationAlertDeferred(kind)
}

@Composable
fun NotificationAlertBannerActions(
    alertKind: NotificationAlertKind,
    foreground: Color,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    var showDeferDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideTextButton(onClick = { showDeferDialog = true }) {
            Text(
                text = if (compact) "Remind" else "Remind me on…",
                color = foreground,
            )
        }
        GlideTextButton(onClick = onPrimaryClick) {
            Text(text = primaryLabel, color = foreground)
        }
    }

    if (showDeferDialog) {
        DeferNotificationDialog(
            alertKind = alertKind,
            onDismiss = { showDeferDialog = false },
            onConfirm = { isoDate ->
                AppSettingsStore.deferNotification(alertKind, isoDate)
                showDeferDialog = false
            },
        )
    }
}
