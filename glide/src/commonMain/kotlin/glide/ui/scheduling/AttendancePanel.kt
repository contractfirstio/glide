package glide.ui.scheduling

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.data.AbsentCreditPreview
import glide.data.AttendanceCreditService
import glide.data.ClassAttendanceStore
import glide.data.RollingPlanBillingService
import glide.data.LocationStore
import glide.data.ScheduledClassStore
import glide.data.attendeesForClass
import glide.model.parseIsoLocalDate
import glide.model.AttendanceStatus
import glide.model.formatMoney
import glide.model.ClassAttendee
import glide.model.ClassSessionKey
import glide.model.ScheduledClass
import glide.model.canTakeAttendance
import glide.model.scheduleLine
import glide.ui.layout.GlideLayout
import glide.ui.shared.FormPanelLinkedBox
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideTextButton
import glide.ui.theme.glideListItemTitleColor

fun attendancePanelTitle(scheduledClass: ScheduledClass?, sessionDate: String): String {
    val className = scheduledClass?.name ?: "Class"
    val dateLabel = if (sessionDate.isNotBlank()) {
        formatIsoDateForDisplay(sessionDate)
    } else {
        ""
    }
    return when {
        dateLabel.isNotBlank() -> "Attendance — $className · $dateLabel"
        else -> "Attendance — $className"
    }
}

@Composable
fun AttendancePanel(
    session: ClassSessionKey,
    modifier: Modifier = Modifier,
) {
    val scheduledClass = ScheduledClassStore.findById(session.scheduledClassId)
    val sessionDate = remember(session.sessionDate) { parseIsoLocalDate(session.sessionDate) }
    val attendees = remember(scheduledClass, sessionDate) {
        if (scheduledClass == null || sessionDate == null) emptyList()
        else attendeesForClass(scheduledClass, sessionDate)
    }
    val attendeeKeys = remember(attendees) { attendees.map { it.key } }
    val canTakeAttendance = scheduledClass != null && sessionDate != null &&
        scheduledClass.canTakeAttendance(sessionDate)
    ClassAttendanceStore.records
    val isSubmitted = ClassAttendanceStore.isSessionSubmitted(session)
    val canEditAttendance = canTakeAttendance && !isSubmitted

    var draftByAttendeeKey by remember(session) {
        mutableStateOf(ClassAttendanceStore.draftForSession(session, attendeeKeys))
    }
    var saveMessage by remember(session) { mutableStateOf<String?>(null) }
    var saveMessageIsError by remember(session) { mutableStateOf(false) }
    var highlightUnmarked by remember(session) { mutableStateOf(false) }
    var showCreditDialog by remember(session) { mutableStateOf(false) }
    var creditDialogPreviews by remember(session) { mutableStateOf<List<AbsentCreditPreview>>(emptyList()) }

    LaunchedEffect(session, attendeeKeys) {
        draftByAttendeeKey = ClassAttendanceStore.draftForSession(session, attendeeKeys)
        saveMessage = null
        saveMessageIsError = false
        highlightUnmarked = false
        showCreditDialog = false
        creditDialogPreviews = emptyList()
    }

    fun finalizeSave(applyCredits: Boolean) {
        if (isSubmitted) {
            saveMessageIsError = true
            saveMessage = "Attendance was already submitted and cannot be changed."
            showCreditDialog = false
            return
        }
        if (applyCredits) {
            val absent = AttendanceCreditService.absentAttendees(attendees, draftByAttendeeKey)
            val result = AttendanceCreditService.applyCreditsForAbsentAttendees(
                session = session,
                className = scheduledClass?.name ?: "Class",
                absentAttendees = absent,
            )
            if (!ClassAttendanceStore.saveSession(session, draftByAttendeeKey, attendeeKeys)) {
                saveMessageIsError = true
                saveMessage = "Could not save attendance."
                showCreditDialog = false
                return
            }
            highlightUnmarked = false
            saveMessageIsError = false
            saveMessage = when {
                result.creditsAdded > 0 -> {
                    val creditLabel = formatMoney(result.totalCreditMinor, result.currencyCode)
                    "Attendance submitted. $creditLabel credited toward next plan bill."
                }
                result.creditsSkipped > 0 ->
                    "Attendance submitted. No billing credits were added (enrollment missing or already credited)."
                else -> "Attendance submitted."
            }
        } else {
            if (!ClassAttendanceStore.saveSession(session, draftByAttendeeKey, attendeeKeys)) {
                saveMessageIsError = true
                saveMessage = "Could not save attendance."
                showCreditDialog = false
                return
            }
            highlightUnmarked = false
            saveMessageIsError = false
            saveMessage = "Attendance submitted."
        }
        attendees.map { it.peopleGroupId }.distinct().forEach { groupId ->
            RollingPlanBillingService.syncRollingPlanBilling(groupId)
        }
        showCreditDialog = false
    }

    fun attemptSave() {
        if (isSubmitted) {
            saveMessageIsError = true
            saveMessage = "Attendance was already submitted and cannot be changed."
            return
        }
        val unmarked = ClassAttendanceStore.unmarkedAttendeeKeys(draftByAttendeeKey, attendeeKeys)
        if (unmarked.isNotEmpty()) {
            highlightUnmarked = true
            saveMessageIsError = true
            saveMessage = if (unmarked.size == 1) {
                "Mark the remaining student as present or absent before saving."
            } else {
                "Mark all ${unmarked.size} students as present or absent before saving."
            }
            return
        }
        val absent = AttendanceCreditService.absentAttendees(attendees, draftByAttendeeKey)
        if (absent.isEmpty()) {
            finalizeSave(applyCredits = false)
            return
        }
        creditDialogPreviews = AttendanceCreditService.previewCredits(session, absent)
        showCreditDialog = true
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < GlideLayout.CompactWidthBreakpoint
        val spacing = if (compact) GlideLayout.compact else GlideLayout.comfortable
        val saveLabel = if (compact) "Save" else "Save attendance"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.outer),
        ) {
            if (scheduledClass == null) {
                Text(
                    text = "Class not found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            val locationName = scheduledClass.locationId?.let { LocationStore.findById(it)?.name }
            val present = draftByAttendeeKey.values.count { it == AttendanceStatus.PRESENT }
            val absent = draftByAttendeeKey.values.count { it == AttendanceStatus.ABSENT }
            val unmarked = (attendees.size - present - absent).coerceAtLeast(0)

            FormPanelSection(
                title = scheduledClass.name,
                description = buildString {
                    append(formatIsoDateForDisplay(session.sessionDate))
                    append(" · ")
                    append(scheduledClass.scheduleLine())
                    if (!locationName.isNullOrBlank()) {
                        append(" · ")
                        append(locationName)
                    }
                },
                spacing = spacing,
                role = FormPanelSectionRole.Primary,
            ) {
                Text(
                    text = "$present present · $absent absent · $unmarked unmarked",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!canTakeAttendance) {
                    Spacer(modifier = Modifier.height(spacing.field))
                    Text(
                        text = "Attendance opens after this class ends (${scheduledClass.endTime}).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (isSubmitted) {
                    Spacer(modifier = Modifier.height(spacing.field))
                    Text(
                        text = "Attendance submitted — cannot be changed.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                saveMessage?.let { message ->
                    Spacer(modifier = Modifier.height(spacing.field))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (saveMessageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }

            FormPanelSectionsDivider(label = "Mark attendance", spacing = spacing)

            if (attendees.isEmpty()) {
                Text(
                    text = "No students enrolled. Assign customer groups in the Classes panel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    FormPanelSection(
                        title = "Students",
                        description = "Mark each attendee present or absent, grouped by household.",
                        spacing = spacing,
                        role = FormPanelSectionRole.Secondary,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GlideOutlinedButton(
                                onClick = {
                                    draftByAttendeeKey = markAllDraft(
                                        draftByAttendeeKey,
                                        attendees,
                                        AttendanceStatus.PRESENT,
                                    )
                                    saveMessage = null
                                    saveMessageIsError = false
                                    highlightUnmarked = false
                                },
                                modifier = Modifier.weight(1f),
                                enabled = canEditAttendance,
                            ) {
                                Text("All present")
                            }
                            GlideOutlinedButton(
                                onClick = {
                                    draftByAttendeeKey = markAllDraft(
                                        draftByAttendeeKey,
                                        attendees,
                                        AttendanceStatus.ABSENT,
                                    )
                                    saveMessage = null
                                    saveMessageIsError = false
                                    highlightUnmarked = false
                                },
                                modifier = Modifier.weight(1f),
                                enabled = canEditAttendance,
                            ) {
                                Text("All absent")
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.section))

                        val grouped = attendees.groupBy { it.peopleGroupId }
                        grouped.forEach { (_, groupAttendees) ->
                            val household = groupAttendees.first().householdLabel
                            FormPanelLinkedBox(role = FormPanelSectionRole.Secondary) {
                                Text(
                                    text = household,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = spacing.field),
                                )
                                groupAttendees.forEachIndexed { index, attendee ->
                                    AttendanceRow(
                                        attendee = attendee,
                                        status = draftByAttendeeKey[attendee.key],
                                        needsMark = highlightUnmarked && draftByAttendeeKey[attendee.key] == null,
                                        enabled = canEditAttendance,
                                        onPresent = {
                                            draftByAttendeeKey = draftByAttendeeKey +
                                                (attendee.key to AttendanceStatus.PRESENT)
                                            saveMessage = null
                                            saveMessageIsError = false
                                        },
                                        onAbsent = {
                                            draftByAttendeeKey = draftByAttendeeKey +
                                                (attendee.key to AttendanceStatus.ABSENT)
                                            saveMessage = null
                                            saveMessageIsError = false
                                        },
                                        onClear = {
                                            draftByAttendeeKey = draftByAttendeeKey + (attendee.key to null)
                                            saveMessage = null
                                            saveMessageIsError = false
                                        },
                                    )
                                    if (index < groupAttendees.lastIndex) {
                                        Spacer(modifier = Modifier.height(spacing.field))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(spacing.section))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.field))

            if (canEditAttendance) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.field),
                ) {
                    GlideButton(
                        onClick = { attemptSave() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(saveLabel)
                    }
                }
            }
        }
    }

    if (showCreditDialog && scheduledClass != null) {
        val eligibleCount = creditDialogPreviews.count { it.eligible && !it.alreadyCredited }
        AlertDialog(
            onDismissRequest = { showCreditDialog = false },
            title = { Text("Credit absent students?") },
            text = {
                Text(
                    AttendanceCreditService.creditDialogMessage(
                        creditDialogPreviews,
                        scheduledClass.name,
                    ),
                )
            },
            confirmButton = {
                GlideTextButton(
                    onClick = { finalizeSave(applyCredits = true) },
                    enabled = eligibleCount > 0,
                ) {
                    Text(if (eligibleCount > 0) "Credit & save" else "Credit unavailable")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlideTextButton(onClick = { showCreditDialog = false }) {
                        Text("Cancel")
                    }
                    GlideTextButton(onClick = { finalizeSave(applyCredits = false) }) {
                        Text("Save without credit")
                    }
                }
            },
        )
    }
}

@Composable
private fun AttendanceRow(
    attendee: ClassAttendee,
    status: AttendanceStatus?,
    needsMark: Boolean,
    enabled: Boolean,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
    onClear: () -> Unit,
) {
    val rowBackground = if (needsMark) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground, MaterialTheme.shapes.small)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attendee.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = glideListItemTitleColor(selected = false),
            )
            Text(
                text = when (status) {
                    AttendanceStatus.PRESENT -> "Present"
                    AttendanceStatus.ABSENT -> "Absent"
                    null -> if (needsMark) "Not marked" else "Unmarked"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (status) {
                    AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primary
                    AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
                    null -> if (needsMark) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                },
            )
        }
        AttendanceToggle(
            label = "Present",
            checked = status == AttendanceStatus.PRESENT,
            enabled = enabled,
            onCheckedChange = { checked ->
                if (checked) onPresent() else if (status == AttendanceStatus.PRESENT) onClear()
            },
        )
        AttendanceToggle(
            label = "Absent",
            checked = status == AttendanceStatus.ABSENT,
            enabled = enabled,
            onCheckedChange = { checked ->
                if (checked) onAbsent() else if (status == AttendanceStatus.ABSENT) onClear()
            },
        )
    }
}

@Composable
private fun AttendanceToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun markAllDraft(
    draft: Map<String, AttendanceStatus?>,
    attendees: List<ClassAttendee>,
    status: AttendanceStatus,
): Map<String, AttendanceStatus?> {
    var updated = draft
    attendees.forEach { attendee ->
        updated = updated + (attendee.key to status)
    }
    return updated
}
