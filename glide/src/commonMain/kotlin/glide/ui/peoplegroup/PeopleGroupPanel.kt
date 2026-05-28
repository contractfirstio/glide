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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.data.BillStore
import glide.data.BillingPanelState
import glide.data.ClientStore
import glide.data.ClientsPanelState
import glide.data.LocationStore
import glide.data.PlansPanelState
import glide.data.StudentsPanelState
import glide.data.StudentStore
import glide.data.SchedulePanelState
import glide.data.TermStore
import glide.data.SoldPlanEnrollmentStore
import glide.data.LeadNavigation
import glide.data.LeadStore
import glide.data.SoldPlanStore
import glide.data.findSoldPlanById
import glide.data.soldPlanDeletionBlockReason
import glide.data.PlanStore
import glide.data.ClassStore
import glide.debug.GlidePanelDebug
import glide.debug.PanelDebugStateEffect
import glide.model.formatMoney
import glide.data.classAttendeeCount
import glide.data.hasClassParticipant
import glide.data.hasResolvableMainClient
import glide.data.memberCount
import glide.data.resolveMainClient
import glide.data.resolveStudents
import glide.model.Lead
import glide.model.LeadStatus
import glide.model.SoldPlan
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
import glide.ui.shared.ListFormPanelLayout
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.IsoDateField
import glide.ui.shared.PanelListSearchField
import glide.ui.shared.PanelListSearchSpacer
import glide.ui.shared.matchesPanelListSearch
import glide.ui.shared.panelListCountLabel
import glide.ui.shared.FormValidationAnchor
import glide.ui.shared.FormValidationState
import glide.ui.shared.ValidatedIsoDateField
import glide.ui.shared.rememberFormDirtyTracker
import glide.ui.shared.rememberFormValidation
import glide.ui.theme.glideOutlinedFieldColors
import glide.ui.theme.GlideTextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class LeadsSoldPlansPanelUi(
    val isLeadPanel: Boolean,
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

private val LeadsPanelUi = LeadsSoldPlansPanelUi(
    isLeadPanel = true,
    subtitle = "Manage leads, link existing clients, and search to add students.",
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

private val SoldPlansPanelUi = LeadsSoldPlansPanelUi(
    isLeadPanel = false,
    subtitle = "Sold plans are locked after creation. Clone one to a new lead to build another plan.",
    emptyListMessage = "No sold plans yet. Mark a lead as sold.",
    newButtonLabel = "",
    createFormTitle = "Sold plan",
    editFormTitle = "Sold plan",
    listCountLabel = { count -> "$count sold plan${if (count == 1) "" else "s"}" },
    showPipelineStatus = false,
    showConvertToCustomer = false,
    showPlanPicker = false,
    allowCreate = false,
    readOnly = true,
    noSelectionMessage = "Select a sold plan to view.",
    deleteConfirmTitle = "Revert sold plan to lead?",
    deleteConfirmMessage = "Billing and class enrollment for this sold plan will be removed. " +
        "The household will become a lead again with the same client and plan selection.",
    showClearSelection = true,
)

@Composable
fun LeadsPanel(modifier: Modifier = Modifier) {
    PeopleGroupPanel(ui = LeadsPanelUi, modifier = modifier)
}

@Composable
fun SoldPlansPanel(modifier: Modifier = Modifier) {
    PeopleGroupPanel(ui = SoldPlansPanelUi, modifier = modifier)
}

private data class LeadFormState(
    val mainClientId: String? = null,
    val clientName: String = "",
    val dateOfBirth: String = "",
    val email: String = "",
    val phone: String = "",
    val studentIds: List<String> = emptyList(),
    val status: LeadStatus = LeadStatus.New,
    val planId: String? = null,
    val planStartDate: String = "",
    val mainClientAttendsClass: Boolean = true,
    val notes: String = "",
) {
    fun isValidForLead(): Boolean =
        (mainClientId != null && ClientStore.findById(mainClientId) != null) ||
            clientName.isNotBlank()

    fun hasPlanSelected(): Boolean = !planId.isNullOrBlank()

    fun hasClassParticipant(): Boolean {
        val mainClientAttends = mainClientId != null || clientName.isNotBlank()
        return (mainClientAttends && this.mainClientAttendsClass) || studentIds.isNotEmpty()
    }

    fun leadSaveValidationKeys(): List<String> = buildList {
        val linkedClientExists = mainClientId != null && ClientStore.findById(mainClientId) != null
        if (!linkedClientExists && clientName.isBlank()) {
            add("clientLink")
            add("clientName")
        }
    }

    fun soldValidationKeys(): List<String> = buildList {
        val linkedClientExists = mainClientId != null && ClientStore.findById(mainClientId) != null
        if (!linkedClientExists && clientName.isBlank()) {
            add("clientLink")
            add("clientName")
        }
        if (!linkedClientExists && email.isBlank()) add("clientEmail")
        if (!hasPlanSelected()) add("plan")
        if (planStartDate.isBlank() || parseIsoLocalDate(planStartDate) == null) add("planStartDate")
        if (!hasClassParticipant()) add("students")
    }

    fun toLead(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): Lead {
        val linkedClient = mainClientId != null
        return Lead(
            id = existingId ?: UUID.randomUUID().toString(),
            mainClientId = if (linkedClient) mainClientId else null,
            clientName = if (linkedClient) "" else clientName.trim(),
            dateOfBirth = if (linkedClient) "" else dateOfBirth.trim(),
            email = if (linkedClient) "" else email.trim(),
            phone = if (linkedClient) "" else phone.trim(),
            studentIds = studentIds,
            status = status,
            planId = planId,
            planStartDate = planStartDate.trim(),
            mainClientAttendsClass = mainClientAttendsClass,
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
            val group = groupId?.let { LeadStore.findById(it) }
            val main = group?.resolveMainClient()
            when {
                main == null || main.name.isBlank() ->
                    "Main client name is required before marking as sold."
                main.email.isBlank() || main.phone.isBlank() ->
                    "Email and phone are required before marking as sold."
                !hasPlanSelected -> "Select a plan above to enable Sold."
                group?.planStartDate.isNullOrBlank() ||
                    parseIsoLocalDate(group!!.planStartDate) == null ->
                    "Plan start date is required before marking as sold."
                group != null && !group.hasClassParticipant() ->
                    "At least one class participant is required before marking as sold."
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
    ui: LeadsSoldPlansPanelUi,
    modifier: Modifier = Modifier,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val form = rememberFormDirtyTracker(LeadFormState(planStartDate = todayIsoDate()))
    var isCreating by remember(ui.allowCreate) { mutableStateOf(ui.allowCreate) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    val formValidation = rememberFormValidation()
    val saveScope = rememberCoroutineScope()
    var cloneMessage by remember { mutableStateOf<String?>(null) }
    var listSearchQuery by remember { mutableStateOf("") }

    val clientFilterId = if (!ui.isLeadPanel) {
        ClientsPanelState.selectedClientId
    } else {
        null
    }
    val planFilterId = if (!ui.isLeadPanel) {
        PlansPanelState.selectedPlanId
    } else {
        null
    }
    val studentFilterId = if (!ui.isLeadPanel) {
        StudentsPanelState.selectedStudentId
    } else {
        null
    }
    val schedulingSoldPlansPanel =
        !ui.isLeadPanel && AppViewState.mode == AppViewMode.SCHEDULING
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
    val leads = if (ui.isLeadPanel) LeadStore.all else emptyList()
    val soldPlans = when {
        ui.isLeadPanel -> emptyList()
        schedulingSoldPlansPanel -> SoldPlanStore.forSchedulingSoldPlans(
            classId = classFilterId,
            termId = soldPlansTermFilterId,
            locationId = soldPlansLocationFilterId,
        )
        else -> SoldPlanStore.forSoldPlansPanel(clientFilterId, studentFilterId, planFilterId)
    }
    // Do not remember filtered lists: store lists keep the same reference when items are added.
    val filteredLeads = leads.filter { lead -> leadMatchesPanelSearch(lead, listSearchQuery) }
    val filteredSoldPlans = soldPlans.filter { group -> soldPlanMatchesPanelSearch(group, listSearchQuery) }
    val searchActive = listSearchQuery.isNotBlank()
    val groupCount = if (ui.isLeadPanel) leads.size else soldPlans.size
    val filteredGroupCount = if (ui.isLeadPanel) filteredLeads.size else filteredSoldPlans.size
    val clientFilterLabel = clientFilterId?.let { ClientStore.findById(it)?.name?.takeIf { it.isNotBlank() } }
    val planFilterLabel = planFilterId?.let { PlanStore.findById(it)?.name?.takeIf { it.isNotBlank() } }
    val studentFilterLabel = studentFilterId?.let {
        StudentStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val classFilterLabel = classFilterId?.let {
        ClassStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val termFilterLabel = soldPlansTermFilterId?.let {
        TermStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val locationFilterLabel = soldPlansLocationFilterId?.let {
        LocationStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val panelTag = if (ui.isLeadPanel) GlidePanelDebug.Panel.LEADS else GlidePanelDebug.Panel.SOLD_PLANS

    PanelDebugStateEffect(
        panelTag,
        selectedId,
        isCreating,
        clientFilterId,
        planFilterId,
        studentFilterId,
        soldPlanFilterId,
        classFilterId,
        termFilterId,
        locationFilterId,
        listSearchQuery,
        groupCount,
        filteredGroupCount,
    ) {
        "selected=$selectedId creating=$isCreating groups=$filteredGroupCount/$groupCount " +
            "search='$listSearchQuery' scheduling=$schedulingSoldPlansPanel || " +
            GlidePanelDebug.globalSnapshot()
    }

    fun resetFormForCreate() {
        selectedId = null
        isCreating = ui.allowCreate
        form.load(LeadFormState(planStartDate = todayIsoDate()))
        formError = null
        formValidation.clear()
    }

    fun clearLocalSelection() {
        selectedId = null
        isCreating = false
        form.load(LeadFormState(planStartDate = todayIsoDate()))
        formError = null
        formValidation.clear()
    }

    fun clearSelection() {
        clearLocalSelection()
        if (schedulingSoldPlansPanel) {
            SchedulePanelState.onSoldPlanCleared()
        } else if (!ui.isLeadPanel) {
            BillingPanelState.onSoldPlanCleared()
        }
    }

    fun loadSoldPlanIntoForm(group: SoldPlan) {
        selectedId = group.id
        isCreating = false
        formError = null
        formValidation.clear()
        if (schedulingSoldPlansPanel) {
            SchedulePanelState.onSoldPlanSelected(group.id)
        }
    }

    fun loadLeadIntoForm(group: Lead) {
        selectedId = group.id
        isCreating = false
        val main = group.resolveMainClient()
        form.load(
            LeadFormState(
                mainClientId = group.mainClientId,
                clientName = if (group.mainClientId != null) "" else main.name,
                dateOfBirth = if (group.mainClientId != null) "" else main.dateOfBirth,
                email = if (group.mainClientId != null) "" else main.email,
                phone = if (group.mainClientId != null) "" else main.phone,
                studentIds = group.studentIds,
                status = group.status,
                planId = group.planId,
                planStartDate = group.planStartDate.ifBlank { todayIsoDate() },
                mainClientAttendsClass = group.mainClientAttendsClass,
                notes = group.notes,
            ),
        )
        formError = null
        formValidation.clear()
    }

    val pendingLeadId = LeadNavigation.pendingLeadId
    LaunchedEffect(pendingLeadId, ui.isLeadPanel) {
        if (ui.isLeadPanel && pendingLeadId != null) {
            LeadStore.findById(pendingLeadId)?.let { loadLeadIntoForm(it) }
            LeadNavigation.clearPendingLead()
        }
    }

    val pendingSoldPlanId = LeadNavigation.pendingSoldPlanId
    LaunchedEffect(pendingSoldPlanId, ui.isLeadPanel) {
        if (!ui.isLeadPanel && pendingSoldPlanId != null) {
            findSoldPlanById(pendingSoldPlanId)?.let { group ->
                loadSoldPlanIntoForm(group)
                BillingPanelState.onSoldPlanSelected(group.id)
            }
            LeadNavigation.clearPendingSoldPlan()
        }
    }

    LaunchedEffect(BillingPanelState.soldPlanId) {
        if (ui.isLeadPanel || schedulingSoldPlansPanel) return@LaunchedEffect
        when (BillingPanelState.soldPlanId) {
            null -> if (selectedId != null) clearLocalSelection()
            else -> if (selectedId != null && selectedId != BillingPanelState.soldPlanId) {
                clearLocalSelection()
            }
        }
    }

    LaunchedEffect(clientFilterId, planFilterId, studentFilterId, classFilterId, soldPlanFilterId, soldPlansTermFilterId, soldPlansLocationFilterId) {
        if (!ui.isLeadPanel && soldPlanFilterId != null) return@LaunchedEffect
        if (!ui.isLeadPanel &&
            (clientFilterId != null || planFilterId != null || studentFilterId != null ||
                classFilterId != null || soldPlansTermFilterId != null || soldPlansLocationFilterId != null) &&
            selectedId != null
        ) {
            clearLocalSelection()
        }
    }

    LaunchedEffect(
        clientFilterId,
        planFilterId,
        studentFilterId,
        classFilterId,
        soldPlanFilterId,
        soldPlansTermFilterId,
        soldPlansLocationFilterId,
        soldPlans,
        leads,
        selectedId,
    ) {
        if (!ui.isLeadPanel &&
            selectedId != null &&
            soldPlans.none { it.id == selectedId }
        ) {
            clearSelection()
        }
        if (ui.isLeadPanel &&
            selectedId != null &&
            leads.none { it.id == selectedId }
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
                        !ui.isLeadPanel && planFilterId != null -> {
                            val label = planFilterLabel ?: "this plan"
                            "Showing sold plans on $label. Use Clear filter in Plans to reset."
                        }
                        !ui.isLeadPanel && studentFilterId != null -> {
                            val label = studentFilterLabel ?: "this student"
                            "Showing sold plans for $label. Use Clear filter in Students to reset."
                        }
                        !ui.isLeadPanel && clientFilterId != null -> {
                            val label = clientFilterLabel ?: "this client"
                            "Showing sold plans for $label. Use Clear filter to reset."
                        }
                        schedulingSoldPlansPanel ->
                            "Select a sold plan to see its students."
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
                            text = if (searchActive) {
                                panelListCountLabel(
                                    singular = if (ui.isLeadPanel) "lead" else "sold plan",
                                    plural = if (ui.isLeadPanel) "leads" else "sold plans",
                                    filteredCount = filteredGroupCount,
                                    totalCount = groupCount,
                                    searchActive = true,
                                )
                            } else {
                                ui.listCountLabel(groupCount)
                            },
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
                            if (!ui.isLeadPanel && planFilterId != null) {
                                GlideTextButton(onClick = { PlansPanelState.clearPlanFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (!ui.isLeadPanel && studentFilterId != null) {
                                GlideTextButton(onClick = { StudentsPanelState.clearStudentFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (!ui.isLeadPanel && clientFilterId != null) {
                                GlideTextButton(onClick = { ClientsPanelState.clearClientFilter() }) {
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
                    PanelListSearchSpacer()
                    PanelListSearchField(
                        query = listSearchQuery,
                        onQueryChange = { listSearchQuery = it },
                        placeholder = if (ui.isLeadPanel) "Name, email, plan…" else "Client, student, plan…",
                    )
                    Spacer(modifier = Modifier.height(spacing.field))

                    if (groupCount == 0) {
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
                                    !ui.isLeadPanel && planFilterId != null ->
                                        "No sold plans on this plan."
                                    !ui.isLeadPanel && studentFilterId != null ->
                                        "No sold plans for this student."
                                    !ui.isLeadPanel && clientFilterId != null ->
                                        "No sold plans for this client."
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
                    } else if (filteredGroupCount == 0) {
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
                                text = if (ui.isLeadPanel) {
                                    "No leads match your search."
                                } else {
                                    "No sold plans match your search."
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
                            if (ui.isLeadPanel) {
                                items(filteredLeads, key = { it.id }) { group ->
                                    LeadListItem(
                                        group = group,
                                        dateFormat = dateFormat,
                                        selected = group.id == selectedId,
                                        compact = compact,
                                        showPipelineStatus = ui.showPipelineStatus,
                                        onClick = {
                                            if (group.id == selectedId) {
                                                clearSelection()
                                            } else {
                                                loadLeadIntoForm(group)
                                            }
                                        },
                                    )
                                }
                            } else {
                                items(filteredSoldPlans, key = { it.id }) { group ->
                                    SoldPlanListItem(
                                        group = group,
                                        dateFormat = dateFormat,
                                        selected = group.id == selectedId,
                                        compact = compact,
                                        emphasizePlan = true,
                                        onClick = {
                                            if (group.id == selectedId) {
                                                clearSelection()
                                            } else {
                                                loadSoldPlanIntoForm(group)
                                                if (!schedulingSoldPlansPanel) {
                                                    BillingPanelState.onSoldPlanSelected(group.id)
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val formSection: @Composable (Modifier) -> Unit = { formModifier ->
                val showForm = selectedId != null || (ui.allowCreate && isCreating)
                val selectedGroup = selectedId?.let { findSoldPlanById(it) }

                Column(modifier = formModifier.fillMaxHeight()) {
                    Text(
                        text = when {
                            !showForm -> if (schedulingSoldPlansPanel) {
                                "Students"
                            } else {
                                ui.editFormTitle
                            }
                            isCreating -> ui.createFormTitle
                            schedulingSoldPlansPanel && selectedGroup != null -> "Students"
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
                                    "Select a sold plan to see its students."
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
                                val group = findSoldPlanById(selectedId!!)
                                if (group != null) {
                                    if (schedulingSoldPlansPanel) {
                                        SchedulingSoldPlanDetailView(
                                            group = group,
                                            spacing = spacing,
                                        )
                                    } else {
                                        SoldPlanDetailView(
                                            group = group,
                                            spacing = spacing,
                                            onOpenBilling = {
                                                BillingPanelState.reopenForCurrentSoldPlan()
                                            },
                                            onDelete = { showDeleteConfirm = true },
                                            onCloneToLead = {
                                                cloneMessage = null
                                                val lead = SoldPlanStore.cloneToLead(group.id)
                                                if (lead != null) {
                                                    LeadNavigation.openLead(lead.id)
                                                    cloneMessage =
                                                        "Lead created from this sold plan. Open the Leads panel to edit and convert."
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
                                LeadForm(
                                    state = form.draft,
                                    onStateChange = { form.draft = it },
                                    spacing = spacing,
                                    notesHeight = notesHeight,
                                    isSoldPlan = !ui.isLeadPanel,
                                    showPipelineStatus = ui.showPipelineStatus,
                                    showPlanPicker = ui.showPlanPicker,
                                    validation = formValidation,
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
                                        if (ui.isLeadPanel) {
                                            val invalidKeys = form.draft.leadSaveValidationKeys()
                                            if (invalidKeys.isNotEmpty()) {
                                                formValidation.reportInvalid(invalidKeys, saveScope)
                                                formError = "Link a client or enter a new client name."
                                                return@GlideButton
                                            }
                                        }
                                        formValidation.clear()
                                        if (isCreating && ui.allowCreate) {
                                            val group = form.draft.toLead()
                                            LeadStore.create(group)
                                            loadLeadIntoForm(group)
                                        } else {
                                            val existing = selectedId?.let { LeadStore.findById(it) }
                                            if (existing != null) {
                                                val updated = form.draft.toLead(
                                                    existingId = existing.id,
                                                    createdAtMillis = existing.createdAtMillis,
                                                )
                                                if (!LeadStore.update(updated)) {
                                                    formError = "This record cannot be changed."
                                                    return@GlideButton
                                                }
                                                loadLeadIntoForm(updated)
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
                                            formError = null
                                            if (form.isDirty) {
                                                formError = "Save changes before marking as sold."
                                                return@GlideOutlinedButton
                                            }
                                            val soldInvalidKeys = form.draft.soldValidationKeys()
                                            if (soldInvalidKeys.isNotEmpty()) {
                                                formValidation.reportInvalid(soldInvalidKeys, saveScope)
                                                formError = when {
                                                    "clientName" in soldInvalidKeys || "clientLink" in soldInvalidKeys ->
                                                        "Main client name is required."
                                                    "clientEmail" in soldInvalidKeys ->
                                                        "Email is required before marking as sold."
                                                    "plan" in soldInvalidKeys ->
                                                        "Select a plan before marking as sold."
                                                    "planStartDate" in soldInvalidKeys ->
                                                        "Plan start date is required before marking as sold."
                                                    "students" in soldInvalidKeys ->
                                                        "At least one class participant is required before marking as sold."
                                                    else ->
                                                        "Complete all required fields before marking as sold."
                                                }
                                                return@GlideOutlinedButton
                                            }
                                            val existing = LeadStore.findById(selectedId!!)
                                                ?: return@GlideOutlinedButton
                                            val updated = form.draft.toLead(
                                                existingId = existing.id,
                                                createdAtMillis = existing.createdAtMillis,
                                            )
                                            LeadStore.update(updated)
                                            if (!LeadStore.convertToSoldPlan(selectedId!!)) {
                                                formError =
                                                    "Could not mark as sold. Check plan, start date, and class participants."
                                                return@GlideOutlinedButton
                                            }
                                            formError = null
                                            formValidation.clear()
                                            resetFormForCreate()
                                        },
                                        enabled = true,
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

            ListFormPanelLayout(
                hasSelection = selectedId != null || (ui.allowCreate && isCreating),
                spacing = spacing,
                onCloseForm = { clearSelection() },
                modifier = Modifier.fillMaxSize(),
                listSection = listSection,
                formSection = formSection,
            )
        }
    }

    if (showDeleteConfirm && selectedId != null && (!ui.isLeadPanel || !ui.readOnly)) {
        val soldPlanBlockReason = if (!ui.isLeadPanel) {
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
                val deleted = if (ui.isLeadPanel) {
                    LeadStore.delete(groupId)
                } else {
                    SoldPlanStore.delete(groupId)
                }
                if (deleted) {
                    showDeleteConfirm = false
                    when {
                        !ui.isLeadPanel -> {
                            LeadNavigation.openLead(groupId)
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
            blockedReason = soldPlanBlockReason,
        )
    }
}

@Composable
private fun LeadListItem(
    group: Lead,
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
        val main = group.resolveMainClient()
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
        group.studentsSummary(compact)?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        planLabelForLead(group)?.let { planLabel ->
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
            }
            Text(
                text = dateFormat.format(Date(group.createdAtMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SoldPlanListItem(
    group: SoldPlan,
    dateFormat: SimpleDateFormat,
    selected: Boolean,
    compact: Boolean,
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
        val main = group.resolveMainClient()
        Text(
            text = main?.name?.ifBlank { "Unknown client" } ?: "Unknown client",
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
    }
}

private fun leadMatchesPanelSearch(lead: Lead, query: String): Boolean {
    val main = lead.resolveMainClient()
    val studentNames = lead.resolveStudents().joinToString(" ") { it.name }
    val planName = lead.planId?.let { PlanStore.findById(it)?.name }.orEmpty()
    return matchesPanelListSearch(
        query,
        formatPersonLabel(main.name, main.dateOfBirth),
        main.email,
        main.phone,
        lead.notes,
        studentNames,
        planName,
        lead.status.label,
    )
}

private fun soldPlanMatchesPanelSearch(group: SoldPlan, query: String): Boolean {
    val main = group.resolveMainClient()
    val studentNames = group.resolveStudents().joinToString(" ") { it.name }
    val planName = group.planId?.let { PlanStore.findById(it)?.name }.orEmpty()
    return matchesPanelListSearch(
        query,
        main?.name.orEmpty(),
        studentNames,
        planName,
        planNameForGroup(group),
        planStartDateLabelForGroup(group),
    )
}

private fun planLabelForLead(group: Lead): String? {
    val planId = group.planId ?: return null
    val plan = PlanStore.findById(planId) ?: return null
    return "Plan: ${plan.name}"
}

private fun Lead.studentsSummary(compact: Boolean): String? {
    val students = resolveStudents()
    if (students.isEmpty()) return null
    val labels = students.joinToString { formatPersonLabel(it.name, it.dateOfBirth) }
    return if (compact) labels else "Students: $labels"
}

private fun planNameForGroup(group: SoldPlan): String =
    group.planId
        ?.let { PlanStore.findById(it)?.name?.takeIf { name -> name.isNotBlank() } }
        ?: "No plan"

private fun planStartDateLabelForGroup(group: SoldPlan): String {
    val fromGroup = group.planStartDate.takeIf { it.isNotBlank() }
        ?.let { formatIsoDateForDisplay(it) }
        ?.takeIf { it.isNotBlank() }
    if (fromGroup != null) return fromGroup

    return SoldPlanEnrollmentStore.forSoldPlan(group.id)
        ?.startedAtMillis
        ?.let { formatIsoDateForDisplay(millisToIsoDate(it)) }
        ?.takeIf { it.isNotBlank() }
        ?: "No start date"
}

private fun planLabelForGroup(group: SoldPlan): String? {
    val planId = group.planId ?: return null
    val plan = PlanStore.findById(planId) ?: return null
    return "Plan: ${plan.name}"
}

private fun SoldPlan.studentsSummary(compact: Boolean): String? {
    val students = resolveStudents()
    if (students.isEmpty()) return null
    val labels = students.joinToString { formatPersonLabel(it.name, it.dateOfBirth) }
    return if (compact) labels else "Students: $labels"
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
    group: SoldPlan,
    spacing: GlideLayout.Spacing,
) {
    FormPanelSection(
        title = "Students",
        description = "Students on this sold plan. The client is highlighted when they attend class too.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        SchedulingSoldPlanStudentsSection(group = group, spacing = spacing)
    }
}

@Composable
private fun SchedulingSoldPlanStudentsSection(
    group: SoldPlan,
    spacing: GlideLayout.Spacing,
) {
    val clientAttends = group.mainClientAttendsClass && group.hasResolvableMainClient()
    if (clientAttends) {
        val main = group.resolveMainClient()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(
                    horizontal = spacing.listItemHorizontal,
                    vertical = spacing.listItemVertical,
                ),
        ) {
            Text(
                text = "Client",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatPersonLabel(main?.name.orEmpty(), main?.dateOfBirth.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Also attends class",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(spacing.field))
    }
    ReadOnlyStudentsSection(studentIds = group.studentIds, showLabel = false)
}

@Composable
private fun SoldPlanDetailView(
    group: SoldPlan,
    spacing: GlideLayout.Spacing,
    onOpenBilling: () -> Unit,
    onDelete: () -> Unit,
    onCloneToLead: () -> Unit,
) {
    val enrollment = SoldPlanEnrollmentStore.forSoldPlan(group.id)
    val outstanding = enrollment?.let { BillStore.outstandingMinorForEnrollment(it.id) } ?: 0L

    FormPanelSection(
        title = "Plan",
        description = "The plan assigned to this sold plan.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        ReadOnlyPlanSection(planId = group.planId, prominent = false)
    }

    FormPanelSectionsDivider(label = "Clients on this plan", spacing = spacing)

    FormPanelSection(
        title = "Main client",
        description = "The primary person for billing and household identity.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        ReadOnlyMainClientSection(clientId = group.mainClientId, showLabel = false)
        Spacer(modifier = Modifier.height(spacing.field))
        Text(
            text = if (group.mainClientAttendsClass) {
                "Main client attends class"
            } else {
                "Main client does not attend class"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Students always attend. Set on the lead before conversion.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }

    FormPanelSection(
        title = "Students",
        description = "Others on this plan besides the main client.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        ReadOnlyStudentsSection(studentIds = group.studentIds, showLabel = false)
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
            text = "This sold plan is locked. Clone to a lead to create another plan with the same people.",
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
private fun LeadMainClientAttendsField(
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
                text = "Main client attends class",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (attends) {
                    "Main client counts toward room capacity when this group is on a class."
                } else {
                    "Only students attend; main client is not counted on classes."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeadForm(
    state: LeadFormState,
    onStateChange: (LeadFormState) -> Unit,
    spacing: GlideLayout.Spacing,
    notesHeight: Dp,
    isSoldPlan: Boolean,
    showPipelineStatus: Boolean,
    showPlanPicker: Boolean,
    validation: FormValidationState,
) {
    var statusExpanded by remember { mutableStateOf(false) }

    if (isSoldPlan) {
        ReadOnlyMainClientSection(clientId = state.mainClientId)
    } else {
        LeadMainClientSection(
            mainClientId = state.mainClientId,
            clientName = state.clientName,
            dateOfBirth = state.dateOfBirth,
            email = state.email,
            phone = state.phone,
            onStateChange = { clientId, name, dob, email, phone ->
                onStateChange(
                    state.copy(
                        mainClientId = clientId,
                        clientName = name,
                        dateOfBirth = dob,
                        email = email,
                        phone = phone,
                    ),
                )
            },
            spacing = spacing,
            validation = validation,
        )
    }
    if (!isSoldPlan) {
        LeadPeopleSectionsDivider(spacing = spacing)
    } else {
        Spacer(modifier = Modifier.height(spacing.section * 2))
    }

    if (isSoldPlan) {
        StudentLinkSection(
            selectedIds = state.studentIds,
            onSelectionChange = { onStateChange(state.copy(studentIds = it)) },
            spacing = spacing,
        )
    } else {
        LeadStudentsSection(
            selectedIds = state.studentIds,
            onSelectionChange = { onStateChange(state.copy(studentIds = it)) },
            spacing = spacing,
            validation = validation,
        )
    }

    if (!isSoldPlan && (showPlanPicker || showPipelineStatus)) {
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
                onPlanSelected = {
                    validation.clearKey("plan")
                    onStateChange(state.copy(planId = it))
                },
                validation = validation,
            )
            if (!isSoldPlan) {
                Spacer(modifier = Modifier.height(spacing.field))
                LeadMainClientAttendsField(
                    attends = state.mainClientAttendsClass,
                    onAttendsChange = { onStateChange(state.copy(mainClientAttendsClass = it)) },
                )
            }
        }
    } else if (!isSoldPlan) {
        Spacer(modifier = Modifier.height(spacing.field))
        LeadMainClientAttendsField(
            attends = state.mainClientAttendsClass,
            onAttendsChange = { onStateChange(state.copy(mainClientAttendsClass = it)) },
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
                        LeadStatus.entries.forEach { status ->
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
            validation.ValidatedIsoDateField(
                fieldKey = "planStartDate",
                label = "Start date",
                value = state.planStartDate,
                onValueChange = { onStateChange(state.copy(planStartDate = it)) },
                required = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanDropdown(
    selectedPlanId: String?,
    onPlanSelected: (String) -> Unit,
    validation: FormValidationState,
) {
    val plans = PlanStore.plans
    var expanded by remember { mutableStateOf(false) }
    val selectedPlan = plans.find { it.id == selectedPlanId }
    val displayValue = selectedPlan?.let { "${it.name} (${it.summaryLine()})" }
        ?: if (plans.isEmpty()) "No plans available" else "Select a plan"

    FormValidationAnchor(validation = validation, fieldKey = "plan") {
    Column {
        GlideFieldLabel("Plan")
        Spacer(modifier = Modifier.height(2.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                enabled = true,
                isError = validation.isInvalid("plan"),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                colors = glideOutlinedFieldColors(isError = validation.isInvalid("plan")),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = GlideDimensions.fieldHeight)
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                if (plans.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "No plans yet — create one in the Plans panel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { expanded = false },
                        enabled = false,
                    )
                } else {
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
        }
        if (plans.isEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create a plan in the Plans panel, then return here to select it.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    }
}
