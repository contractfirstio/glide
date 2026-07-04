package glide.ui.scheduling

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.data.DayCapacity
import glide.model.DayOfWeek
import glide.ui.theme.GlideTextButton

@Composable
fun WeeklyPlanScheduleDialog(
    planName: String,
    requiredDays: Int,
    availableDays: List<DayOfWeek>,
    dayCapacities: List<DayCapacity> = emptyList(),
    addingHeadcount: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (Set<DayOfWeek>) -> Unit,
) {
    val sortedDays = remember(availableDays) { availableDays.sortedBy { it.sortOrder } }
    val capacityByDay = remember(dayCapacities) { dayCapacities.associateBy { it.day } }
    var selectedDays by remember(requiredDays, sortedDays) {
        mutableStateOf(
            if (requiredDays >= sortedDays.size) {
                sortedDays.toSet()
            } else {
                emptySet()
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule sold plan") },
        text = {
            Column {
                Text(
                    text = buildString {
                        append(planName)
                        append(" requires ")
                        append(requiredDays)
                        append(if (requiredDays == 1) " day" else " days")
                        append(" on this weekly class. Select which days the plan should fall on.")
                        if (addingHeadcount > 0) {
                            append(" This plan has ")
                            append(addingHeadcount)
                            append(if (addingHeadcount == 1) " attendee" else " attendees")
                            append(".")
                        }
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                sortedDays.forEach { day ->
                    val dayCapacity = capacityByDay[day]
                    val dayFull = dayCapacity?.wouldExceed(addingHeadcount) == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !dayFull) {
                                selectedDays = if (day in selectedDays) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = day in selectedDays,
                            enabled = !dayFull,
                            onCheckedChange = { checked ->
                                if (dayFull) return@Checkbox
                                selectedDays = if (checked) {
                                    selectedDays + day
                                } else {
                                    selectedDays - day
                                }
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = day.label,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            dayCapacity?.let { capacity ->
                                WeeklyDayCapacityRow(
                                    dayCapacity = capacity,
                                    addingHeadcount = if (day in selectedDays) addingHeadcount else 0,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${selectedDays.size} of $requiredDays selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            GlideTextButton(
                onClick = { onConfirm(selectedDays) },
                enabled = selectedDays.size == requiredDays,
            ) {
                Text("Link plan")
            }
        },
        dismissButton = {
            GlideTextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun WeeklyPlanScheduleBlockedDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cannot link plan") },
        text = { Text(message) },
        confirmButton = {
            GlideTextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}
