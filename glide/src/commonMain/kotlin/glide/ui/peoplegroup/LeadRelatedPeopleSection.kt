package glide.ui.peoplegroup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.data.RelatedPersonStore
import glide.model.RelatedPerson
import glide.ui.layout.GlideLayout
import glide.ui.leads.DateOfBirthField
import glide.ui.shared.formatPersonLabel
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField

@Composable
fun LeadRelatedPeopleSection(
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    var relatedSearchQuery by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newDateOfBirth by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }

    val linked = selectedIds.mapNotNull { RelatedPersonStore.findById(it) }
    val relatedSearchResults = remember(relatedSearchQuery, selectedIds, RelatedPersonStore.all) {
        val query = relatedSearchQuery.trim()
        if (query.isEmpty()) {
            emptyList()
        } else {
            RelatedPersonStore.all
                .filter { it.id !in selectedIds }
                .filter { person ->
                    listOf(person.name, person.dateOfBirth, person.notes)
                        .any { it.matchesEntitySearch(query) }
                }
                .map { it.toSearchItem() }
        }
    }

    LeadPanelSection(
        title = "Related people",
        description = "Others included on this lead besides the main contact.",
        spacing = spacing,
    ) {
        if (linked.isNotEmpty()) {
            GlideFieldLabel("On this lead")
            Spacer(modifier = Modifier.height(spacing.field))
            linked.forEach { person ->
                LeadRelatedPersonLinkedRow(
                    person = person,
                    onRemove = { onSelectionChange(selectedIds.filter { it != person.id }) },
                )
                Spacer(modifier = Modifier.height(spacing.field))
            }
            Spacer(modifier = Modifier.height(spacing.section))
        }

        LeadActionSubsection(
            title = "Link existing related person",
            description = "Search people already added on other leads or customer packs.",
            spacing = spacing,
        ) {
            EntitySearchPicker(
                label = "Search saved related people",
                placeholder = "Type name or date of birth…",
                query = relatedSearchQuery,
                onQueryChange = { relatedSearchQuery = it },
                results = relatedSearchResults,
                onSelect = { id -> onSelectionChange(selectedIds + id) },
                noResultsText = "No saved people match. Create a new related person below instead.",
            )
        }

        Spacer(modifier = Modifier.height(spacing.section))

        LeadActionSubsection(
            title = "Create new related person",
            description = "Add someone new to the system and attach them to this lead.",
            spacing = spacing,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                GlideOutlinedField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        addError = null
                    },
                    label = "New person name",
                )
                Spacer(modifier = Modifier.height(spacing.field))
                DateOfBirthField(
                    value = newDateOfBirth,
                    onValueChange = { newDateOfBirth = it },
                )
                Spacer(modifier = Modifier.height(spacing.field))
                GlideOutlinedButton(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isEmpty()) {
                            addError = "Enter a name before adding."
                            return@GlideOutlinedButton
                        }
                        val person = RelatedPerson(name = trimmed, dateOfBirth = newDateOfBirth.trim())
                        RelatedPersonStore.create(person)
                        onSelectionChange(selectedIds + person.id)
                        newName = ""
                        newDateOfBirth = ""
                        addError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add new person to this lead")
                }
                addError?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.field))
        Text(
            text = "Edit names and details in the Related panel once they are on a customer pack.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun RelatedPerson.toSearchItem() = SearchResultItem(
    id = id,
    primaryLabel = formatPersonLabel(name, dateOfBirth),
    secondaryLabel = notes.takeIf { it.isNotBlank() },
)
