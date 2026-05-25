package glide.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import glide.data.BillingPanelState
import glide.data.ClientStore
import glide.data.ClientsPanelState
import glide.data.SoldPlanStore
import glide.data.PlanStore
import glide.data.PlansPanelState
import glide.data.StudentsPanelState
import glide.data.StudentStore
import glide.data.resolveMainClient
import glide.data.findSoldPlanById
import glide.model.Student
import glide.ui.layout.GlideLayout
import glide.ui.shared.DeleteConfirmDialog
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.ListFormPanelLayout
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.rememberFormDirtyTracker
import glide.ui.leads.DateOfBirthField
import glide.ui.shared.formatPersonLabel
import glide.ui.theme.GlideButton
import glide.ui.theme.glideListItemTitleColor
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton
import java.util.UUID

private data class StudentFormState(
    val name: String = "",
    val dateOfBirth: String = "",
    val notes: String = "",
) {
    fun isValid(): Boolean = name.isNotBlank()

    fun toStudent(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): Student =
        Student(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name.trim(),
            dateOfBirth = dateOfBirth.trim(),
            notes = notes.trim(),
            createdAtMillis = createdAtMillis,
        )
}

@Composable
fun StudentsPanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val form = rememberFormDirtyTracker(StudentFormState())
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val soldPlanId = BillingPanelState.soldPlanId
    val clientFilterId = ClientsPanelState.selectedClientId
    val planFilterId = PlansPanelState.selectedPlanId
    val studentFilterId = StudentsPanelState.selectedStudentId
    val people = StudentStore.forStudentsPanel(
        soldPlanId,
        clientFilterId,
        planFilterId,
        studentFilterId,
    )
    val soldPlanLabel = soldPlanId?.let { id ->
        findSoldPlanById(id)?.resolveMainClient()?.name?.takeIf { it.isNotBlank() }
    }
    val clientFilterLabel = clientFilterId?.let { ClientStore.findById(it)?.name?.takeIf { it.isNotBlank() } }
    val planFilterLabel = planFilterId?.let { PlanStore.findById(it)?.name?.takeIf { it.isNotBlank() } }

    fun clearLocalSelection() {
        selectedId = null
        form.load(StudentFormState())
        formError = null
    }

    fun clearSelection() {
        clearLocalSelection()
        StudentsPanelState.clearStudentFilter()
    }

    LaunchedEffect(people, selectedId) {
        if (selectedId != null && people.none { it.id == selectedId }) {
            clearSelection()
        }
    }

    fun loadIntoForm(person: Student) {
        StudentsPanelState.onStudentSelected(person.id)
        selectedId = person.id
        form.load(
            StudentFormState(
                name = person.name,
                dateOfBirth = person.dateOfBirth,
                notes = person.notes,
            ),
        )
        formError = null
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < GlideLayout.CompactWidthBreakpoint
        val spacing = if (compact) GlideLayout.compact else GlideLayout.comfortable
        val listSectionHeight = if (compact) 112.dp else null
        val saveLabel = if (compact) "Save" else "Save changes"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.outer),
        ) {
            if (!compact) {
                Text(
                    text = when {
                        soldPlanId != null -> {
                            val label = soldPlanLabel ?: "this sold plan"
                            "Showing students for $label. Use Show all to reset."
                        }
                        clientFilterId != null -> {
                            val label = clientFilterLabel ?: "this client"
                            "Showing students for $label. Use Clear filter in Clients to reset."
                        }
                        planFilterId != null -> {
                            val label = planFilterLabel ?: "this plan"
                            "Showing students on groups with $label. Use Clear filter in Plans to reset."
                        }
                        studentFilterId != null -> {
                            val label = StudentStore.findById(studentFilterId)?.name?.takeIf { it.isNotBlank() }
                                ?: "this student"
                            "Filtering clients and sold plans for $label. Use Clear filter to reset."
                        }
                        else ->
                            "Edit students on customer plans. They appear here after a lead becomes a customer."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(spacing.section))
            }

            val listSection: @Composable (Modifier) -> Unit = { listModifier ->
                Column(modifier = listModifier) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${people.size} ${if (people.size == 1) "student" else "students"}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.field)) {
                            if (planFilterId != null) {
                                GlideTextButton(onClick = { PlansPanelState.clearPlanFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (studentFilterId != null) {
                                GlideTextButton(onClick = { StudentsPanelState.clearStudentFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (clientFilterId != null) {
                                GlideTextButton(onClick = { ClientsPanelState.clearClientFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (soldPlanId != null) {
                                GlideTextButton(onClick = { BillingPanelState.onSoldPlanCleared() }) {
                                    Text("Show all")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.field))

                    if (people.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (listSectionHeight != null) {
                                        Modifier.height(listSectionHeight)
                                    } else {
                                        Modifier.weight(1f)
                                    },
                                )
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    MaterialTheme.shapes.small,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = when {
                                    soldPlanId != null ->
                                        "No students in this sold plan."
                                    clientFilterId != null ->
                                        "No students linked to this client."
                                    planFilterId != null ->
                                        "No students on groups with this plan."
                                    studentFilterId != null ->
                                        "Student not found."
                                    else ->
                                        "No one on a customer plan yet. Add students on a lead, then make the lead a customer."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.then(
                                if (listSectionHeight != null) {
                                    Modifier.height(listSectionHeight)
                                } else {
                                    Modifier.weight(1f)
                                },
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(people, key = { it.id }) { person ->
                                StudentListItem(
                                    person = person,
                                    selected = person.id == selectedId,
                                    compact = compact,
                                    onClick = { loadIntoForm(person) },
                                )
                            }
                        }
                    }
                }
            }

            val formSection: @Composable (Modifier) -> Unit = { formModifier ->
                Column(modifier = formModifier.fillMaxHeight()) {
                    Text(
                        text = "Edit student",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(spacing.section))

                    if (selectedId == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    MaterialTheme.shapes.small,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Select someone on a customer plan to edit. Add them on a lead and convert to customer first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(spacing.outer),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            StudentForm(
                                state = form.draft,
                                onStateChange = { form.draft = it },
                                spacing = spacing,
                            )

                            val soldPlanCount = selectedId?.let { StudentStore.soldPlanCount(it) } ?: 0
                            if (soldPlanCount > 0) {
                                Spacer(modifier = Modifier.height(spacing.field))
                                Text(
                                    text = "On $soldPlanCount sold plan${if (soldPlanCount == 1) "" else "s"}.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            formError?.let { error ->
                                Spacer(modifier = Modifier.height(spacing.field))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.field))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.field),
                        ) {
                            GlideButton(
                                onClick = {
                                    if (!form.draft.isValid()) {
                                        formError = "Name is required."
                                        return@GlideButton
                                    }
                                    formError = null
                                    val existing = selectedId?.let { StudentStore.findById(it) }
                                    if (existing != null) {
                                        val updated = form.draft.toStudent(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                        )
                                        StudentStore.update(updated)
                                        loadIntoForm(updated)
                                    }
                                },
                                enabled = form.isDirty,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(saveLabel)
                            }

                            GlideOutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                enabled = selectedId?.let { StudentStore.canDelete(it) } == true,
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            ListFormPanelLayout(
                hasSelection = selectedId != null,
                spacing = spacing,
                onCloseForm = { clearSelection() },
                modifier = Modifier.fillMaxSize(),
                listSection = listSection,
                formSection = formSection,
            )
        }
    }

    if (showDeleteConfirm && selectedId != null) {
        val onSoldPlan = StudentStore.isOnSoldPlan(selectedId!!)
        DeleteConfirmDialog(
            title = "Delete student?",
            message = if (onSoldPlan) {
                "This person is on one or more sold plans and cannot be deleted."
            } else {
                "They will be removed from all leads."
            },
            onDismiss = { showDeleteConfirm = false },
            onContinue = {
                if (!onSoldPlan) {
                    StudentStore.delete(selectedId!!)
                    showDeleteConfirm = false
                    clearSelection()
                } else {
                    showDeleteConfirm = false
                    formError = "This person is on a sold plan and cannot be deleted."
                }
            },
            continueEnabled = !onSoldPlan,
        )
    }
}

@Composable
private fun StudentListItem(
    person: Student,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val spacing = if (compact) GlideLayout.compact else GlideLayout.comfortable
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val soldPlanCount = StudentStore.soldPlanCount(person.id)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(
                horizontal = spacing.listItemHorizontal,
                vertical = spacing.listItemVertical,
            ),
    ) {
        Text(
            text = formatPersonLabel(person.name, person.dateOfBirth),
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = glideListItemTitleColor(selected),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (soldPlanCount > 0) {
            Text(
                text = "$soldPlanCount sold plan${if (soldPlanCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StudentForm(
    state: StudentFormState,
    onStateChange: (StudentFormState) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    FormPanelSection(
        title = "Identity",
        description = "Name and date of birth for this student.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        GlideOutlinedField(
            value = state.name,
            onValueChange = { onStateChange(state.copy(name = it)) },
            label = "Name",
        )
        Spacer(modifier = Modifier.height(spacing.field))
        DateOfBirthField(
            value = state.dateOfBirth,
            onValueChange = { onStateChange(state.copy(dateOfBirth = it)) },
        )
    }

    FormPanelSectionsDivider(label = "Notes", spacing = spacing)

    FormPanelSection(
        title = "Notes",
        description = "Internal notes about this person.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        GlideOutlinedField(
            value = state.notes,
            onValueChange = { onStateChange(state.copy(notes = it)) },
            label = "Notes",
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            fieldHeight = GlideDimensions.notesMinHeight,
        )
    }
}
