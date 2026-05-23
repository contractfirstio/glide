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
import androidx.compose.material3.AlertDialog
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
import glide.data.ScheduledClassStore
import glide.data.TermStore
import glide.model.AcademicTerm
import glide.ui.layout.GlideLayout
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.leads.parseIsoDateToMillis
import glide.ui.shared.IsoDateField
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
) {
    fun isValid(): Boolean {
        if (name.isBlank()) return false
        val startMillis = parseIsoDateToMillis(startDate) ?: return false
        val endMillis = parseIsoDateToMillis(endDate) ?: return false
        return endMillis >= startMillis
    }

    fun toTerm(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): AcademicTerm = AcademicTerm(
        id = existingId ?: UUID.randomUUID().toString(),
        name = name.trim(),
        startDate = startDate,
        endDate = endDate,
        notes = notes.trim(),
        createdAtMillis = createdAtMillis,
    )
}

@Composable
fun TermsPanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var formState by remember { mutableStateOf(TermFormState()) }
    var isCreating by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val terms = TermStore.sortedForPanel()

    fun clearSelection() {
        selectedId = null
        isCreating = true
        formState = TermFormState()
        formError = null
    }

    fun resetFormForCreate() {
        selectedId = null
        isCreating = true
        formState = TermFormState()
        formError = null
    }

    fun loadIntoForm(term: AcademicTerm) {
        selectedId = term.id
        isCreating = false
        formState = TermFormState(
            name = term.name,
            startDate = term.startDate,
            endDate = term.endDate,
            notes = term.notes,
        )
        formError = null
    }

    LaunchedEffect(terms, selectedId) {
        if (selectedId != null && terms.none { it.id == selectedId }) {
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
                    text = "Define academic terms and school breaks for your schedule.",
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
                            text = "${terms.size} term${if (terms.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        GlideButton(onClick = { resetFormForCreate() }) {
                            Text(if (compact) "New" else "New term")
                        }
                    }
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
                                text = "No terms yet.",
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
                            items(terms, key = { it.id }) { term ->
                                TermListItem(
                                    term = term,
                                    selected = term.id == selectedId,
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

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        TermForm(
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
                                    formError = "Name, start date, and end date are required. End must be on or after start."
                                    return@GlideButton
                                }
                                formError = null
                                if (isCreating) {
                                    val term = formState.toTerm()
                                    TermStore.create(term)
                                    loadIntoForm(term)
                                } else {
                                    val existing = selectedId?.let { TermStore.findById(it) }
                                    if (existing != null) {
                                        val updated = formState.toTerm(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                        )
                                        TermStore.update(updated)
                                        loadIntoForm(updated)
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
        val linkedClasses = ScheduledClassStore.countForTerm(selectedId!!)
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete term?") },
            text = {
                Text(
                    if (linkedClasses > 0) {
                        "This term will be removed. $linkedClasses class${if (linkedClasses == 1) "" else "es"} " +
                            "will be unlinked from this term."
                    } else {
                        "This term will be removed permanently."
                    },
                )
            },
            confirmButton = {
                GlideTextButton(
                    onClick = {
                        TermStore.delete(selectedId!!)
                        showDeleteConfirm = false
                        clearSelection()
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
private fun TermListItem(
    term: AcademicTerm,
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
        val classCount = ScheduledClassStore.countForTerm(term.id)
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
) {
    GlideOutlinedField(
        value = state.name,
        onValueChange = { onStateChange(state.copy(name = it)) },
        label = "Term name",
        placeholder = "e.g. Spring 2026",
    )
    Spacer(modifier = Modifier.height(spacing.field))
    IsoDateField(
        label = "Start date",
        value = state.startDate,
        onValueChange = { onStateChange(state.copy(startDate = it)) },
    )
    Spacer(modifier = Modifier.height(spacing.field))
    IsoDateField(
        label = "End date",
        value = state.endDate,
        onValueChange = { onStateChange(state.copy(endDate = it)) },
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
