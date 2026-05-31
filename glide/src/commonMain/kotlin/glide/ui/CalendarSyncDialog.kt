package glide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.calendar.CalendarSyncResult
import glide.calendar.GoogleCalendarSyncService
import glide.data.AppSettingsStore
import glide.ui.theme.GlideButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CalendarSyncDialog(
    onDismiss: () -> Unit,
) {
    val settings by AppSettingsStore.settingsState
    var busy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf(settings.googleCalendarSyncLastMessage) }
    var connectionLabel by remember { mutableStateOf(GoogleCalendarSyncService.connectionLabel()) }
    val scope = rememberCoroutineScope()

    fun runCalendarAction(action: () -> CalendarSyncResult) {
        scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { action() }
            connectionLabel = GoogleCalendarSyncService.connectionLabel()
            statusMessage = when (result) {
                is CalendarSyncResult.Success -> result.message
                is CalendarSyncResult.Failure -> result.message
            }
            busy = false
        }
    }

    LaunchedEffect(settings.googleCalendarSyncLastMessage) {
        if (settings.googleCalendarSyncLastMessage.isNotBlank()) {
            statusMessage = settings.googleCalendarSyncLastMessage
        }
    }

    val lastSyncLabel = remember(settings.googleCalendarSyncLastSyncMillis) {
        if (settings.googleCalendarSyncLastSyncMillis <= 0L) {
            null
        } else {
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.UK)
                .withZone(ZoneId.systemDefault())
            formatter.format(Instant.ofEpochMilli(settings.googleCalendarSyncLastSyncMillis))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Google Calendar sync") },
        text = {
            Column {
                Text(
                    text = "Glide writes class sessions into a dedicated Glide calendar in your Google account. " +
                        "Glide is the source of schedule data — edits in Google Calendar may be overwritten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = connectionLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clickable(enabled = !busy && GoogleCalendarSyncService.isConnected()) {
                            runCalendarAction {
                                GoogleCalendarSyncService.setEnabled(!settings.googleCalendarSyncEnabled)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = settings.googleCalendarSyncEnabled,
                        onCheckedChange = null,
                        enabled = !busy && GoogleCalendarSyncService.isConnected(),
                    )
                    Text(
                        text = "Keep Google Calendar updated",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (lastSyncLabel != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last sync: $lastSyncLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (statusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (!GoogleCalendarSyncService.isConnected()) {
                    Text(
                        text = "After Connect, this dialog should show Connected as your@gmail.com. " +
                            "If it still says Not connected, read the error message above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = "OAuth config: ~/Library/Application Support/Glide/google-oauth-client.properties " +
                        "needs clientId and clientSecret (both shown in Google Cloud for Desktop clients), " +
                        "or save Download JSON as google-oauth-client.json.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row {
                if (GoogleCalendarSyncService.isConnected()) {
                    GlideButton(
                        onClick = { runCalendarAction { GoogleCalendarSyncService.disconnect() } },
                        enabled = !busy,
                    ) {
                        Text("Disconnect")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    GlideButton(
                        onClick = { runCalendarAction { GoogleCalendarSyncService.connect() } },
                        enabled = !busy,
                    ) {
                        Text("Connect")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                GlideButton(
                    onClick = { runCalendarAction { GoogleCalendarSyncService.syncNow() } },
                    enabled = !busy && GoogleCalendarSyncService.isConnected(),
                ) {
                    Text("Sync now")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
