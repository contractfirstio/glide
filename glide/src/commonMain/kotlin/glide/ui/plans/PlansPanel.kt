package glide.ui.plans

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import glide.data.BillingPanelState
import glide.data.ClientStore
import glide.data.ClientsPanelState
import glide.data.SoldPlanStore
import glide.data.PlanStore
import glide.debug.GlidePanelDebug
import glide.debug.PlansPanelDebug
import glide.data.PlansPanelState
import glide.data.StudentsPanelState
import glide.data.StudentStore
import glide.data.resolveMainClient
import glide.data.findSoldPlanById
import glide.model.Plan
import glide.model.PlanKind
import glide.model.formatMoney
import glide.model.majorToMinor
import glide.model.parseMajorAmount
import glide.model.summaryLine
import glide.model.DEFAULT_CURRENCY_CODE
import glide.ui.layout.GlideLayout
import glide.ui.shared.DeleteConfirmDialog
import glide.ui.shared.DeleteActionButton
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.ListFormPanelLayout
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.PanelListSearchField
import glide.ui.shared.PanelListSearchSpacer
import glide.ui.shared.matchesPanelListSearch
import glide.ui.shared.panelListCountLabel
import glide.ui.shared.FormValidationState
import glide.ui.shared.ValidatedGlideOutlinedField
import glide.ui.shared.rememberFormDirtyTracker
import glide.ui.shared.rememberFormValidation
import glide.ui.theme.glideOutlinedFieldColors
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.glideListItemTitleColor
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton
import java.util.UUID

private data class PlanFormState(
    val name: String = "",
    val kind: PlanKind = PlanKind.MULTI_LESSON_PLAN,
    val lessonCount: String = "",
    val rolling: Boolean = true,
    val priceMajor: String = "",
    val notes: String = "",
) {
    fun isValid(): Boolean {
        if (name.isBlank()) return false
        if (parseMajorAmount(priceMajor) == null) return false
        return when (kind) {
            PlanKind.SINGLE_LESSON_PLAN -> true
            PlanKind.MULTI_LESSON_PLAN -> lessonCount.toIntOrNull()?.let { it > 0 } == true
            PlanKind.CAMP -> lessonCount.toIntOrNull()?.let { it > 0 } == true
        }
    }

    fun validationFieldKeys(): List<String> = buildList {
        if (name.isBlank()) add("name")
        if (parseMajorAmount(priceMajor) == null) add("priceMajor")
        when (kind) {
            PlanKind.MULTI_LESSON_PLAN, PlanKind.CAMP ->
                if (lessonCount.toIntOrNull()?.let { it > 0 } != true) add("lessonCount")
            PlanKind.SINGLE_LESSON_PLAN -> Unit
        }
    }

    fun toPlan(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
        existingCurrency: String = DEFAULT_CURRENCY_CODE,
    ): Plan? {
        val count = when (kind) {
            PlanKind.SINGLE_LESSON_PLAN -> 1
            PlanKind.MULTI_LESSON_PLAN -> lessonCount.toIntOrNull() ?: return null
            PlanKind.CAMP -> lessonCount.toIntOrNull() ?: return null
        }
        val priceMajorAmount = parseMajorAmount(priceMajor) ?: return null
        return Plan(
            id = existingId ?: UUID.randomUUID().toString(),
            kind = kind,
            name = name.trim(),
            lessonCount = count,
            rolling = kind == PlanKind.MULTI_LESSON_PLAN && rolling,
            priceAmountMinor = majorToMinor(priceMajorAmount),
            currencyCode = existingCurrency,
            notes = notes.trim(),
            createdAtMillis = createdAtMillis,
        )
    }
}

private fun Plan.priceMajorString(): String {
    val major = priceAmountMinor / 100
    val minor = priceAmountMinor % 100
    return if (minor == 0L) major.toString() else "$major.${minor.toString().padStart(2, '0')}"
}

@Composable
fun PlansPanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val form = rememberFormDirtyTracker(PlanFormState())
    var isCreating by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    val formValidation = rememberFormValidation()
    val saveScope = rememberCoroutineScope()
    var listSearchQuery by remember { mutableStateOf("") }
    var filterClearedMessage by remember { mutableStateOf<String?>(null) }

    val soldPlanId = BillingPanelState.soldPlanId
    val clientFilterId = ClientsPanelState.selectedClientId
    val studentFilterId = StudentsPanelState.selectedStudentId
    val outboundPlanFilterId = PlansPanelState.selectedPlanId
    val plans = PlanStore.forPlansPanel(soldPlanId, clientFilterId, studentFilterId)
    val filteredPlans = plans.filter { plan ->
        matchesPanelListSearch(
            listSearchQuery,
            plan.name,
            plan.kind.label,
            plan.summaryLine(),
            plan.notes,
        )
    }
    val searchActive = listSearchQuery.isNotBlank()
    val outboundPlanFilterLabel = outboundPlanFilterId?.let {
        PlanStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val soldPlanLabel = soldPlanId?.let { id ->
        findSoldPlanById(id)?.resolveMainClient()?.name?.takeIf { it.isNotBlank() }
    }
    val clientFilterLabel = clientFilterId?.let { ClientStore.findById(it)?.name?.takeIf { it.isNotBlank() } }
    val studentFilterLabel = studentFilterId?.let {
        StudentStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val hasInboundFilter = soldPlanId != null ||
        clientFilterId != null ||
        studentFilterId != null

    fun stateSnapshot(): String =
        "selected=$selectedId isCreating=$isCreating soldPlan=$soldPlanId client=$clientFilterId " +
            "student=$studentFilterId outbound=$outboundPlanFilterId search='$listSearchQuery' " +
            "visible=${plans.size}/${PlanStore.plans.size} visibleIds=${plans.map { it.id }}"

    fun clearLocalSelection(caller: String) {
        PlansPanelDebug.log("clearLocalSelection", "caller=$caller | ${stateSnapshot()}")
        selectedId = null
        isCreating = false
        form.load(PlanFormState())
        formError = null
        formValidation.clear()
    }

    fun clearSelection(caller: String) {
        PlansPanelDebug.log("clearSelection", "caller=$caller | ${stateSnapshot()}")
        clearLocalSelection("clearSelection->$caller")
        PlansPanelState.clearPlanFilter()
        filterClearedMessage = null
    }

    fun clearInboundFiltersForPlanCreation(caller: String): Boolean {
        val hadFilter = soldPlanId != null ||
            clientFilterId != null ||
            studentFilterId != null ||
            outboundPlanFilterId != null ||
            listSearchQuery.isNotBlank()
        PlansPanelDebug.log(
            "clearInboundFilters",
            "caller=$caller hadFilter=$hadFilter | ${stateSnapshot()}",
        )
        if (soldPlanId != null) {
            BillingPanelState.onSoldPlanCleared()
        }
        if (clientFilterId != null) {
            ClientsPanelState.clearClientFilter()
        }
        if (studentFilterId != null) {
            StudentsPanelState.clearStudentFilter()
        }
        if (outboundPlanFilterId != null) {
            PlansPanelState.clearPlanFilter()
        }
        if (listSearchQuery.isNotBlank()) {
            listSearchQuery = ""
        }
        return hadFilter
    }

    fun resetFormForCreate() {
        val hadFilter = clearInboundFiltersForPlanCreation("resetFormForCreate")
        clearLocalSelection("resetFormForCreate")
        isCreating = true
        form.load(PlanFormState())
        formError = null
        formValidation.clear()
        filterClearedMessage = if (hadFilter) {
            "Filters cleared — showing all plans so you can create a new one."
        } else {
            null
        }
        PlansPanelDebug.log("resetFormForCreate", "after | ${stateSnapshot()}")
    }

    fun loadIntoForm(plan: Plan, applyOutboundFilter: Boolean = true) {
        PlansPanelDebug.log(
            "loadIntoForm",
            "id=${plan.id} name=${plan.name} applyOutboundFilter=$applyOutboundFilter | ${stateSnapshot()}",
        )
        if (applyOutboundFilter) {
            PlansPanelState.onPlanSelected(plan.id)
        }
        selectedId = plan.id
        isCreating = false
        form.load(
            PlanFormState(
                name = plan.name,
                kind = plan.kind,
                lessonCount = plan.lessonCount.toString(),
                rolling = plan.rolling,
                priceMajor = plan.priceMajorString(),
                notes = plan.notes,
            ),
        )
        formError = null
        formValidation.clear()
        PlansPanelDebug.log("loadIntoForm", "after | ${stateSnapshot()}")
    }

    fun commitCreatedPlan(plan: Plan) {
        PlansPanelDebug.log(
            "commitCreatedPlan",
            "before id=${plan.id} name=${plan.name} | ${stateSnapshot()}",
        )
        clearInboundFiltersForPlanCreation("commitCreatedPlan")
        selectedId = plan.id
        isCreating = false
        form.load(
            PlanFormState(
                name = plan.name,
                kind = plan.kind,
                lessonCount = plan.lessonCount.toString(),
                rolling = plan.rolling,
                priceMajor = plan.priceMajorString(),
                notes = plan.notes,
            ),
        )
        formError = null
        formValidation.clear()
        filterClearedMessage = null
        PlansPanelDebug.log("commitCreatedPlan", "after | ${stateSnapshot()}")
    }

    LaunchedEffect(
        soldPlanId,
        clientFilterId,
        studentFilterId,
        outboundPlanFilterId,
        selectedId,
        isCreating,
        plans.size,
        PlanStore.plans.size,
        listSearchQuery,
    ) {
        PlansPanelDebug.log(
            "state",
            "${stateSnapshot()} || ${GlidePanelDebug.globalSnapshot()}",
        )
    }

    LaunchedEffect(soldPlanId, clientFilterId, studentFilterId) {
        if (!hasInboundFilter || isCreating) return@LaunchedEffect
        val id = selectedId ?: return@LaunchedEffect
        if (plans.any { it.id == id }) return@LaunchedEffect
        PlansPanelDebug.log(
            "effect:inboundFilter",
            "clearing local selection — selected plan not in filtered list | id=$id ${stateSnapshot()}",
        )
        clearLocalSelection("effect:inboundFilter")
    }

    LaunchedEffect(plans, selectedId, isCreating) {
        if (isCreating) return@LaunchedEffect
        val id = selectedId ?: return@LaunchedEffect
        if (plans.any { it.id == id }) return@LaunchedEffect
        if (PlanStore.findById(id) != null) {
            PlansPanelDebug.log(
                "effect:plans",
                "plan exists in store but not visible — clearing inbound filters | id=$id ${stateSnapshot()}",
            )
            clearInboundFiltersForPlanCreation("effect:plans")
            return@LaunchedEffect
        }
        PlansPanelDebug.log(
            "effect:plans",
            "clearing selection — plan not in store | id=$id ${stateSnapshot()}",
        )
        clearSelection("effect:plans")
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < GlideLayout.CompactWidthBreakpoint
        val spacing = if (compact) GlideLayout.compact else GlideLayout.comfortable
        val listSectionHeight = if (compact) 112.dp else null
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
                        soldPlanId != null -> {
                            val label = soldPlanLabel ?: "this sold plan"
                            "Showing plans for $label. Use Show all in Customers to reset."
                        }
                        clientFilterId != null -> {
                            val label = clientFilterLabel ?: "this client"
                            "Showing plans for $label. Use Clear filter in Clients to reset."
                        }
                        studentFilterId != null -> {
                            val label = studentFilterLabel ?: "this student"
                            "Showing plans for $label. Use Clear filter in Students to reset."
                        }
                        outboundPlanFilterId != null -> {
                            val label = outboundPlanFilterLabel ?: "this plan"
                            "Filtering sold plans, clients, and students for $label. Use Clear filter to reset."
                        }
                        else -> "Define plan types and plans offered to customers."
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
                            text = panelListCountLabel(
                                singular = "plan",
                                plural = "plans",
                                filteredCount = filteredPlans.size,
                                totalCount = plans.size,
                                searchActive = searchActive,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.field)) {
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
                            if (outboundPlanFilterId != null) {
                                GlideTextButton(onClick = { PlansPanelState.clearPlanFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            GlideButton(onClick = { resetFormForCreate() }) {
                                Text(if (compact) "New" else "New plan")
                            }
                        }
                    }
                    PanelListSearchSpacer()
                    PanelListSearchField(
                        query = listSearchQuery,
                        onQueryChange = { listSearchQuery = it },
                        placeholder = "Plan name, type…",
                    )
                    Spacer(modifier = Modifier.height(spacing.field))

                    if (plans.isEmpty()) {
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
                                        "No plan on this sold plan."
                                    clientFilterId != null ->
                                        "No plans linked to this client."
                                    studentFilterId != null ->
                                        "No plans linked to this student."
                                    else -> "No plans yet."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (filteredPlans.isEmpty()) {
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
                                text = "No plans match your search.",
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
                            items(filteredPlans, key = { it.id }) { plan ->
                                PlanListItem(
                                    plan = plan,
                                    selected = plan.id == selectedId,
                                    compact = compact,
                                    onClick = { loadIntoForm(plan) },
                                )
                            }
                        }
                    }
                }
            }

            val formSection: @Composable (Modifier) -> Unit = { formModifier ->
                Column(modifier = formModifier.fillMaxHeight()) {
                    Text(
                        text = if (isCreating) "Create plan" else "Edit plan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(spacing.section))

                    val planReadOnly = !isCreating &&
                        selectedId != null &&
                        !PlanStore.canEdit(selectedId!!)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        PlanForm(
                            state = form.draft,
                            onStateChange = { form.draft = it },
                            spacing = spacing,
                            readOnly = planReadOnly,
                            validation = formValidation,
                        )

                        if (planReadOnly) {
                            Spacer(modifier = Modifier.height(spacing.field))
                            Text(
                                text = PlanStore.planEditBlockReason(selectedId!!)
                                    ?: "This plan has been sold and cannot be edited.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        filterClearedMessage?.let { message ->
                            Spacer(modifier = Modifier.height(spacing.field))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
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
                                val invalidKeys = form.draft.validationFieldKeys()
                                if (invalidKeys.isNotEmpty()) {
                                    formValidation.reportInvalid(invalidKeys, saveScope)
                                    formError = when (form.draft.kind) {
                                        PlanKind.SINGLE_LESSON_PLAN -> "Name and a valid price per person are required."
                                        PlanKind.MULTI_LESSON_PLAN -> "Name, class count, and a valid price per person are required."
                                        PlanKind.CAMP -> "Name, day count, and a valid price per person are required."
                                    }
                                    return@GlideButton
                                }
                                val plan = form.draft.toPlan()
                                if (plan == null) {
                                    formValidation.reportInvalid(listOf("lessonCount"), saveScope)
                                    formError = "Count must be a positive number."
                                    return@GlideButton
                                }
                                formError = null
                                formValidation.clear()
                                if (isCreating) {
                                    PlansPanelDebug.log(
                                        "save",
                                        "create id=${plan.id} name=${plan.name} | ${stateSnapshot()}",
                                    )
                                    PlanStore.create(plan)
                                    commitCreatedPlan(plan)
                                } else {
                                    val existing = selectedId?.let { PlanStore.findById(it) }
                                    if (existing != null) {
                                        PlanStore.planEditBlockReason(existing.id)?.let { reason ->
                                            formError = reason
                                            return@GlideButton
                                        }
                                        val updated = form.draft.toPlan(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                            existingCurrency = existing.currencyCode,
                                        )
                                        if (updated != null) {
                                            if (PlanStore.update(updated)) {
                                                loadIntoForm(updated)
                                            } else {
                                                formError = PlanStore.planEditBlockReason(existing.id)
                                                    ?: "This plan has been sold and cannot be edited."
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = form.isDirty && !planReadOnly,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(saveLabel)
                        }

                        if (!isCreating) {
                            DeleteActionButton(
                                onClick = { showDeleteConfirm = true },
                                enabled = selectedId?.let { PlanStore.canDelete(it) } == true,
                                blockedReason = selectedId?.let { PlanStore.planDeletionBlockReason(it) },
                            )
                        }
                    }
                }
            }

            ListFormPanelLayout(
                hasSelection = selectedId != null || isCreating,
                spacing = spacing,
                onCloseForm = { clearSelection("onCloseForm") },
                modifier = Modifier.fillMaxSize(),
                listSection = listSection,
                formSection = formSection,
            )
        }
    }

    if (showDeleteConfirm && selectedId != null) {
        val blockReason = PlanStore.planDeletionBlockReason(selectedId!!)
        DeleteConfirmDialog(
            title = "Delete plan?",
            message = blockReason ?: "This plan will be removed permanently.",
            onDismiss = { showDeleteConfirm = false },
            onContinue = {
                if (PlanStore.delete(selectedId!!)) {
                    showDeleteConfirm = false
                    clearSelection("deletePlan")
                    isCreating = true
                    form.load(PlanFormState())
                } else {
                    showDeleteConfirm = false
                    formError = blockReason ?: "This plan has been sold and cannot be deleted."
                }
            },
            continueEnabled = blockReason == null,
            blockedReason = blockReason,
        )
    }
}

@Composable
private fun PlanListItem(
    plan: Plan,
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
            text = plan.name,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = glideListItemTitleColor(selected),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = plan.kind.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = plan.summaryLine(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${formatMoney(plan.priceAmountMinor, plan.currencyCode)} per person",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanForm(
    state: PlanFormState,
    onStateChange: (PlanFormState) -> Unit,
    spacing: GlideLayout.Spacing,
    readOnly: Boolean = false,
    validation: FormValidationState,
) {
    var kindExpanded by remember { mutableStateOf(false) }

    FormPanelSection(
        title = "Plan identity",
        description = "Name and type of plan you offer to customers.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        validation.ValidatedGlideOutlinedField(
            fieldKey = "name",
            value = state.name,
            onValueChange = { onStateChange(state.copy(name = it)) },
            label = "Plan name",
            placeholder = "e.g. 10 Class Rolling Plan",
            readOnly = readOnly,
            required = true,
        )
        Spacer(modifier = Modifier.height(spacing.field))

        Column {
            GlideFieldLabel("Plan type")
            Spacer(modifier = Modifier.height(2.dp))
            ExposedDropdownMenuBox(
                expanded = kindExpanded,
                onExpandedChange = { if (!readOnly) kindExpanded = it },
            ) {
                OutlinedTextField(
                    value = state.kind.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    colors = glideOutlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = GlideDimensions.fieldHeight)
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = kindExpanded,
                    onDismissRequest = { kindExpanded = false },
                ) {
                    PlanKind.entries.forEach { kind ->
                        DropdownMenuItem(
                            text = { Text(kind.label, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onStateChange(
                                    when (kind) {
                                        PlanKind.SINGLE_LESSON_PLAN -> state.copy(
                                            kind = kind,
                                            lessonCount = "1",
                                            rolling = false,
                                        )
                                        PlanKind.MULTI_LESSON_PLAN -> state.copy(
                                            kind = kind,
                                            lessonCount = if (state.kind == PlanKind.SINGLE_LESSON_PLAN) "" else state.lessonCount,
                                        )
                                        PlanKind.CAMP -> state.copy(
                                            kind = kind,
                                            rolling = false,
                                            lessonCount = if (state.kind == PlanKind.SINGLE_LESSON_PLAN) "" else state.lessonCount,
                                        )
                                    },
                                )
                                kindExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }

    FormPanelSectionsDivider(
        label = when (state.kind) {
            PlanKind.CAMP -> "Camp configuration"
            else -> "Plan configuration"
        },
        spacing = spacing,
    )

    FormPanelSection(
        title = when (state.kind) {
            PlanKind.CAMP -> "Camp duration"
            else -> "Classes in plan"
        },
        description = when (state.kind) {
            PlanKind.CAMP -> "How many days the camp runs for. Camps are always fixed and never roll over."
            else -> "How many lessons are included and whether they roll over."
        },
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        when (state.kind) {
            PlanKind.MULTI_LESSON_PLAN -> {
                validation.ValidatedGlideOutlinedField(
                    fieldKey = "lessonCount",
                    value = state.lessonCount,
                    onValueChange = { onStateChange(state.copy(lessonCount = it.filter { c -> c.isDigit() })) },
                    label = "Number of classes",
                    placeholder = "10",
                    readOnly = readOnly,
                    required = true,
                )
                Spacer(modifier = Modifier.height(spacing.field))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = state.rolling,
                        onCheckedChange = { onStateChange(state.copy(rolling = it)) },
                        enabled = !readOnly,
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "Rolling plan",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = "Classes roll over within the plan (e.g. 10-class rolling plan).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            PlanKind.CAMP -> {
                validation.ValidatedGlideOutlinedField(
                    fieldKey = "lessonCount",
                    value = state.lessonCount,
                    onValueChange = { onStateChange(state.copy(lessonCount = it.filter { c -> c.isDigit() })) },
                    label = "Number of days",
                    placeholder = "5",
                    readOnly = readOnly,
                    required = true,
                )
            }
            PlanKind.SINGLE_LESSON_PLAN -> {
                Text(
                    text = "Number of classes",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "1 class (fixed for this plan type)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    FormPanelSectionsDivider(label = "Pricing & notes", spacing = spacing)

    FormPanelSection(
        title = "Pricing",
        description = "Amount charged per person on this plan.",
        spacing = spacing,
        role = FormPanelSectionRole.Tertiary,
    ) {
        validation.ValidatedGlideOutlinedField(
            fieldKey = "priceMajor",
            value = state.priceMajor,
            onValueChange = { onStateChange(state.copy(priceMajor = it)) },
            label = "Price per person",
            placeholder = "e.g. 12.00",
            readOnly = readOnly,
            required = true,
        )
        Spacer(modifier = Modifier.height(spacing.field))
        GlideOutlinedField(
            value = state.notes,
            onValueChange = { onStateChange(state.copy(notes = it)) },
            label = "Notes",
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            fieldHeight = GlideDimensions.notesMinHeight,
            readOnly = readOnly,
        )
    }
}
