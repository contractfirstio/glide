package glide.ui.peoplegroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
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
import glide.data.ClientStore
import glide.data.PlanStore
import glide.data.StudentStore
import glide.model.Student
import glide.model.summaryLine
import glide.ui.layout.GlideLayout
import glide.ui.shared.formatPersonLabel
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.GlideTextButton

@Composable
fun ReadOnlyMainClientSection(
    clientId: String?,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val client = clientId?.let { ClientStore.findById(it) }
    Column(modifier = modifier) {
        if (showLabel) {
            GlideFieldLabel("Main client")
            Spacer(modifier = Modifier.height(2.dp))
        }
        if (client == null) {
            Text(
                text = "No main client linked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = formatPersonLabel(client.name, client.dateOfBirth),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (client.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = client.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (client.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = client.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Edit this client's details in the Clients panel.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ReadOnlyStudentsSection(
    studentIds: List<String>,
    showLabel: Boolean = true,
) {
    val people = studentIds.mapNotNull { StudentStore.findById(it) }
    if (showLabel) {
        Text(
            text = "Students",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
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
fun ReadOnlyPlanSection(
    planId: String?,
    prominent: Boolean = false,
) {
    val plan = planId?.let { PlanStore.findById(it) }
    if (prominent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (plan != null) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Plan",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (plan != null) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (plan != null) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = plan.summaryLine(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                )
            } else {
                Text(
                    text = "No plan assigned",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        GlideFieldLabel("Plan")
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = plan?.let { "${it.name} (${it.summaryLine()})" } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun LeadStudentLinkedRow(
    person: Student,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                MaterialTheme.shapes.small,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatPersonLabel(person.name, person.dateOfBirth),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        GlideTextButton(onClick = onRemove) {
            Text("Remove", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun StudentLinkSection(
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    var studentSearchQuery by remember { mutableStateOf("") }
    val linked = selectedIds.mapNotNull { StudentStore.findById(it) }
    val studentSearchResults = remember(studentSearchQuery, selectedIds, StudentStore.all) {
        val query = studentSearchQuery.trim()
        StudentStore.all
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

    Text(
        text = "Students",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(spacing.field))

    linked.forEach { person ->
        LeadStudentLinkedRow(
            person = person,
            onRemove = { onSelectionChange(selectedIds.filter { it != person.id }) },
        )
        Spacer(modifier = Modifier.height(spacing.field))
    }

    EntitySearchPicker(
        label = "Search students",
        placeholder = "Name or date of birth",
        query = studentSearchQuery,
        onQueryChange = { studentSearchQuery = it },
        results = studentSearchResults,
        onSelect = { id -> onSelectionChange(selectedIds + id) },
        noResultsText = "No matches. Add students on a lead first.",
    )

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Edit details in the Students panel once they are on a customer plan.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
