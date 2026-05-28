package glide.ui.scheduling

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import glide.data.LocationStore
import glide.data.ClassStore
import glide.data.SchedulePanelState
import glide.data.TermStore
import glide.data.validateTermDisablingRollingPlans
import glide.model.Term
import glide.model.findOverlappingTerm
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
import glide.ui.shared.ValidatedIsoDateRangeField
import glide.ui.shared.rememberFormDirtyTracker
import glide.ui.shared.rememberFormValidation
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.leads.parseIsoDateToMillis
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton
import glide.ui.theme.glideListItemTitleColor
import java.util.UUID

private data class TermFormState(
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = "",
    val acceptsRollingPlans: Boolean = true,
) {
    fun isValid(): Boolean {
        if (name.isBlank()) return false
        val startMillis = parseIsoDateToMillis(startDate) ?: return false
        val endMillis = parseIsoDateToMillis(endDate) ?: return false
        return endMillis >= startMillis
    }

    fun validationFieldKeys(): List<String> = buildList {
        if (name.isBlank()) add("name")
        val startMillis = parseIsoDateToMillis(startDate)
        val endMillis = parseIsoDateToMillis(endDate)
        if (startMillis == null) add("startDate")
        if (endMillis == null) add("endDate")
        if (startMillis != null && endMillis != null && endMillis < startMillis) add("endDate")
    }

    fun toTerm(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): Term = Term(
        id = existingId ?: UUID.randomUUID().toString(),
        name = name.trim(),
        startDate = startDate,
        endDate = endDate,
        notes = notes.trim(),
        acceptsRollingPlans = acceptsRollingPlans,
        createdAtMillis = createdAtMillis,
    )
}

private fun overlapErrorMessage(overlapping: Term): String {
    val range = buildString {
        if (overlapping.startDate.isNotBlank()) append(formatIsoDateForDisplay(overlapping.startDate))
        if (overlapping.startDate.isNotBlank() && overlapping.endDate.isNotBlank()) append(" – ")
        if (overlapping.endDate.isNotBlank()) append(formatIsoDateForDisplay(overlapping.endDate))
    }
    return "Dates overlap with \"${overlapping.name}\" ($range). Terms must not share any dates."
}

@Composable
fun TermsPanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val form = rememberFormDirtyTracker(TermFormState())
    var isCreating by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    val formValidation = rememberFormValidation()
    val saveScope = rememberCoroutineScope()
    var listSearchQuery by remember { mutableStateOf("") }

    val allTerms = TermStore.sortedForPanel()
    val termFilterId = SchedulePanelState.selectedTermFilterId
    val locationFilterId = SchedulePanelState.selectedLocationFilterId
    val classSyncId = SchedulePanelState.selectedClassId.takeIf {
        termFilterId == null && locationFilterId == null
    }
    val terms = if (locationFilterId != null) {
        val termIds = ClassStore.termIdsForLocation(locationFilterId)
        allTerms.filter { it.id in termIds }
    } else {
        allTerms
    }
    val filteredTerms = remember(terms, listSearchQuery) {
        terms.filter { term ->
            matchesPanelListSearch(
                listSearchQuery,
                term.name,
                term.notes,
                formatIsoDateForDisplay(term.startDate),
                formatIsoDateForDisplay(term.endDate),
            )
        }
    }
    val searchActive = listSearchQuery.isNotBlank()
    val highlightedTermId = termFilterId ?: selectedId
    val locationFilterLabel = locationFilterId?.let {
        LocationStore.findById(it)?.name?.takeIf { name -> name.isNotBlank() }
    }

    fun clearLocalSelection() {
        selectedId = null
        isCreating = true
        form.load(TermFormState())
        formError = null
        formValidation.clear()
    }

    fun clearSelection() {
        clearLocalSelection()
        SchedulePanelState.onTermCleared()
    }

    fun resetFormForCreate() {
        clearLocalSelection()
        SchedulePanelState.onTermCleared()
    }

    fun syncTermIntoForm(term: Term) {
        selectedId = term.id
        isCreating = false
        form.load(
            TermFormState(
                name = term.name,
                startDate = term.startDate,
                endDate = term.endDate,
                notes = term.notes,
                acceptsRollingPlans = term.acceptsRollingPlans,
            ),
        )
        formError = null
        formValidation.clear()
    }

    fun loadIntoForm(term: Term) {
        selectedId = term.id
        isCreating = false
        SchedulePanelState.onTermSelected(term.id)
        form.load(
            TermFormState(
                name = term.name,
                startDate = term.startDate,
                endDate = term.endDate,
                notes = term.notes,
                acceptsRollingPlans = term.acceptsRollingPlans,
            ),
        )
        formError = null
        formValidation.clear()
    }

    LaunchedEffect(allTerms, selectedId, locationFilterId) {
        if (selectedId != null && allTerms.none { it.id == selectedId }) {
            clearSelection()
        }
    }

    LaunchedEffect(classSyncId, allTerms) {
        val classId = classSyncId ?: return@LaunchedEffect
        val scheduledClass = ClassStore.findById(classId) ?: return@LaunchedEffect
        val resolvedTermId = termIdForClassSelection(scheduledClass, allTerms) ?: return@LaunchedEffect
        allTerms.find { it.id == resolvedTermId }?.let { syncTermIntoForm(it) }
    }

    LaunchedEffect(termFilterId, allTerms) {
        val id = termFilterId ?: return@LaunchedEffect
        if (selectedId == id) return@LaunchedEffect
        allTerms.find { it.id == id }?.let { syncTermIntoForm(it) }
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
                        locationFilterId != null -> {
                            val label = locationFilterLabel ?: "this location"
                            "Showing terms with classes at $label. Use Clear filter in Locations to reset."
                        }
                        else -> "Define academic terms and school breaks. Term dates cannot overlap."
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
                                singular = "term",
                                plural = "terms",
                                filteredCount = filteredTerms.size,
                                totalCount = terms.size,
                                searchActive = searchActive,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.field)) {
                            if (locationFilterId != null) {
                                GlideTextButton(onClick = { SchedulePanelState.clearLocationFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            GlideButton(onClick = { resetFormForCreate() }) {
                                Text(if (compact) "New" else "New term")
                            }
                        }
                    }
                    PanelListSearchSpacer()
                    PanelListSearchField(
                        query = listSearchQuery,
                        onQueryChange = { listSearchQuery = it },
                        placeholder = "Term name…",
                    )
                    Spacer(modifier = Modifier.height(spacing.field))

                    if (terms.isEmpty()) {
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
                                    locationFilterId != null -> "No terms with classes at this location."
                                    else -> "No terms yet."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (filteredTerms.isEmpty()) {
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
                                text = "No terms match your search.",
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
                            items(filteredTerms, key = { it.id }) { term ->
                                TermListItem(
                                    term = term,
                                    selected = term.id == highlightedTermId,
                                    compact = compact,
                                    onClick = { loadIntoForm(term) },
                                )
                            }
                        }
                    }
                }
            }

            val formSection: @Composable (Modifier) -> Unit = { formModifier ->
                Column(modifier = formModifier.fillMaxHeight()) {
                    Text(
                        text = if (isCreating) "Create term" else "Edit term",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(spacing.section))

                    val termReadOnly = !isCreating &&
                        selectedId != null &&
                        !TermStore.canEdit(selectedId!!)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        TermForm(
                            state = form.draft,
                            onStateChange = { form.draft = it },
                            spacing = spacing,
                            readOnly = termReadOnly,
                            validation = formValidation,
                        )

                        if (termReadOnly) {
                            Spacer(modifier = Modifier.height(spacing.field))
                            Text(
                                text = TermStore.termEditBlockReason(selectedId!!)
                                    ?: "This term is attached to a class and cannot be edited.",
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
                                val invalidKeys = form.draft.validationFieldKeys()
                                if (invalidKeys.isNotEmpty()) {
                                    formValidation.reportInvalid(invalidKeys, saveScope)
                                    formError = "Name, start date, and end date are required. End must be on or after start."
                                    return@GlideButton
                                }
                                val excludeId = if (isCreating) null else selectedId
                                val candidate = form.draft.toTerm(existingId = excludeId)
                                val overlapping = findOverlappingTerm(terms, candidate, excludeTermId = excludeId)
                                if (overlapping != null) {
                                    formError = overlapErrorMessage(overlapping)
                                    return@GlideButton
                                }
                                validateTermDisablingRollingPlans(candidate)?.let { message ->
                                    formError = message
                                    return@GlideButton
                                }
                                formError = null
                                formValidation.clear()
                                if (isCreating) {
                                    if (!TermStore.create(candidate)) {
                                        formError = "Could not create term — dates overlap an existing term."
                                        return@GlideButton
                                    }
                                    loadIntoForm(candidate)
                                } else {
                                    val existing = selectedId?.let { TermStore.findById(it) }
                                    if (existing != null) {
                                        TermStore.termEditBlockReason(existing.id)?.let { reason ->
                                            formError = reason
                                            return@GlideButton
                                        }
                                        val updated = form.draft.toTerm(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                        )
                                        if (!TermStore.update(updated)) {
                                            formError = TermStore.termEditBlockReason(existing.id)
                                                ?: "Could not save term — dates overlap an existing term."
                                            return@GlideButton
                                        }
                                        loadIntoForm(updated)
                                    }
                                }
                            },
                            enabled = form.isDirty && !termReadOnly,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(saveLabel)
                        }

                        if (!isCreating) {
                            DeleteActionButton(
                                onClick = { showDeleteConfirm = true },
                                enabled = selectedId?.let { TermStore.canDelete(it) } == true,
                                blockedReason = selectedId
                                    ?.let { id -> TermStore.classCount(id) }
                                    ?.takeIf { it > 0 }
                                    ?.let { count ->
                                        "Used by $count class${if (count == 1) "" else "es"}. Remove it from those classes first."
                                    },
                            )
                        }
                    }
                }
            }

            ListFormPanelLayout(
                hasSelection = selectedId != null || isCreating,
                spacing = spacing,
                onCloseForm = { clearSelection() },
                modifier = Modifier.fillMaxSize(),
                listSection = listSection,
                formSection = formSection,
            )
        }
    }

    if (showDeleteConfirm && selectedId != null) {
        val linkedClasses = TermStore.classCount(selectedId!!)
        val attachedToClass = linkedClasses > 0
        DeleteConfirmDialog(
            title = "Delete term?",
            message = if (attachedToClass) {
                "This term is used by $linkedClasses class${if (linkedClasses == 1) "" else "es"} " +
                    "and cannot be deleted. Remove it from those classes first."
            } else {
                "This term will be removed permanently."
            },
            onDismiss = { showDeleteConfirm = false },
            onContinue = {
                if (TermStore.delete(selectedId!!)) {
                    showDeleteConfirm = false
                    clearSelection()
                } else {
                    showDeleteConfirm = false
                    formError = "This term is attached to a class and cannot be deleted."
                }
            },
            continueEnabled = !attachedToClass,
            blockedReason = if (attachedToClass) {
                "Remove this term from linked classes before deleting."
            } else {
                null
            },
        )
    }
}

@Composable
private fun TermListItem(
    term: Term,
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
    val dateLine = buildString {
        if (term.startDate.isNotBlank()) append(formatIsoDateForDisplay(term.startDate))
        if (term.startDate.isNotBlank() && term.endDate.isNotBlank()) append(" – ")
        if (term.endDate.isNotBlank()) append(formatIsoDateForDisplay(term.endDate))
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
            text = term.name,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = glideListItemTitleColor(selected),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (dateLine.isNotBlank()) {
            Text(
                text = dateLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val classCount = TermStore.classCount(term.id)
        if (classCount > 0) {
            Text(
                text = "$classCount class${if (classCount == 1) "" else "es"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TermForm(
    state: TermFormState,
    onStateChange: (TermFormState) -> Unit,
    spacing: GlideLayout.Spacing,
    readOnly: Boolean = false,
    validation: FormValidationState,
) {
    FormPanelSection(
        title = "Term identity",
        description = "Name shown on the calendar and class forms.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        validation.ValidatedGlideOutlinedField(
            fieldKey = "name",
            value = state.name,
            onValueChange = { onStateChange(state.copy(name = it)) },
            label = "Term name",
            placeholder = "e.g. Spring 2026",
            readOnly = readOnly,
        )
    }

    FormPanelSectionsDivider(label = "Date range", spacing = spacing)

    FormPanelSection(
        title = "Dates",
        description = "When this term starts and ends.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        validation.ValidatedIsoDateRangeField(
            startFieldKey = "startDate",
            endFieldKey = "endDate",
            label = "Start and end date",
            startValue = state.startDate,
            endValue = state.endDate,
            onValueChange = { startIso, endIso ->
                onStateChange(state.copy(startDate = startIso, endDate = endIso))
            },
            readOnly = readOnly,
        )
    }

    FormPanelSectionsDivider(label = "Rolling plans", spacing = spacing)

    FormPanelSection(
        title = "Rolling plans",
        description = "Whether rolling plan classes can extend into this term.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = state.acceptsRollingPlans,
                onCheckedChange = { onStateChange(state.copy(acceptsRollingPlans = it)) },
                enabled = !readOnly,
            )
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = "Accepts Rolling Plans",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "Rolling plan schedules may span into this term when a new term is added.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    FormPanelSectionsDivider(label = "Notes", spacing = spacing)

    FormPanelSection(
        title = "Notes",
        description = "Internal notes about this term.",
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
            fieldHeight = GlideDimensions.notesMinHeight,
            readOnly = readOnly,
        )
    }
}
