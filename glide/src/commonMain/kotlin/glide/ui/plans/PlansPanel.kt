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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import glide.data.PlanStore
import glide.model.Plan
import glide.model.PlanKind
import glide.model.formatMoney
import glide.model.majorToMinor
import glide.model.parseMajorAmount
import glide.model.summaryLine
import glide.model.DEFAULT_CURRENCY_CODE
import glide.ui.layout.GlideLayout
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
    val kind: PlanKind = PlanKind.MULTI_LESSON_PACK,
    val lessonCount: String = "10",
    val rolling: Boolean = true,
    val priceMajor: String = "",
    val notes: String = "",
) {
    fun isValid(): Boolean {
        if (name.isBlank()) return false
        if (parseMajorAmount(priceMajor) == null) return false
        return when (kind) {
            PlanKind.SINGLE_LESSON_PACK -> true
            PlanKind.MULTI_LESSON_PACK -> lessonCount.toIntOrNull()?.let { it > 0 } == true
        }
    }

    fun toPlan(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
        existingCurrency: String = DEFAULT_CURRENCY_CODE,
    ): Plan? {
        val count = when (kind) {
            PlanKind.SINGLE_LESSON_PACK -> 1
            PlanKind.MULTI_LESSON_PACK -> lessonCount.toIntOrNull() ?: return null
        }
        val priceMajorAmount = parseMajorAmount(priceMajor) ?: return null
        return Plan(
            id = existingId ?: UUID.randomUUID().toString(),
            kind = kind,
            name = name.trim(),
            lessonCount = count,
            rolling = kind != PlanKind.SINGLE_LESSON_PACK && rolling,
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
    var formState by remember { mutableStateOf(PlanFormState()) }
    var isCreating by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val plans = PlanStore.plans

    fun resetFormForCreate() {
        selectedId = null
        isCreating = true
        formState = PlanFormState()
        formError = null
    }

    fun loadIntoForm(plan: Plan) {
        selectedId = plan.id
        isCreating = false
        formState = PlanFormState(
            name = plan.name,
            kind = plan.kind,
            lessonCount = plan.lessonCount.toString(),
            rolling = plan.rolling,
            priceMajor = plan.priceMajorString(),
            notes = plan.notes,
        )
        formError = null
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
                    text = "Define plan types and packs offered to customers.",
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
                        GlideButton(onClick = { resetFormForCreate() }) {
                            Text(if (compact) "New" else "New plan")
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
                                text = "No plans yet.",
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

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        PlanForm(
                            state = formState,
                            onStateChange = { formState = it },
                            spacing = spacing,
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

                    Spacer(modifier = Modifier.height(spacing.field))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.field),
                    ) {
                        GlideButton(
                            onClick = {
                                if (!formState.isValid()) {
                                    formError = when (formState.kind) {
                                        PlanKind.SINGLE_LESSON_PACK -> "Name and a valid price are required."
                                        PlanKind.MULTI_LESSON_PACK -> "Name, class count, and a valid price are required."
                                    }
                                    return@GlideButton
                                }
                                val plan = formState.toPlan()
                                if (plan == null) {
                                    formError = "Class count must be a positive number."
                                    return@GlideButton
                                }
                                formError = null
                                if (isCreating) {
                                    PlanStore.create(plan)
                                    loadIntoForm(plan)
                                } else {
                                    val existing = selectedId?.let { PlanStore.findById(it) }
                                    if (existing != null) {
                                        val updated = formState.toPlan(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                            existingCurrency = existing.currencyCode,
                                        )
                                        if (updated != null) {
                                            PlanStore.update(updated)
                                            loadIntoForm(updated)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(saveLabel)
                        }

                        if (!isCreating) {
                            GlideOutlinedButton(onClick = { showDeleteConfirm = true }) {
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete plan?") },
            text = { Text("This plan will be removed permanently.") },
            confirmButton = {
                GlideTextButton(
                    onClick = {
                        PlanStore.delete(selectedId!!)
                        showDeleteConfirm = false
                        resetFormForCreate()
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
            text = formatMoney(plan.priceAmountMinor, plan.currencyCode),
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
) {
    var kindExpanded by remember { mutableStateOf(false) }

    GlideOutlinedField(
        value = state.name,
        onValueChange = { onStateChange(state.copy(name = it)) },
        label = "Plan name",
        placeholder = "e.g. 10 Class Rolling Pack",
    )
    Spacer(modifier = Modifier.height(spacing.field))

    Column {
        GlideFieldLabel("Plan type")
        Spacer(modifier = Modifier.height(2.dp))
        ExposedDropdownMenuBox(
            expanded = kindExpanded,
            onExpandedChange = { kindExpanded = it },
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
                                    PlanKind.SINGLE_LESSON_PACK -> state.copy(
                                        kind = kind,
                                        lessonCount = "1",
                                        rolling = false,
                                    )
                                    PlanKind.MULTI_LESSON_PACK -> state.copy(kind = kind)
                                },
                            )
                            kindExpanded = false
                        },
                    )
                }
            }
        }
    }

    when (state.kind) {
        PlanKind.MULTI_LESSON_PACK -> {
            Spacer(modifier = Modifier.height(spacing.field))
            GlideOutlinedField(
                value = state.lessonCount,
                onValueChange = { onStateChange(state.copy(lessonCount = it.filter { c -> c.isDigit() })) },
                label = "Number of classes",
                placeholder = "10",
            )
            Spacer(modifier = Modifier.height(spacing.field))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = state.rolling,
                    onCheckedChange = { onStateChange(state.copy(rolling = it)) },
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "Rolling plan",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "Classes roll over within the pack (e.g. 10-class rolling pack).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PlanKind.SINGLE_LESSON_PACK -> {
            Spacer(modifier = Modifier.height(spacing.field))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        MaterialTheme.shapes.small,
                    )
                    .padding(spacing.outer),
            ) {
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

    Spacer(modifier = Modifier.height(spacing.field))
    GlideOutlinedField(
        value = state.priceMajor,
        onValueChange = { onStateChange(state.copy(priceMajor = it)) },
        label = "Price",
        placeholder = "e.g. 120.00",
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
    )
}
