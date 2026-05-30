package glide.ui.scheduling

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.data.ClassStore
import glide.data.SoldPlanTransferPreview
import glide.data.TransferSoldPlanResult
import glide.data.buildSoldPlanTransferPreview
import glide.data.findOutstandingAttendanceForSoldPlanTransfer
import glide.data.openPendingAttendanceSession
import glide.data.toUserMessage
import glide.data.transferSoldPlanToClass
import glide.model.Class
import glide.model.DayOfWeek
import glide.model.compareClasses
import glide.model.isWeekly
import glide.model.parseIsoLocalDate
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.shared.IsoDateField
import glide.ui.theme.GlideTextButton
import java.time.LocalDate

@Composable
fun SoldPlanClassTransferDialog(
    soldPlanId: String,
    onDismiss: () -> Unit,
    onTransferred: (String) -> Unit,
) {
    val fromClass = ClassStore.findClassContainingSoldPlan(soldPlanId)
    if (fromClass == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Move to another class") },
            text = { Text("This sold plan is not on a class.") },
            confirmButton = {
                GlideTextButton(onClick = onDismiss) { Text("OK") }
            },
        )
        return
    }

    val outstanding = remember(soldPlanId, fromClass.id) {
        findOutstandingAttendanceForSoldPlanTransfer(soldPlanId, fromClass)
    }
    if (outstanding.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Attendance required") },
            text = {
                Column {
                    Text(
                        "Submit attendance for all past sessions on \"${fromClass.name}\" before moving this sold plan.",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    outstanding.take(5).forEach { item ->
                        Text(
                            text = "· ${item.className} — ${formatIsoDateForDisplay(item.session.sessionDate)} " +
                                "(${item.unmarkedCount} unmarked)",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .clickable { openPendingAttendanceSession(item) }
                                .padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (outstanding.size > 5) {
                        Text(
                            text = "…and ${outstanding.size - 5} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                GlideTextButton(onClick = onDismiss) { Text("OK") }
            },
        )
        return
    }

    val targetClasses = remember(fromClass.id) {
        ClassStore.classes
            .filter { it.id != fromClass.id }
            .sortedWith(compareClasses())
    }

    var selectedTargetId by remember { mutableStateOf<String?>(null) }
    val selectedTarget = selectedTargetId?.let { ClassStore.findById(it) }

    var effectiveDateIso by remember(selectedTargetId) {
        mutableStateOf(
            selectedTarget?.let { buildSoldPlanTransferPreview(soldPlanId, it)?.defaultEffectiveDate?.toString() }
                ?: LocalDate.now().toString(),
        )
    }

    var weeklySelectedDays by remember(selectedTargetId) { mutableStateOf<Set<DayOfWeek>>(emptySet()) }

    val preview = selectedTarget?.let { buildSoldPlanTransferPreview(soldPlanId, it) }

    var transferResult by remember { mutableStateOf<TransferSoldPlanResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 480.dp),
        title = { Text("Move to another class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "From: ${fromClass.name}",
                    style = MaterialTheme.typography.bodySmall,
                )
                preview?.let { showPreview(it) }

                Text(
                    text = "To:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
                if (targetClasses.isEmpty()) {
                    Text(
                        text = "No other classes available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    targetClasses.forEach { cls ->
                        TargetClassRow(
                            cls = cls,
                            selected = cls.id == selectedTargetId,
                            onSelect = {
                                selectedTargetId = cls.id
                                val built = buildSoldPlanTransferPreview(soldPlanId, cls)
                                effectiveDateIso = built?.defaultEffectiveDate?.toString()
                                    ?: LocalDate.now().toString()
                                weeklySelectedDays = emptySet()
                            },
                        )
                    }
                }

                if (selectedTarget != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    IsoDateField(
                        label = "First session on new class",
                        value = effectiveDateIso,
                        onValueChange = { effectiveDateIso = it },
                    )
                }

                if (selectedTarget?.isWeekly() == true && preview?.remainingSessions != null) {
                    val required = preview.remainingSessions
                    val sortedDays = selectedTarget.weeklyDays.sortedBy { it.sortOrder }
                    Text(
                        text = "Select $required ${if (required == 1) "day" else "days"} for remaining sessions:",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    sortedDays.forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    weeklySelectedDays = if (day in weeklySelectedDays) {
                                        weeklySelectedDays - day
                                    } else {
                                        weeklySelectedDays + day
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = day in weeklySelectedDays,
                                onCheckedChange = { checked ->
                                    weeklySelectedDays = if (checked) {
                                        weeklySelectedDays + day
                                    } else {
                                        weeklySelectedDays - day
                                    }
                                },
                            )
                            Text(text = day.label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        text = "${weeklySelectedDays.size} of $required selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                preview?.billWarning?.let { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                transferResult?.let { result ->
                    Text(
                        text = result.toUserMessage(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            val effectiveDate = parseIsoLocalDate(effectiveDateIso)
            val canConfirm = selectedTargetId != null &&
                effectiveDate != null &&
                (
                    selectedTarget?.isWeekly() != true ||
                        preview?.remainingSessions == null ||
                        weeklySelectedDays.size == preview.remainingSessions
                    )
            GlideTextButton(
                onClick = {
                    val targetId = selectedTargetId ?: return@GlideTextButton
                    val date = effectiveDate ?: return@GlideTextButton
                    val result = transferSoldPlanToClass(
                        soldPlanId = soldPlanId,
                        targetClassId = targetId,
                        effectiveDate = date,
                        weeklySelectedDays = weeklySelectedDays.takeIf { it.isNotEmpty() },
                    )
                    when (result) {
                        is TransferSoldPlanResult.Success -> {
                            onTransferred(result.message)
                            onDismiss()
                        }
                        else -> transferResult = result
                    }
                },
                enabled = canConfirm,
            ) {
                Text("Move")
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
private fun showPreview(preview: SoldPlanTransferPreview) {
    val usageText = when (val remaining = preview.remainingSessions) {
        null -> "Rolling plan — schedule continues on the new class."
        else ->
            "${preview.usedSessions} of ${preview.planSessions} sessions used · " +
                "$remaining remaining to schedule"
    }
    Text(
        text = usageText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TargetClassRow(
    cls: Class,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = cls.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}
