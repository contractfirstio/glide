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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import glide.data.BillingPanelState
import glide.data.ClientStore
import glide.data.ClientsPanelState
import glide.data.PeopleGroupStore
import glide.data.PlanStore
import glide.data.PlansPanelState
import glide.data.StudentsPanelState
import glide.data.StudentStore
import glide.data.resolveMainClient
import glide.model.Plan
import glide.model.PlanKind
import glide.model.formatMoney
import glide.model.majorToMinor
import glide.model.parseMajorAmount
import glide.model.summaryLine
import glide.model.DEFAULT_CURRENCY_CODE
import glide.ui.layout.GlideLayout
import glide.ui.shared.DeleteConfirmDialog
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.rememberFormDirtyTracker
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
    val lessonCount: String = "10",
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

    val customerGroupId = BillingPanelState.peopleGroupId
    val clientFilterId = ClientsPanelState.selectedClientId
    val studentFilterId = StudentsPanelState.selectedStudentId
    val outboundPlanFilterId = PlansPanelState.selectedPlanId
    val plans = PlanStore.forPlansPanel(customerGroupId, clientFilterId, studentFilterId)
    val outboundPlanFilterLabel = outboundPlanFilterId?.let {
        PlanStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val customerGroupLabel = customerGroupId?.let { id ->
        PeopleGroupStore.findById(id)?.resolveMainClient()?.name?.takeIf { it.isNotBlank() }
    }
    val clientFilterLabel = clientFilterId?.let { ClientStore.findById(it)?.name?.takeIf { it.isNotBlank() } }
    val studentFilterLabel = studentFilterId?.let {
        StudentStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }
    val hasInboundFilter = customerGroupId != null ||
        clientFilterId != null ||
        studentFilterId != null

    fun clearLocalSelection() {
        selectedId = null
        isCreating = false
        form.load(PlanFormState())
        formError = null
    }

    fun clearSelection() {
        clearLocalSelection()
        PlansPanelState.clearPlanFilter()
    }

    fun resetFormForCreate() {
        clearLocalSelection()
        isCreating = true
        form.load(PlanFormState())
        formError = null
    }

    fun loadIntoForm(plan: Plan) {
        PlansPanelState.onPlanSelected(plan.id)
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
    }

    LaunchedEffect(customerGroupId, clientFilterId, studentFilterId) {
        if (hasInboundFilter && selectedId != null) {
            clearLocalSelection()
        }
    }

    LaunchedEffect(plans, selectedId) {
        if (selectedId != null && plans.none { it.id == selectedId }) {
            clearSelection()
        }
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
                        customerGroupId != null -> {
                            val label = customerGroupLabel ?: "this customer group"
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
                            "Filtering customer groups, clients, and students for $label. Use Clear filter to reset."
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
                            text = "${plans.size} plan${if (plans.size == 1) "" else "s"}",
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
                            if (customerGroupId != null) {
                                GlideTextButton(onClick = { BillingPanelState.onCustomerGroupCleared() }) {
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
                                    customerGroupId != null ->
                                        "No plan on this customer group."
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
                            items(plans, key = { it.id }) { plan ->
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
                                    formError = when (form.draft.kind) {
                                        PlanKind.SINGLE_LESSON_PLAN -> "Name and a valid price per person are required."
                                        PlanKind.MULTI_LESSON_PLAN -> "Name, class count, and a valid price per person are required."
                                        PlanKind.CAMP -> "Name, day count, and a valid price per person are required."
                                    }
                                    return@GlideButton
                                }
                                val plan = form.draft.toPlan()
                                if (plan == null) {
                                    formError = "Count must be a positive number."
                                    return@GlideButton
                                }
                                formError = null
                                if (isCreating) {
                                    PlanStore.create(plan)
                                    loadIntoForm(plan)
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
                            GlideOutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                enabled = selectedId?.let { PlanStore.canDelete(it) } == true,
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
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

    if (showDeleteConfirm && selectedId != null) {
        val blockReason = PlanStore.planDeletionBlockReason(selectedId!!)
        DeleteConfirmDialog(
            title = "Delete plan?",
            message = blockReason ?: "This plan will be removed permanently.",
            onDismiss = { showDeleteConfirm = false },
            onContinue = {
                if (PlanStore.delete(selectedId!!)) {
                    showDeleteConfirm = false
                    clearSelection()
                    isCreating = true
                    form.load(PlanFormState())
                } else {
                    showDeleteConfirm = false
                    formError = blockReason ?: "This plan has been sold and cannot be deleted."
                }
            },
            continueEnabled = blockReason == null,
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
) {
    var kindExpanded by remember { mutableStateOf(false) }

    FormPanelSection(
        title = "Plan identity",
        description = "Name and type of plan you offer to customers.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        GlideOutlinedField(
            value = state.name,
            onValueChange = { onStateChange(state.copy(name = it)) },
            label = "Plan name",
            placeholder = "e.g. 10 Class Rolling Plan",
            readOnly = readOnly,
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
                                        PlanKind.MULTI_LESSON_PLAN -> state.copy(kind = kind)
                                        PlanKind.CAMP -> state.copy(
                                            kind = kind,
                                            rolling = false,
                                            lessonCount = if (state.lessonCount == "1") "5" else state.lessonCount,
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
                GlideOutlinedField(
                    value = state.lessonCount,
                    onValueChange = { onStateChange(state.copy(lessonCount = it.filter { c -> c.isDigit() })) },
                    label = "Number of classes",
                    placeholder = "10",
                    readOnly = readOnly,
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
                GlideOutlinedField(
                    value = state.lessonCount,
                    onValueChange = { onStateChange(state.copy(lessonCount = it.filter { c -> c.isDigit() })) },
                    label = "Number of days",
                    placeholder = "5",
                    readOnly = readOnly,
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
        GlideOutlinedField(
            value = state.priceMajor,
            onValueChange = { onStateChange(state.copy(priceMajor = it)) },
            label = "Price per person",
            placeholder = "e.g. 12.00",
            readOnly = readOnly,
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
