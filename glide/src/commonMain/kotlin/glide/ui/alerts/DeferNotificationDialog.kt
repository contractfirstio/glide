package glide.ui.alerts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import glide.data.NotificationAlertKind
import glide.ui.leads.millisToIsoDate
import glide.ui.leads.todayIsoDate
import glide.ui.theme.GlideTextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId

private fun notificationAlertDeferLabel(kind: NotificationAlertKind): String = when (kind) {
    NotificationAlertKind.PENDING_ATTENDANCE -> "Remind me about attendance on…"
    NotificationAlertKind.PENDING_BILL_ISSUANCE -> "Remind me about bills to issue on…"
    NotificationAlertKind.OVERDUE_BILL_PAYMENT -> "Remind me about overdue payments on…"
    NotificationAlertKind.ROLLING_TERM_COVERAGE -> "Remind me about rolling term coverage on…"
    NotificationAlertKind.UNASSIGNED_SOLD_PLAN -> "Remind me about unassigned sold plans on…"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeferNotificationDialog(
    alertKind: NotificationAlertKind,
    onDismiss: () -> Unit,
    onConfirm: (remindOnIsoDate: String) -> Unit,
) {
    val todayMillis = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val selectableDates = remember(todayMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMillis
        }
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayMillis,
        selectableDates = selectableDates,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            GlideTextButton(
                onClick = {
                    val isoDate = pickerState.selectedDateMillis?.let { millisToIsoDate(it) }
                        ?: todayIsoDate()
                    onConfirm(isoDate)
                },
            ) {
                Text("Remind me")
            }
        },
        dismissButton = {
            GlideTextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        Column {
            Text(
                text = notificationAlertDeferLabel(alertKind),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            DatePicker(
                state = pickerState,
                showModeToggle = true,
                colors = DatePickerDefaults.colors(),
            )
        }
    }
}
