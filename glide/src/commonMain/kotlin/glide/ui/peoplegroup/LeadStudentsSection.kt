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
import glide.data.StudentStore
import glide.model.Student
import glide.ui.layout.GlideLayout
import glide.ui.leads.DateOfBirthField
import glide.ui.shared.formatPersonLabel
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField

@Composable
fun LeadStudentsSection(
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    var studentSearchQuery by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newDateOfBirth by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }

    val linked = selectedIds.mapNotNull { StudentStore.findById(it) }
    val studentSearchResults = remember(studentSearchQuery, selectedIds, StudentStore.all) {
        val query = studentSearchQuery.trim()
        if (query.isEmpty()) {
            emptyList()
        } else {
            StudentStore.all
                .filter { it.id !in selectedIds }
                .filter { person ->
                    listOf(person.name, person.dateOfBirth, person.notes)
                        .any { it.matchesEntitySearch(query) }
                }
                .map { it.toSearchItem() }
        }
    }

    LeadPanelSection(
        title = "Students",
        description = "Family or others on this lead — separate from the main client.",
        spacing = spacing,
        role = LeadPanelSectionRole.Secondary,
    ) {
        if (linked.isNotEmpty()) {
            LeadStudentsLinkedBox {
                GlideFieldLabel("On this lead (${linked.size})")
                Spacer(modifier = Modifier.height(spacing.field))
                linked.forEachIndexed { index, person ->
                    LeadStudentLinkedRow(
                        person = person,
                        onRemove = { onSelectionChange(selectedIds.filter { it != person.id }) },
                    )
                    if (index < linked.lastIndex) {
                        Spacer(modifier = Modifier.height(spacing.field))
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.section))
        }

        LeadActionSubsection(
            title = "Link existing student",
            description = "Search people already added on other leads or customer plans.",
            spacing = spacing,
        ) {
            EntitySearchPicker(
                label = "Search saved students",
                placeholder = "Type name or date of birth…",
                query = studentSearchQuery,
                onQueryChange = { studentSearchQuery = it },
                results = studentSearchResults,
                onSelect = { id -> onSelectionChange(selectedIds + id) },
                noResultsText = "No saved people match. Create a new student below instead.",
            )
        }

        Spacer(modifier = Modifier.height(spacing.section))

        LeadActionSubsection(
            title = "Create new student",
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
                        val person = Student(name = trimmed, dateOfBirth = newDateOfBirth.trim())
                        StudentStore.create(person)
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
            text = "Edit names and details in the Students panel once they are on a customer plan.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Student.toSearchItem() = SearchResultItem(
    id = id,
    primaryLabel = formatPersonLabel(name, dateOfBirth),
    secondaryLabel = notes.takeIf { it.isNotBlank() },
)
