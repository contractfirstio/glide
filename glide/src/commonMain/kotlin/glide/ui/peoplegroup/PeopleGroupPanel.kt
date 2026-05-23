package glide.ui.peoplegroup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import glide.data.ContactStore
import glide.data.PeopleGroupNavigation
import glide.data.PeopleGroupStore
import glide.data.PlanStore
import glide.data.resolveMainContact
import glide.data.resolveRelatedPeople
import glide.model.PeopleGroup
import glide.model.PeopleGroupStatus
import glide.model.PeopleGroupType
import glide.model.summaryLine
import glide.ui.shared.formatPersonLabel
import glide.ui.layout.GlideLayout
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PeopleGroupPanelUi(
    val type: PeopleGroupType,
    val subtitle: String,
    val emptyListMessage: String,
    val newButtonLabel: String,
    val createFormTitle: String,
    val editFormTitle: String,
    val listCountLabel: (Int) -> String,
    val showPipelineStatus: Boolean,
    val showConvertToCustomer: Boolean,
    val showPlanPicker: Boolean,
    val allowCreate: Boolean,
    val readOnly: Boolean,
    val noSelectionMessage: String,
    val deleteConfirmTitle: String,
    val deleteConfirmMessage: String,
)

private val LeadsPanelUi = PeopleGroupPanelUi(
    type = PeopleGroupType.LEAD,
    subtitle = "Manage leads, link existing contacts, and search to add related people.",
    emptyListMessage = "No leads yet.",
    newButtonLabel = "New lead",
    createFormTitle = "Create lead",
    editFormTitle = "Edit lead",
    listCountLabel = { count -> "$count lead${if (count == 1) "" else "s"}" },
    showPipelineStatus = true,
    showConvertToCustomer = true,
    showPlanPicker = true,
    allowCreate = true,
    readOnly = false,
    noSelectionMessage = "Select a lead to view or edit, or create a new one.",
    deleteConfirmTitle = "Delete lead?",
    deleteConfirmMessage = "This lead will be removed permanently.",
)

private val CustomersPanelUi = PeopleGroupPanelUi(
    type = PeopleGroupType.CUSTOMER,
    subtitle = "Customer groups are locked after creation. Clone one to a new lead to build another pack.",
    emptyListMessage = "No customers yet. Use Make customer on a lead.",
    newButtonLabel = "",
    createFormTitle = "Customer group",
    editFormTitle = "Customer group",
    listCountLabel = { count -> "$count customer group${if (count == 1) "" else "s"}" },
    showPipelineStatus = false,
    showConvertToCustomer = false,
    showPlanPicker = false,
    allowCreate = false,
    readOnly = true,
    noSelectionMessage = "Select a customer group to view.",
    deleteConfirmTitle = "Delete customer group?",
    deleteConfirmMessage = "Customer groups cannot be deleted.",
)

@Composable
fun LeadsPeopleGroupPanel(modifier: Modifier = Modifier) {
    PeopleGroupPanel(ui = LeadsPanelUi, modifier = modifier)
}

@Composable
fun CustomersPeopleGroupPanel(modifier: Modifier = Modifier) {
    PeopleGroupPanel(ui = CustomersPanelUi, modifier = modifier)
}

private data class PeopleGroupFormState(
    val mainContactId: String? = null,
    val contactName: String = "",
    val dateOfBirth: String = "",
    val email: String = "",
    val phone: String = "",
    val relatedPersonIds: List<String> = emptyList(),
    val status: PeopleGroupStatus = PeopleGroupStatus.New,
    val planId: String? = null,
    val notes: String = "",
) {
    fun isValidForLead(): Boolean =
        (mainContactId != null && ContactStore.findById(mainContactId) != null) ||
            contactName.isNotBlank()

    fun hasPlanSelected(): Boolean = !planId.isNullOrBlank()

    fun toPeopleGroup(
        type: PeopleGroupType,
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): PeopleGroup {
        val isCustomer = type == PeopleGroupType.CUSTOMER
        val linkedContact = !isCustomer && mainContactId != null
        return PeopleGroup(
            id = existingId ?: UUID.randomUUID().toString(),
            type = type,
            mainContactId = if (isCustomer || linkedContact) mainContactId else null,
            contactName = if (isCustomer || linkedContact) "" else contactName.trim(),
            dateOfBirth = if (isCustomer || linkedContact) "" else dateOfBirth.trim(),
            email = if (isCustomer || linkedContact) "" else email.trim(),
            phone = if (isCustomer || linkedContact) "" else phone.trim(),
            relatedPersonIds = relatedPersonIds,
            status = status,
            planId = planId,
            notes = notes.trim(),
            createdAtMillis = createdAtMillis,
        )
    }
}

@Composable
private fun PeopleGroupPanel(
    ui: PeopleGroupPanelUi,
    modifier: Modifier = Modifier,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var formState by remember { mutableStateOf(PeopleGroupFormState()) }
    var isCreating by remember(ui.allowCreate) { mutableStateOf(ui.allowCreate) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var cloneMessage by remember { mutableStateOf<String?>(null) }

    val groups = when (ui.type) {
        PeopleGroupType.LEAD -> PeopleGroupStore.leads
        PeopleGroupType.CUSTOMER -> PeopleGroupStore.customers
    }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    fun resetFormForCreate() {
        selectedId = null
        isCreating = ui.allowCreate
        formState = PeopleGroupFormState()
        formError = null
    }

    fun clearSelection() {
        selectedId = null
        isCreating = false
        formState = PeopleGroupFormState()
        formError = null
    }

    fun loadIntoForm(group: PeopleGroup) {
        selectedId = group.id
        isCreating = false
        val main = group.resolveMainContact()
        formState = PeopleGroupFormState(
            mainContactId = group.mainContactId,
            contactName = if (group.mainContactId != null) "" else main.name,
            dateOfBirth = if (group.mainContactId != null) "" else main.dateOfBirth,
            email = if (group.mainContactId != null) "" else main.email,
            phone = if (group.mainContactId != null) "" else main.phone,
            relatedPersonIds = group.relatedPersonIds,
            status = group.status,
            planId = group.planId,
            notes = group.notes,
        )
        formError = null
    }

    val pendingLeadId = PeopleGroupNavigation.pendingLeadId
    LaunchedEffect(pendingLeadId, ui.type) {
        if (ui.type == PeopleGroupType.LEAD && pendingLeadId != null) {
            PeopleGroupStore.findById(pendingLeadId)?.let { loadIntoForm(it) }
            PeopleGroupNavigation.clearPendingLead()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < GlideLayout.CompactWidthBreakpoint
        val spacing = if (compact) GlideLayout.compact else GlideLayout.comfortable
        val listSectionHeight = if (compact) 112.dp else null
        val notesHeight = GlideDimensions.notesMinHeight
        val saveLabel = if (compact) {
            if (isCreating) "Create" else "Save"
        } else {
            if (isCreating) "Create" else "Save changes"
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.outer),
        ) {
            if (!compact) {
                Text(
                    text = ui.subtitle,
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
                            text = ui.listCountLabel(groups.size),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if (ui.allowCreate) {
                            GlideButton(onClick = { resetFormForCreate() }) {
                                Text(if (compact) "New" else ui.newButtonLabel)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.field))

                    if (groups.isEmpty()) {
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
                                text = ui.emptyListMessage,
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
                            items(groups, key = { it.id }) { group ->
                                PeopleGroupListItem(
                                    group = group,
                                    dateFormat = dateFormat,
                                    selected = group.id == selectedId,
                                    compact = compact,
                                    showPipelineStatus = ui.showPipelineStatus,
                                    onClick = { loadIntoForm(group) },
                                )
                            }
                        }
                    }
                }
            }

            val formSection: @Composable (Modifier) -> Unit = { formModifier ->
                val showForm = selectedId != null || (ui.allowCreate && isCreating)

                Column(modifier = formModifier.fillMaxHeight()) {
                    Text(
                        text = when {
                            !showForm -> ui.editFormTitle
                            isCreating -> ui.createFormTitle
                            else -> ui.editFormTitle
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(spacing.section))

                    if (!showForm) {
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
                                text = ui.noSelectionMessage,
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
                            if (ui.readOnly && selectedId != null) {
                                val group = PeopleGroupStore.findById(selectedId!!)
                                if (group != null) {
                                    CustomerGroupDetailView(
                                        group = group,
                                        spacing = spacing,
                                        onCloneToLead = {
                                            cloneMessage = null
                                            val lead = PeopleGroupStore.cloneToLead(group.id)
                                            if (lead != null) {
                                                PeopleGroupNavigation.openLead(lead.id)
                                                cloneMessage =
                                                    "Lead created from this customer group. Open the Leads panel to edit and convert."
                                            } else {
                                                cloneMessage = "Could not create a lead from this group."
                                            }
                                        },
                                    )
                                }

                                cloneMessage?.let { message ->
                                    Spacer(modifier = Modifier.height(spacing.field))
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                PeopleGroupForm(
                                    state = formState,
                                    onStateChange = { formState = it },
                                    spacing = spacing,
                                    notesHeight = notesHeight,
                                    isCustomerGroup = ui.type == PeopleGroupType.CUSTOMER,
                                    showPipelineStatus = ui.showPipelineStatus,
                                    showPlanPicker = ui.showPlanPicker,
                                )

                                formError?.let { error ->
                                    Spacer(modifier = Modifier.height(spacing.field))
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }

                        if (ui.showConvertToCustomer && !isCreating && selectedId != null) {
                            Spacer(modifier = Modifier.height(spacing.field))
                            val canConvert = formState.hasPlanSelected() && PlanStore.plans.isNotEmpty()
                            GlideOutlinedButton(
                                onClick = {
                                    if (!formState.isValidForLead()) {
                                        formError = "Main contact name is required."
                                        return@GlideOutlinedButton
                                    }
                                    if (!formState.hasPlanSelected()) {
                                        formError = "Select a pack before making a customer."
                                        return@GlideOutlinedButton
                                    }
                                    val existing = PeopleGroupStore.findById(selectedId!!) ?: return@GlideOutlinedButton
                                    val updated = formState.toPeopleGroup(
                                        type = existing.type,
                                        existingId = existing.id,
                                        createdAtMillis = existing.createdAtMillis,
                                    )
                                    PeopleGroupStore.update(updated)
                                    if (!PeopleGroupStore.convertToCustomer(selectedId!!)) {
                                        formError = "Select a pack before making a customer."
                                        return@GlideOutlinedButton
                                    }
                                    formError = null
                                    resetFormForCreate()
                                },
                                enabled = canConvert,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Make customer")
                            }
                            if (!canConvert) {
                                Spacer(modifier = Modifier.height(spacing.field))
                                Text(
                                    text = if (PlanStore.plans.isEmpty()) {
                                        "Create a pack in the Plans panel first."
                                    } else {
                                        "Select a pack above to enable conversion."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (!ui.readOnly) {
                            Spacer(modifier = Modifier.height(spacing.field))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.field),
                            ) {
                                GlideButton(
                                    onClick = {
                                        formError = null
                                        if (ui.type == PeopleGroupType.LEAD && !formState.isValidForLead()) {
                                            formError = "Link a contact or enter a new contact name."
                                            return@GlideButton
                                        }
                                        if (isCreating && ui.allowCreate) {
                                            val group = formState.toPeopleGroup(type = PeopleGroupType.LEAD)
                                            PeopleGroupStore.create(group)
                                            loadIntoForm(group)
                                        } else {
                                            val existing = selectedId?.let { PeopleGroupStore.findById(it) }
                                            if (existing != null) {
                                                val updated = formState.toPeopleGroup(
                                                    type = existing.type,
                                                    existingId = existing.id,
                                                    createdAtMillis = existing.createdAtMillis,
                                                )
                                                if (!PeopleGroupStore.update(updated)) {
                                                    formError = "This record cannot be changed."
                                                    return@GlideButton
                                                }
                                                loadIntoForm(updated)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(saveLabel)
                                }

                                if (!isCreating && selectedId != null) {
                                    GlideOutlinedButton(onClick = { showDeleteConfirm = true }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (compact) {
                listSection(Modifier.fillMaxWidth())
                HorizontalDivider(modifier = Modifier.padding(vertical = spacing.section))
                formSection(Modifier.fillMaxWidth().weight(1f))
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.section),
                ) {
                    listSection(Modifier.weight(0.42f).fillMaxHeight())
                    VerticalDivider(modifier = Modifier.fillMaxHeight())
                    formSection(Modifier.weight(0.58f).fillMaxHeight())
                }
            }
        }
    }

    if (showDeleteConfirm && selectedId != null && !ui.readOnly) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(ui.deleteConfirmTitle) },
            text = { Text(ui.deleteConfirmMessage) },
            confirmButton = {
                GlideTextButton(
                    onClick = {
                        if (PeopleGroupStore.delete(selectedId!!)) {
                            showDeleteConfirm = false
                            if (ui.allowCreate) {
                                resetFormForCreate()
                            } else {
                                clearSelection()
                            }
                        } else {
                            showDeleteConfirm = false
                            formError = "This record cannot be deleted."
                        }
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                GlideTextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun PeopleGroupListItem(
    group: PeopleGroup,
    dateFormat: SimpleDateFormat,
    selected: Boolean,
    compact: Boolean,
    showPipelineStatus: Boolean,
    onClick: () -> Unit,
) {
    val spacing = if (compact) GlideLayout.compact else GlideLayout.comfortable
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
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
        val main = group.resolveMainContact()
        Text(
            text = formatPersonLabel(main.name, main.dateOfBirth),
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (main.email.isNotBlank() && !compact) {
            Text(
                text = main.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        group.relatedPeopleSummary(compact)?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        planLabelForGroup(group)?.let { packLabel ->
            Text(
                text = packLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (showPipelineStatus) {
                Text(
                    text = group.status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "Customer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = dateFormat.format(Date(group.createdAtMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun planLabelForGroup(group: PeopleGroup): String? {
    val planId = group.planId ?: return null
    val plan = PlanStore.findById(planId) ?: return null
    return "Pack: ${plan.name}"
}

private fun PeopleGroup.relatedPeopleSummary(compact: Boolean): String? {
    val related = resolveRelatedPeople()
    if (related.isEmpty()) return null
    val labels = related.joinToString { formatPersonLabel(it.name, it.dateOfBirth) }
    return if (compact) labels else "Related: $labels"
}

@Composable
private fun CustomerGroupDetailView(
    group: PeopleGroup,
    spacing: GlideLayout.Spacing,
    onCloneToLead: () -> Unit,
) {
    ReadOnlyMainContactSection(contactId = group.mainContactId)
    Spacer(modifier = Modifier.height(spacing.section))
    ReadOnlyRelatedPeopleSection(relatedPersonIds = group.relatedPersonIds)
    Spacer(modifier = Modifier.height(spacing.field))
    ReadOnlyPackSection(planId = group.planId)
    Spacer(modifier = Modifier.height(spacing.field))
    if (group.notes.isNotBlank()) {
        GlideFieldLabel("Notes")
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = group.notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    Spacer(modifier = Modifier.height(spacing.field))
    Text(
        text = "This customer group is locked. Clone to a lead to create another pack with the same people.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(spacing.field))
    GlideOutlinedButton(
        onClick = onCloneToLead,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Clone to new lead")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeopleGroupForm(
    state: PeopleGroupFormState,
    onStateChange: (PeopleGroupFormState) -> Unit,
    spacing: GlideLayout.Spacing,
    notesHeight: Dp,
    isCustomerGroup: Boolean,
    showPipelineStatus: Boolean,
    showPlanPicker: Boolean,
) {
    var statusExpanded by remember { mutableStateOf(false) }

    if (isCustomerGroup) {
        ReadOnlyMainContactSection(contactId = state.mainContactId)
    } else {
        LeadMainContactSection(
            mainContactId = state.mainContactId,
            contactName = state.contactName,
            dateOfBirth = state.dateOfBirth,
            email = state.email,
            phone = state.phone,
            onStateChange = { contactId, name, dob, email, phone ->
                onStateChange(
                    state.copy(
                        mainContactId = contactId,
                        contactName = name,
                        dateOfBirth = dob,
                        email = email,
                        phone = phone,
                    ),
                )
            },
            spacing = spacing,
        )
    }
    Spacer(modifier = Modifier.height(spacing.section * 2))

    if (isCustomerGroup) {
        RelatedPersonLinkSection(
            selectedIds = state.relatedPersonIds,
            onSelectionChange = { onStateChange(state.copy(relatedPersonIds = it)) },
            spacing = spacing,
        )
    } else {
        LeadRelatedPeopleSection(
            selectedIds = state.relatedPersonIds,
            onSelectionChange = { onStateChange(state.copy(relatedPersonIds = it)) },
            spacing = spacing,
        )
    }

    if (showPlanPicker) {
        Spacer(modifier = Modifier.height(spacing.field))
        PlanPackDropdown(
            selectedPlanId = state.planId,
            onPlanSelected = { onStateChange(state.copy(planId = it)) },
        )
    }

    if (showPipelineStatus) {
        Spacer(modifier = Modifier.height(spacing.field))
        Column {
            GlideFieldLabel("Status")
            Spacer(modifier = Modifier.height(2.dp))
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it },
            ) {
                OutlinedTextField(
                    value = state.status.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = GlideDimensions.fieldHeight)
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false },
                ) {
                    PeopleGroupStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.label, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onStateChange(state.copy(status = status))
                                statusExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(spacing.field))
    GlideOutlinedField(
        value = state.notes,
        onValueChange = { onStateChange(state.copy(notes = it)) },
        label = "Notes",
        singleLine = false,
        minLines = 2,
        maxLines = 4,
        fieldHeight = notesHeight,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanPackDropdown(
    selectedPlanId: String?,
    onPlanSelected: (String) -> Unit,
) {
    val plans = PlanStore.plans
    var expanded by remember { mutableStateOf(false) }
    val selectedPlan = plans.find { it.id == selectedPlanId }
    val displayValue = selectedPlan?.let { "${it.name} (${it.summaryLine()})" }
        ?: if (plans.isEmpty()) "No packs available" else "Select a pack"

    Column {
        GlideFieldLabel("Pack")
        Spacer(modifier = Modifier.height(2.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (plans.isNotEmpty()) expanded = it },
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                enabled = plans.isNotEmpty(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = GlideDimensions.fieldHeight)
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                plans.forEach { plan ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(plan.name, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    plan.summaryLine(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onPlanSelected(plan.id)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (plans.isEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add multi lesson packs in the Plans panel.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
