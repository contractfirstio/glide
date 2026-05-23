package glide.ui.peoplegroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.data.ContactStore
import glide.data.PlanStore
import glide.data.RelatedPersonStore
import glide.model.RelatedPerson
import glide.model.summaryLine
import glide.ui.layout.GlideLayout
import glide.ui.shared.formatPersonLabel
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.GlideTextButton

@Composable
fun ReadOnlyMainContactSection(
    contactId: String?,
    modifier: Modifier = Modifier,
) {
    val contact = contactId?.let { ContactStore.findById(it) }
    Column(modifier = modifier) {
        GlideFieldLabel("Main contact")
        Spacer(modifier = Modifier.height(2.dp))
        if (contact == null) {
            Text(
                text = "No main contact linked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = formatPersonLabel(contact.name, contact.dateOfBirth),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (contact.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (contact.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Edit this person's details in the People panel.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ReadOnlyRelatedPeopleSection(relatedPersonIds: List<String>) {
    val people = relatedPersonIds.mapNotNull { RelatedPersonStore.findById(it) }
    Text(
        text = "Related people",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(4.dp))
    if (people.isEmpty()) {
        Text(
            text = "None linked.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        people.forEach { person ->
            Text(
                text = formatPersonLabel(person.name, person.dateOfBirth),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun ReadOnlyPackSection(planId: String?) {
    val plan = planId?.let { PlanStore.findById(it) }
    GlideFieldLabel("Pack")
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = plan?.let { "${it.name} (${it.summaryLine()})" } ?: "—",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun LeadRelatedPersonLinkedRow(
    person: RelatedPerson,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatPersonLabel(person.name, person.dateOfBirth),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        GlideTextButton(onClick = onRemove) {
            Text("Remove", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun RelatedPersonLinkSection(
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    var relatedSearchQuery by remember { mutableStateOf("") }
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
                .map { person ->
                    SearchResultItem(
                        id = person.id,
                        primaryLabel = formatPersonLabel(person.name, person.dateOfBirth),
                        secondaryLabel = person.notes.takeIf { it.isNotBlank() },
                    )
                }
        }
    }

    Text(
        text = "Related people",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(spacing.field))

    linked.forEach { person ->
        LeadRelatedPersonLinkedRow(
            person = person,
            onRemove = { onSelectionChange(selectedIds.filter { it != person.id }) },
        )
        Spacer(modifier = Modifier.height(spacing.field))
    }

    EntitySearchPicker(
        label = "Search related people",
        placeholder = "Name or date of birth",
        query = relatedSearchQuery,
        onQueryChange = { relatedSearchQuery = it },
        results = relatedSearchResults,
        onSelect = { id -> onSelectionChange(selectedIds + id) },
        noResultsText = "No matches. Add related people on a lead first.",
    )

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Edit details in the Related panel once they are on a customer pack.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
