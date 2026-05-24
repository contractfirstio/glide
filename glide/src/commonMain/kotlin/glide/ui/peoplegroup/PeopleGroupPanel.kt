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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.data.BillStore
import glide.data.BillingPanelState
import glide.data.ContactStore
import glide.data.ContactsPanelState
import glide.data.LocationStore
import glide.data.PlansPanelState
import glide.data.RelatedPanelState
import glide.data.RelatedPersonStore
import glide.data.SchedulePanelState
import glide.data.TermStore
import glide.data.PlanEnrollmentStore
import glide.data.PeopleGroupNavigation
import glide.data.PeopleGroupStore
import glide.data.soldPlanDeletionBlockReason
import glide.data.PlanStore
import glide.data.ScheduledClassStore
import glide.model.formatMoney
import glide.data.classAttendeeCount
import glide.data.memberCount
import glide.data.resolveMainContact
import glide.data.resolveRelatedPeople
import glide.model.PeopleGroup
import glide.model.PeopleGroupStatus
import glide.model.PeopleGroupType
import glide.model.parseIsoLocalDate
import glide.model.summaryLine
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.leads.millisToIsoDate
import glide.ui.leads.todayIsoDate
import glide.ui.shared.formatPersonLabel
import glide.ui.layout.GlideLayout
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.glideListItemTitleColor
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.shared.DeleteConfirmDialog
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.IsoDateField
import glide.ui.shared.rememberFormDirtyTracker
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
    val showClearSelection: Boolean = false,
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
    showClearSelection = true,
)

private val CustomersPanelUi = PeopleGroupPanelUi(
    type = PeopleGroupType.CUSTOMER,
    subtitle = "Customer groups are locked after creation. Clone one to a new lead to build another plan.",
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
    deleteConfirmTitle = "Revert sold plan to lead?",
    deleteConfirmMessage = "Billing and class enrollment for this sold plan will be removed. " +
        "The household will become a lead again with the same contact and plan selection.",
    showClearSelection = true,
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
    val planStartDate: String = "",
    val mainContactAttendsClass: Boolean = true,
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
            planStartDate = planStartDate.trim(),
            mainContactAttendsClass = mainContactAttendsClass,
            notes = notes.trim(),
            createdAtMillis = createdAtMillis,
        )
    }
}

private fun soldDisabledReason(
    groupId: String?,
    isDirty: Boolean,
    hasPlanSelected: Boolean,
): String? =
    when {
        PlanStore.plans.isEmpty() -> "Create a plan in the Plans panel first."
        isDirty -> "Save changes before marking as sold."
        else -> {
            val group = groupId?.let { PeopleGroupStore.findById(it) }
            val main = group?.resolveMainContact()
            when {
                main == null || main.name.isBlank() ->
                    "Main contact name is required before marking as sold."
                main.email.isBlank() || main.phone.isBlank() ->
                    "Email and phone are required before marking as sold."
                !hasPlanSelected -> "Select a plan above to enable Sold."
                group?.planStartDate.isNullOrBlank() ||
                    parseIsoLocalDate(group.planStartDate) == null ->
                    "Plan start date is required before marking as sold."
                else -> null
            }
        }
    }

private fun canMarkLeadSold(
    groupId: String?,
    isDirty: Boolean,
    hasPlanSelected: Boolean,
): Boolean = soldDisabledReason(groupId, isDirty, hasPlanSelected) == null

@Composable
private fun PeopleGroupPanel(
    ui: PeopleGroupPanelUi,
    modifier: Modifier = Modifier,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val form = rememberFormDirtyTracker(PeopleGroupFormState(planStartDate = todayIsoDate()))
    var isCreating by remember(ui.allowCreate) { mutableStateOf(ui.allowCreate) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var cloneMessage by remember { mutableStateOf<String?>(null) }

    val contactFilterId = if (ui.type == PeopleGroupType.CUSTOMER) {
        ContactsPanelState.selectedContactId
    } else {
        null
    }
    val planFilterId = if (ui.type == PeopleGroupType.CUSTOMER) {
        PlansPanelState.selectedPlanId
    } else {
        null
    }
    val relatedFilterId = if (ui.type == PeopleGroupType.CUSTOMER) {
        RelatedPanelState.selectedRelatedPersonId
    } else {
        null
    }
    val schedulingSoldPlansPanel =
        ui.type == PeopleGroupType.CUSTOMER && AppViewState.mode == AppViewMode.SCHEDULING
    val soldPlanFilterId = SchedulePanelState.selectedSoldPlanId
    val termFilterId = SchedulePanelState.selectedTermFilterId
    val locationFilterId = SchedulePanelState.selectedLocationFilterId
    val classFilterId = if (schedulingSoldPlansPanel && soldPlanFilterId == null) {
        SchedulePanelState.selectedClassId
    } else {
        null
    }
    val soldPlansTermFilterId = if (schedulingSoldPlansPanel && soldPlanFilterId == null && classFilterId == null) {
        termFilterId
    } else {
        null
    }
    val soldPlansLocationFilterId = if (
        schedulingSoldPlansPanel &&
        soldPlanFilterId == null &&
        classFilterId == null &&
        termFilterId == null
    ) {
        locationFilterId
    } else {
        null
    }
    val groups = when {
        schedulingSoldPlansPanel -> PeopleGroupStore.forSchedulingSoldPlans(
            classId = classFilterId,
            termId = soldPlansTermFilterId,
            locationId = soldPlansLocationFilterId,
        )
        ui.type == PeopleGroupType.LEAD -> PeopleGroupStore.leads
        else -> PeopleGroupStore.forCustomersPanel(contactFilterId, relatedFilterId, planFilterId)
    }
    val contactFilterLabel = contactFilterId?.let { ContactStore.findById(it)?.name?.takeIf { it.isNotBlank() } }
    val planFilterLabel = planFilterId?.let { PlanStore.findById(it)?.name?.takeIf { it.isNotBlank() } }
    val relatedFilterLabel = relatedFilterId?.let {
        RelatedPersonStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val classFilterLabel = classFilterId?.let {
        ScheduledClassStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val termFilterLabel = soldPlansTermFilterId?.let {
        TermStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val locationFilterLabel = soldPlansLocationFilterId?.let {
        LocationStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    fun resetFormForCreate() {
        selectedId = null
        isCreating = ui.allowCreate
        form.load(PeopleGroupFormState(planStartDate = todayIsoDate()))
        formError = null
    }

    fun clearLocalSelection() {
        selectedId = null
        isCreating = false
        form.load(PeopleGroupFormState(planStartDate = todayIsoDate()))
        formError = null
    }

    fun clearSelection() {
        clearLocalSelection()
        if (schedulingSoldPlansPanel) {
            SchedulePanelState.onSoldPlanCleared()
        } else if (ui.type == PeopleGroupType.CUSTOMER) {
            BillingPanelState.onCustomerGroupCleared()
        }
    }

    fun loadIntoForm(group: PeopleGroup) {
        selectedId = group.id
        isCreating = false
        val main = group.resolveMainContact()
        form.load(
            PeopleGroupFormState(
                mainContactId = group.mainContactId,
                contactName = if (group.mainContactId != null) "" else main.name,
                dateOfBirth = if (group.mainContactId != null) "" else main.dateOfBirth,
                email = if (group.mainContactId != null) "" else main.email,
                phone = if (group.mainContactId != null) "" else main.phone,
                relatedPersonIds = group.relatedPersonIds,
                status = group.status,
                planId = group.planId,
                planStartDate = group.planStartDate.ifBlank { todayIsoDate() },
                mainContactAttendsClass = group.mainContactAttendsClass,
                notes = group.notes,
            ),
        )
        formError = null
        if (schedulingSoldPlansPanel) {
            SchedulePanelState.onSoldPlanSelected(group.id)
        }
    }

    val pendingLeadId = PeopleGroupNavigation.pendingLeadId
    LaunchedEffect(pendingLeadId, ui.type) {
        if (ui.type == PeopleGroupType.LEAD && pendingLeadId != null) {
            PeopleGroupStore.findById(pendingLeadId)?.let { loadIntoForm(it) }
            PeopleGroupNavigation.clearPendingLead()
        }
    }

    val pendingCustomerGroupId = PeopleGroupNavigation.pendingCustomerGroupId
    LaunchedEffect(pendingCustomerGroupId, ui.type) {
        if (ui.type == PeopleGroupType.CUSTOMER && pendingCustomerGroupId != null) {
            PeopleGroupStore.findById(pendingCustomerGroupId)?.let { group ->
                loadIntoForm(group)
                BillingPanelState.onCustomerGroupSelected(group.id)
            }
            PeopleGroupNavigation.clearPendingCustomerGroup()
        }
    }

    LaunchedEffect(BillingPanelState.peopleGroupId) {
        if (ui.type != PeopleGroupType.CUSTOMER || schedulingSoldPlansPanel) return@LaunchedEffect
        when (BillingPanelState.peopleGroupId) {
            null -> if (selectedId != null) clearLocalSelection()
            else -> if (selectedId != null && selectedId != BillingPanelState.peopleGroupId) {
                clearLocalSelection()
            }
        }
    }

    LaunchedEffect(contactFilterId, planFilterId, relatedFilterId, classFilterId, soldPlanFilterId, soldPlansTermFilterId, soldPlansLocationFilterId) {
        if (ui.type == PeopleGroupType.CUSTOMER && soldPlanFilterId != null) return@LaunchedEffect
        if (ui.type == PeopleGroupType.CUSTOMER &&
            (contactFilterId != null || planFilterId != null || relatedFilterId != null ||
                classFilterId != null || soldPlansTermFilterId != null || soldPlansLocationFilterId != null) &&
            selectedId != null
        ) {
            clearLocalSelection()
        }
    }

    LaunchedEffect(
        contactFilterId,
        planFilterId,
        relatedFilterId,
        classFilterId,
        soldPlanFilterId,
        soldPlansTermFilterId,
        soldPlansLocationFilterId,
        groups,
        selectedId,
    ) {
        if (ui.type == PeopleGroupType.CUSTOMER &&
            selectedId != null &&
            groups.none { it.id == selectedId }
        ) {
            clearSelection()
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
                    text = when {
                        schedulingSoldPlansPanel && classFilterId != null -> {
                            val label = classFilterLabel ?: "this class"
                            "Showing sold plans on $label. Use Clear filter to reset."
                        }
                        schedulingSoldPlansPanel && soldPlansTermFilterId != null -> {
                            val label = termFilterLabel ?: "this term"
                            "Showing sold plans in $label. Use Clear filter to reset."
                        }
                        schedulingSoldPlansPanel && soldPlansLocationFilterId != null -> {
                            val label = locationFilterLabel ?: "this location"
                            "Showing sold plans at $label. Use Clear filter to reset."
                        }
                        ui.type == PeopleGroupType.CUSTOMER && planFilterId != null -> {
                            val label = planFilterLabel ?: "this plan"
                            "Showing customer groups on $label. Use Clear filter in Plans to reset."
                        }
                        ui.type == PeopleGroupType.CUSTOMER && relatedFilterId != null -> {
                            val label = relatedFilterLabel ?: "this related person"
                            "Showing customer groups for $label. Use Clear filter in Related to reset."
                        }
                        ui.type == PeopleGroupType.CUSTOMER && contactFilterId != null -> {
                            val label = contactFilterLabel ?: "this contact"
                            "Showing customer groups for $label. Use Clear filter to reset."
                        }
                        schedulingSoldPlansPanel ->
                            "Select a sold plan to see class assignment and balance."
                        else -> ui.subtitle
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
                            text = ui.listCountLabel(groups.size),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.field)) {
                            if (schedulingSoldPlansPanel && classFilterId != null) {
                                GlideTextButton(onClick = { SchedulePanelState.clearClassFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (schedulingSoldPlansPanel && soldPlansTermFilterId != null) {
                                GlideTextButton(onClick = { SchedulePanelState.clearTermFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (schedulingSoldPlansPanel && soldPlansLocationFilterId != null) {
                                GlideTextButton(onClick = { SchedulePanelState.clearLocationFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (ui.type == PeopleGroupType.CUSTOMER && planFilterId != null) {
                                GlideTextButton(onClick = { PlansPanelState.clearPlanFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (ui.type == PeopleGroupType.CUSTOMER && relatedFilterId != null) {
                                GlideTextButton(onClick = { RelatedPanelState.clearRelatedPersonFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (ui.type == PeopleGroupType.CUSTOMER && contactFilterId != null) {
                                GlideTextButton(onClick = { ContactsPanelState.clearContactFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (ui.showClearSelection && selectedId != null) {
                                GlideTextButton(onClick = { clearSelection() }) {
                                    Text("Clear")
                                }
                            }
                            if (ui.allowCreate) {
                                GlideButton(onClick = { resetFormForCreate() }) {
                                    Text(if (compact) "New" else ui.newButtonLabel)
                                }
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
                                text = when {
                                    ui.type == PeopleGroupType.CUSTOMER && planFilterId != null ->
                                        "No customer groups on this plan."
                                    ui.type == PeopleGroupType.CUSTOMER && relatedFilterId != null ->
                                        "No customer groups for this related person."
                                    ui.type == PeopleGroupType.CUSTOMER && contactFilterId != null ->
                                        "No customer groups for this contact."
                                    schedulingSoldPlansPanel && classFilterId != null ->
                                        "No sold plans on this class."
                                    schedulingSoldPlansPanel && soldPlansTermFilterId != null ->
                                        "No sold plans in this term."
                                    schedulingSoldPlansPanel && soldPlansLocationFilterId != null ->
                                        "No sold plans at this location."
                                    else -> ui.emptyListMessage
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
                            items(groups, key = { it.id }) { group ->
                                PeopleGroupListItem(
                                    group = group,
                                    dateFormat = dateFormat,
                                    selected = group.id == selectedId,
                                    compact = compact,
                                    showPipelineStatus = ui.showPipelineStatus,
                                    emphasizePlan = ui.type == PeopleGroupType.CUSTOMER,
                                    onClick = {
                                        if (group.id == selectedId) {
                                            clearSelection()
                                        } else {
                                            loadIntoForm(group)
                                            if (!schedulingSoldPlansPanel && ui.type == PeopleGroupType.CUSTOMER) {
                                                BillingPanelState.onCustomerGroupSelected(group.id)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            val formSection: @Composable (Modifier) -> Unit = { formModifier ->
                val showForm = selectedId != null || (ui.allowCreate && isCreating)
                val selectedGroup = selectedId?.let { PeopleGroupStore.findById(it) }

                Column(modifier = formModifier.fillMaxHeight()) {
                    Text(
                        text = when {
                            !showForm -> if (schedulingSoldPlansPanel) {
                                "Sold plan details"
                            } else {
                                ui.editFormTitle
                            }
                            isCreating -> ui.createFormTitle
                            schedulingSoldPlansPanel && selectedGroup != null -> {
                                val main = selectedGroup.resolveMainContact()
                                val plan = planNameForGroup(selectedGroup)
                                if (main.name.isNotBlank()) "$plan · ${main.name}" else plan
                            }
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
                                text = if (schedulingSoldPlansPanel) {
                                    "Select a sold plan to see which class it is on and any outstanding balance."
                                } else {
                                    ui.noSelectionMessage
                                },
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
                                    if (schedulingSoldPlansPanel) {
                                        SchedulingSoldPlanDetailView(
                                            group = group,
                                            spacing = spacing,
                                            onDelete = { showDeleteConfirm = true },
                                        )
                                    } else {
                                        CustomerGroupDetailView(
                                            group = group,
                                            spacing = spacing,
                                            onOpenBilling = {
                                                BillingPanelState.reopenForCurrentGroup()
                                            },
                                            onDelete = { showDeleteConfirm = true },
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
                                    state = form.draft,
                                    onStateChange = { form.draft = it },
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

                        if (!ui.readOnly) {
                            if (ui.showConvertToCustomer && !isCreating && selectedId != null) {
                                soldDisabledReason(
                                    groupId = selectedId,
                                    isDirty = form.isDirty,
                                    hasPlanSelected = form.draft.hasPlanSelected(),
                                )?.let { reason ->
                                    Spacer(modifier = Modifier.height(spacing.field))
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        formError = null
                                        if (ui.type == PeopleGroupType.LEAD && !form.draft.isValidForLead()) {
                                            formError = "Link a contact or enter a new contact name."
                                            return@GlideButton
                                        }
                                        if (isCreating && ui.allowCreate) {
                                            val group = form.draft.toPeopleGroup(type = PeopleGroupType.LEAD)
                                            PeopleGroupStore.create(group)
                                            loadIntoForm(group)
                                        } else {
                                            val existing = selectedId?.let { PeopleGroupStore.findById(it) }
                                            if (existing != null) {
                                                val updated = form.draft.toPeopleGroup(
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
                                    enabled = form.isDirty,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(saveLabel)
                                }

                                if (ui.showConvertToCustomer && !isCreating && selectedId != null) {
                                    GlideOutlinedButton(
                                        onClick = {
                                            if (!form.draft.isValidForLead()) {
                                                formError = "Main contact name is required."
                                                return@GlideOutlinedButton
                                            }
                                            if (!form.draft.hasPlanSelected()) {
                                                formError = "Select a plan before marking as sold."
                                                return@GlideOutlinedButton
                                            }
                                            if (form.draft.planStartDate.isBlank() ||
                                                parseIsoLocalDate(form.draft.planStartDate) == null
                                            ) {
                                                formError = "Plan start date is required before marking as sold."
                                                return@GlideOutlinedButton
                                            }
                                            val existing = PeopleGroupStore.findById(selectedId!!)
                                                ?: return@GlideOutlinedButton
                                            val updated = form.draft.toPeopleGroup(
                                                type = existing.type,
                                                existingId = existing.id,
                                                createdAtMillis = existing.createdAtMillis,
                                            )
                                            PeopleGroupStore.update(updated)
                                            if (!PeopleGroupStore.convertToCustomer(selectedId!!)) {
                                                formError = "Select a plan before marking as sold."
                                                return@GlideOutlinedButton
                                            }
                                            formError = null
                                            resetFormForCreate()
                                        },
                                        enabled = canMarkLeadSold(
                                            groupId = selectedId,
                                            isDirty = form.isDirty,
                                            hasPlanSelected = form.draft.hasPlanSelected(),
                                        ),
                                    ) {
                                        Text("Sold")
                                    }
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

    if (showDeleteConfirm && selectedId != null && (ui.type == PeopleGroupType.CUSTOMER || !ui.readOnly)) {
        val soldPlanBlockReason = if (ui.type == PeopleGroupType.CUSTOMER) {
            soldPlanDeletionBlockReason(selectedId!!)
        } else {
            null
        }
        DeleteConfirmDialog(
            title = ui.deleteConfirmTitle,
            message = soldPlanBlockReason ?: ui.deleteConfirmMessage,
            onDismiss = { showDeleteConfirm = false },
            onContinue = {
                val groupId = selectedId!!
                if (PeopleGroupStore.delete(groupId)) {
                    showDeleteConfirm = false
                    when {
                        ui.type == PeopleGroupType.CUSTOMER -> {
                            PeopleGroupNavigation.openLead(groupId)
                            clearSelection()
                        }
                        ui.allowCreate -> resetFormForCreate()
                        else -> clearSelection()
                    }
                } else {
                    showDeleteConfirm = false
                    formError = soldPlanBlockReason
                        ?: "This record cannot be deleted."
                }
            },
            continueEnabled = soldPlanBlockReason == null,
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
    emphasizePlan: Boolean,
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
        if (emphasizePlan) {
            Text(
                text = main.name.ifBlank { "Unknown contact" },
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = glideListItemTitleColor(selected),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = planNameForGroup(group),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = planStartDateLabelForGroup(group),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = formatPersonLabel(main.name, main.dateOfBirth),
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = glideListItemTitleColor(selected),
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
            planLabelForGroup(group)?.let { planLabel ->
                Text(
                    text = planLabel,
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
}

private fun planNameForGroup(group: PeopleGroup): String =
    group.planId
        ?.let { PlanStore.findById(it)?.name?.takeIf { name -> name.isNotBlank() } }
        ?: "No plan"

private fun planStartDateLabelForGroup(group: PeopleGroup): String {
    val fromGroup = group.planStartDate.takeIf { it.isNotBlank() }
        ?.let { formatIsoDateForDisplay(it) }
        ?.takeIf { it.isNotBlank() }
    if (fromGroup != null) return fromGroup

    return PlanEnrollmentStore.forPeopleGroup(group.id)
        ?.startedAtMillis
        ?.let { formatIsoDateForDisplay(millisToIsoDate(it)) }
        ?.takeIf { it.isNotBlank() }
        ?: "No start date"
}

private fun planLabelForGroup(group: PeopleGroup): String? {
    val planId = group.planId ?: return null
    val plan = PlanStore.findById(planId) ?: return null
    return "Plan: ${plan.name}"
}

private fun PeopleGroup.relatedPeopleSummary(compact: Boolean): String? {
    val related = resolveRelatedPeople()
    if (related.isEmpty()) return null
    val labels = related.joinToString { formatPersonLabel(it.name, it.dateOfBirth) }
    return if (compact) labels else "Related: $labels"
}

@Composable
private fun SoldPlanDeleteSection(
    groupId: String,
    spacing: GlideLayout.Spacing,
    onDelete: () -> Unit,
) {
    val blockReason = soldPlanDeletionBlockReason(groupId)
    Spacer(modifier = Modifier.height(spacing.section))
    FormPanelSectionsDivider(label = "Danger zone", spacing = spacing)
    FormPanelSection(
        title = "Revert to lead",
        description = "Remove this sold plan and turn the household back into a lead. " +
            "Only sold plans that are not on a class and have no issued or paid bills can be reverted.",
        spacing = spacing,
        role = FormPanelSectionRole.Tertiary,
    ) {
        blockReason?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(spacing.field))
        }
        GlideOutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            enabled = blockReason == null,
        ) {
            Text("Revert to lead", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SchedulingSoldPlanDetailView(
    group: PeopleGroup,
    spacing: GlideLayout.Spacing,
    onDelete: () -> Unit,
) {
    val enrollment = PlanEnrollmentStore.displayForPeopleGroup(group.id)
    val outstanding = enrollment?.let { BillStore.outstandingMinorForEnrollment(it.id) } ?: 0L
    val currencyCode = enrollment?.planSnapshot?.currencyCode
    val scheduledClass = ScheduledClassStore.findClassContainingCustomerGroup(group.id)

    FormPanelSection(
        title = planNameForGroup(group),
        description = "Class assignment and plan balance.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        GlideFieldLabel("Class")
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = scheduledClass?.name?.takeIf { it.isNotBlank() } ?: "Not assigned to a class",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (scheduledClass != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(modifier = Modifier.height(spacing.field))
        GlideFieldLabel("Outstanding balance")
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = when {
                enrollment == null -> "No billing enrollment"
                outstanding > 0 -> formatMoney(
                    outstanding,
                    currencyCode ?: enrollment.planSnapshot.currencyCode,
                )
                else -> "No outstanding bills"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = when {
                enrollment == null -> MaterialTheme.colorScheme.onSurfaceVariant
                outstanding > 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            },
        )
    }
    SoldPlanDeleteSection(
        groupId = group.id,
        spacing = spacing,
        onDelete = onDelete,
    )
}

@Composable
private fun CustomerGroupDetailView(
    group: PeopleGroup,
    spacing: GlideLayout.Spacing,
    onOpenBilling: () -> Unit,
    onDelete: () -> Unit,
    onCloneToLead: () -> Unit,
) {
    val enrollment = PlanEnrollmentStore.forPeopleGroup(group.id)
    val outstanding = enrollment?.let { BillStore.outstandingMinorForEnrollment(it.id) } ?: 0L

    FormPanelSection(
        title = "Plan",
        description = "The plan assigned to this customer group.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        ReadOnlyPlanSection(planId = group.planId, prominent = false)
    }

    FormPanelSectionsDivider(label = "People on this plan", spacing = spacing)

    FormPanelSection(
        title = "Main contact",
        description = "The primary person for billing and household identity.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        ReadOnlyMainContactSection(contactId = group.mainContactId, showLabel = false)
        Spacer(modifier = Modifier.height(spacing.field))
        Text(
            text = if (group.mainContactAttendsClass) {
                "Main contact attends class"
            } else {
                "Main contact does not attend class"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Related people always attend. Set on the lead before conversion.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }

    FormPanelSection(
        title = "Related people",
        description = "Others on this plan besides the main contact.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        ReadOnlyRelatedPeopleSection(relatedPersonIds = group.relatedPersonIds, showLabel = false)
        Text(
            text = "${group.classAttendeeCount()} attending on classes · ${group.memberCount()} in household",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = spacing.field),
        )
    }

    FormPanelSectionsDivider(label = "Billing & actions", spacing = spacing)

    FormPanelSection(
        title = "Details & billing",
        description = "Notes, outstanding balance, and next steps.",
        spacing = spacing,
        role = FormPanelSectionRole.Tertiary,
    ) {
        if (group.notes.isNotBlank()) {
            GlideFieldLabel("Notes")
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = group.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(spacing.field))
        }
        enrollment?.let {
            Text(
                text = if (outstanding > 0) {
                    "Outstanding: ${formatMoney(outstanding, it.planSnapshot.currencyCode)}"
                } else {
                    "No outstanding bills"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (outstanding > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(modifier = Modifier.height(spacing.field))
        }
        GlideButton(
            onClick = onOpenBilling,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Billing")
        }
        Spacer(modifier = Modifier.height(spacing.field))
        Text(
            text = "This customer group is locked. Clone to a lead to create another plan with the same people.",
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
    SoldPlanDeleteSection(
        groupId = group.id,
        spacing = spacing,
        onDelete = onDelete,
    )
}

@Composable
private fun LeadMainContactAttendsField(
    attends: Boolean,
    onAttendsChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = attends,
            onCheckedChange = onAttendsChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = "Main contact attends class",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (attends) {
                    "Main contact counts toward room capacity when this group is on a class."
                } else {
                    "Only related people attend; main contact is not counted on classes."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    if (!isCustomerGroup) {
        LeadPeopleSectionsDivider(spacing = spacing)
    } else {
        Spacer(modifier = Modifier.height(spacing.section * 2))
    }

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

    if (!isCustomerGroup && (showPlanPicker || showPipelineStatus)) {
        FormPanelSectionsDivider(label = "Lead setup", spacing = spacing)
    }

    if (showPlanPicker) {
        FormPanelSection(
            title = "Plan",
            description = "Select the plan before converting this lead to a customer.",
            spacing = spacing,
            role = FormPanelSectionRole.Tertiary,
        ) {
            PlanDropdown(
                selectedPlanId = state.planId,
                onPlanSelected = { onStateChange(state.copy(planId = it)) },
            )
            if (!isCustomerGroup) {
                Spacer(modifier = Modifier.height(spacing.field))
                LeadMainContactAttendsField(
                    attends = state.mainContactAttendsClass,
                    onAttendsChange = { onStateChange(state.copy(mainContactAttendsClass = it)) },
                )
            }
        }
    } else if (!isCustomerGroup) {
        Spacer(modifier = Modifier.height(spacing.field))
        LeadMainContactAttendsField(
            attends = state.mainContactAttendsClass,
            onAttendsChange = { onStateChange(state.copy(mainContactAttendsClass = it)) },
        )
    }

    if (showPipelineStatus) {
        FormPanelSection(
            title = "Pipeline",
            description = "Track where this lead is in your sales process.",
            spacing = spacing,
            role = FormPanelSectionRole.Tertiary,
        ) {
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
    }

    Spacer(modifier = Modifier.height(spacing.section))
    FormPanelSection(
        title = "Notes",
        description = "Internal notes about this lead.",
        spacing = spacing,
        role = FormPanelSectionRole.Tertiary,
    ) {
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

    if (showPlanPicker) {
        Spacer(modifier = Modifier.height(spacing.section))
        FormPanelSection(
            title = "Plan start",
            description = "When this plan begins for the customer.",
            spacing = spacing,
            role = FormPanelSectionRole.Tertiary,
        ) {
            IsoDateField(
                label = "Start date",
                value = state.planStartDate,
                onValueChange = { onStateChange(state.copy(planStartDate = it)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanDropdown(
    selectedPlanId: String?,
    onPlanSelected: (String) -> Unit,
) {
    val plans = PlanStore.plans
    var expanded by remember { mutableStateOf(false) }
    val selectedPlan = plans.find { it.id == selectedPlanId }
    val displayValue = selectedPlan?.let { "${it.name} (${it.summaryLine()})" }
        ?: if (plans.isEmpty()) "No plans available" else "Select a plan"

    Column {
        GlideFieldLabel("Plan")
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
                text = "Add multi lesson plans in the Plans panel.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
