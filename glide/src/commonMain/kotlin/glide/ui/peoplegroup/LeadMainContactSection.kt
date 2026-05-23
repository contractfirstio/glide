package glide.ui.peoplegroup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import glide.data.ContactStore
import glide.model.Contact
import glide.ui.layout.GlideLayout
import glide.ui.leads.DateOfBirthField
import glide.ui.shared.formatPersonLabel
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton

@Composable
fun LeadMainContactSection(
    mainContactId: String?,
    contactName: String,
    dateOfBirth: String,
    email: String,
    phone: String,
    onStateChange: (mainContactId: String?, contactName: String, dateOfBirth: String, email: String, phone: String) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    var contactSearchQuery by remember { mutableStateOf("") }

    val contactResults = remember(contactSearchQuery, ContactStore.all) {
        val query = contactSearchQuery.trim()
        if (query.isEmpty()) {
            emptyList()
        } else {
            ContactStore.all
                .filter { contact ->
                    listOf(contact.name, contact.email, contact.phone, contact.dateOfBirth)
                        .any { it.matchesEntitySearch(query) }
                }
                .map { it.toSearchItem() }
        }
    }

    LeadPanelSection(
        title = "Main contact",
        description = "The primary person for this lead.",
        spacing = spacing,
    ) {
        if (mainContactId != null) {
            ReadOnlyMainContactSection(contactId = mainContactId, showLabel = false)
            Spacer(modifier = Modifier.height(spacing.field))
            GlideTextButton(
                onClick = {
                    onStateChange(null, "", "", "", "")
                    contactSearchQuery = ""
                },
            ) {
                Text("Choose a different contact")
            }
        } else {
            LeadActionSubsection(
                title = "Link existing contact",
                description = "Search contacts already saved from previous customers.",
                spacing = spacing,
            ) {
                EntitySearchPicker(
                    label = "Search saved contacts",
                    placeholder = "Type name, email, or phone…",
                    query = contactSearchQuery,
                    onQueryChange = { contactSearchQuery = it },
                    results = contactResults,
                    onSelect = { id ->
                        onStateChange(id, "", "", "", "")
                    },
                    noResultsText = "No saved contacts match. Create a new contact below instead.",
                )
            }

            Spacer(modifier = Modifier.height(spacing.section))

            LeadActionSubsection(
                title = "Create new contact",
                description = "For prospects who are not saved yet. They become a contact when you make a customer.",
                spacing = spacing,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    GlideOutlinedField(
                        value = contactName,
                        onValueChange = { onStateChange(null, it, dateOfBirth, email, phone) },
                        label = "New contact name",
                    )
                    Spacer(modifier = Modifier.height(spacing.field))
                    DateOfBirthField(
                        value = dateOfBirth,
                        onValueChange = { onStateChange(null, contactName, it, email, phone) },
                    )
                    Spacer(modifier = Modifier.height(spacing.field))
                    GlideOutlinedField(
                        value = email,
                        onValueChange = { onStateChange(null, contactName, dateOfBirth, it, phone) },
                        label = "New contact email",
                    )
                    Spacer(modifier = Modifier.height(spacing.field))
                    GlideOutlinedField(
                        value = phone,
                        onValueChange = { onStateChange(null, contactName, dateOfBirth, email, it) },
                        label = "New contact phone",
                    )
                }
            }
        }
    }
}

private fun Contact.toSearchItem() = SearchResultItem(
    id = id,
    primaryLabel = formatPersonLabel(name, dateOfBirth),
    secondaryLabel = listOf(email, phone).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() },
)
