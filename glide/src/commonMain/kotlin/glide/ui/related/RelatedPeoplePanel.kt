package glide.ui.related

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
import glide.data.RelatedPersonStore
import glide.model.RelatedPerson
import glide.ui.layout.GlideLayout
import glide.ui.leads.DateOfBirthField
import glide.ui.shared.formatPersonLabel
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton
import java.util.UUID

private data class RelatedPersonFormState(
    val name: String = "",
    val dateOfBirth: String = "",
    val notes: String = "",
) {
    fun isValid(): Boolean = name.isNotBlank()

    fun toRelatedPerson(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): RelatedPerson =
        RelatedPerson(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name.trim(),
            dateOfBirth = dateOfBirth.trim(),
            notes = notes.trim(),
            createdAtMillis = createdAtMillis,
        )
}

@Composable
fun RelatedPeoplePanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var formState by remember { mutableStateOf(RelatedPersonFormState()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val people = RelatedPersonStore.onCustomerPacks

    fun clearSelection() {
        selectedId = null
        formState = RelatedPersonFormState()
        formError = null
    }

    LaunchedEffect(people, selectedId) {
        if (selectedId != null && people.none { it.id == selectedId }) {
            clearSelection()
        }
    }

    fun loadIntoForm(person: RelatedPerson) {
        selectedId = person.id
        formState = RelatedPersonFormState(
            name = person.name,
            dateOfBirth = person.dateOfBirth,
            notes = person.notes,
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
                    text = "Edit related people on customer packs. They appear here after a lead becomes a customer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(spacing.section))
            }

            val listSection: @Composable (Modifier) -> Unit = { listModifier ->
                Column(modifier = listModifier) {
                    Text(
                        text = "${people.size} related ${if (people.size == 1) "person" else "people"}",
                        style = MaterialTheme.typography.labelLarge,
                    )
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
                                text = "No one on a customer pack yet. Add related people on a lead, then make the lead a customer.",
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
                                RelatedPersonListItem(
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
                        text = "Edit related person",
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
                                text = "Select someone on a customer pack to edit. Add them on a lead and convert to customer first.",
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
                            RelatedPersonForm(
                                state = formState,
                                onStateChange = { formState = it },
                                spacing = spacing,
                            )

                            val groupCount = selectedId?.let { RelatedPersonStore.customerPackCount(it) } ?: 0
                            if (groupCount > 0) {
                                Spacer(modifier = Modifier.height(spacing.field))
                                Text(
                                    text = "On $groupCount customer pack${if (groupCount == 1) "" else "s"}.",
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
                                    if (!formState.isValid()) {
                                        formError = "Name is required."
                                        return@GlideButton
                                    }
                                    formError = null
                                    val existing = selectedId?.let { RelatedPersonStore.findById(it) }
                                    if (existing != null) {
                                        val updated = formState.toRelatedPerson(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                        )
                                        RelatedPersonStore.update(updated)
                                        loadIntoForm(updated)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(saveLabel)
                            }

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
        val onCustomerPack = RelatedPersonStore.isOnCustomerPack(selectedId!!)
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete related person?") },
            text = {
                Text(
                    if (onCustomerPack) {
                        "Cannot delete — they are on a customer pack, which is locked."
                    } else {
                        "They will be removed from all leads."
                    },
                )
            },
            confirmButton = {
                GlideTextButton(
                    onClick = {
                        if (!onCustomerPack) {
                            RelatedPersonStore.delete(selectedId!!)
                            showDeleteConfirm = false
                            clearSelection()
                        } else {
                            showDeleteConfirm = false
                            formError = "Cannot delete someone on a locked customer pack."
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
private fun RelatedPersonListItem(
    person: RelatedPerson,
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
    val groupCount = RelatedPersonStore.customerPackCount(person.id)
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (groupCount > 0) {
            Text(
                text = "$groupCount pack${if (groupCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RelatedPersonForm(
    state: RelatedPersonFormState,
    onStateChange: (RelatedPersonFormState) -> Unit,
    spacing: GlideLayout.Spacing,
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
