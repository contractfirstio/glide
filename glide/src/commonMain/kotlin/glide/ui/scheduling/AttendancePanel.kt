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
import androidx.compose.material3.HorizontalDivider
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
import glide.data.ClassAttendanceStore
import glide.data.LocationStore
import glide.data.ScheduledClassStore
import glide.data.attendeesForClass
import glide.model.AttendanceStatus
import glide.model.ClassAttendee
import glide.model.ClassSessionKey
import glide.model.ScheduledClass
import glide.model.scheduleLine
import glide.ui.layout.GlideLayout
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideOutlinedButton
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
    val attendees = remember(scheduledClass, session) {
        scheduledClass?.let { attendeesForClass(it) } ?: emptyList()
    }
    val attendeeKeys = remember(attendees) { attendees.map { it.key } }

    var draftByAttendeeKey by remember(session) {
        mutableStateOf(ClassAttendanceStore.draftForSession(session, attendeeKeys))
    }
    var saveMessage by remember(session) { mutableStateOf<String?>(null) }

    LaunchedEffect(session, attendeeKeys) {
        draftByAttendeeKey = ClassAttendanceStore.draftForSession(session, attendeeKeys)
        saveMessage = null
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
            Text(
                text = scheduledClass.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = buildString {
                    append(formatIsoDateForDisplay(session.sessionDate))
                    append(" · ")
                    append(scheduledClass.scheduleLine())
                    if (!locationName.isNullOrBlank()) {
                        append(" · ")
                        append(locationName)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val present = draftByAttendeeKey.values.count { it == AttendanceStatus.PRESENT }
            val absent = draftByAttendeeKey.values.count { it == AttendanceStatus.ABSENT }
            val unmarked = (attendees.size - present - absent).coerceAtLeast(0)
            Spacer(modifier = Modifier.height(spacing.section))
            Text(
                text = "$present present · $absent absent · $unmarked unmarked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            saveMessage?.let { message ->
                Spacer(modifier = Modifier.height(spacing.field))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(spacing.section))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.section))

            if (attendees.isEmpty()) {
                Text(
                    text = "No students enrolled. Assign customer groups in the Classes panel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlideOutlinedButton(
                        onClick = {
                            draftByAttendeeKey = markAllDraft(draftByAttendeeKey, attendees, AttendanceStatus.PRESENT)
                            saveMessage = null
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("All present")
                    }
                    GlideOutlinedButton(
                        onClick = {
                            draftByAttendeeKey = markAllDraft(draftByAttendeeKey, attendees, AttendanceStatus.ABSENT)
                            saveMessage = null
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("All absent")
                    }
                }
                Spacer(modifier = Modifier.height(spacing.section))

                val grouped = attendees.groupBy { it.peopleGroupId }
                grouped.forEach { (_, groupAttendees) ->
                    val household = groupAttendees.first().householdLabel
                    Text(
                        text = household,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    groupAttendees.forEach { attendee ->
                        AttendanceRow(
                            attendee = attendee,
                            status = draftByAttendeeKey[attendee.key],
                            onPresent = {
                                draftByAttendeeKey = draftByAttendeeKey + (attendee.key to AttendanceStatus.PRESENT)
                                saveMessage = null
                            },
                            onAbsent = {
                                draftByAttendeeKey = draftByAttendeeKey + (attendee.key to AttendanceStatus.ABSENT)
                                saveMessage = null
                            },
                            onClear = {
                                draftByAttendeeKey = draftByAttendeeKey + (attendee.key to null)
                                saveMessage = null
                            },
                        )
                        Spacer(modifier = Modifier.height(spacing.field))
                    }
                    Spacer(modifier = Modifier.height(spacing.section))
                }
            }

            Spacer(modifier = Modifier.height(spacing.field))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.field),
            ) {
                GlideButton(
                    onClick = {
                        ClassAttendanceStore.saveSession(session, draftByAttendeeKey)
                        saveMessage = "Attendance saved."
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(saveLabel)
                }
            }
        }
    }
}

@Composable
private fun AttendanceRow(
    attendee: ClassAttendee,
    status: AttendanceStatus?,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clipListItemBackground()
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
            if (status != null) {
                Text(
                    text = when (status) {
                        AttendanceStatus.PRESENT -> "Present"
                        AttendanceStatus.ABSENT -> "Absent"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (status) {
                        AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primary
                        AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
                    },
                )
            }
        }
        AttendanceToggle(
            label = "Present",
            checked = status == AttendanceStatus.PRESENT,
            onCheckedChange = { checked ->
                if (checked) onPresent() else if (status == AttendanceStatus.PRESENT) onClear()
            },
        )
        AttendanceToggle(
            label = "Absent",
            checked = status == AttendanceStatus.ABSENT,
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
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

@Composable
private fun Modifier.clipListItemBackground(): Modifier =
    this.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))

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
