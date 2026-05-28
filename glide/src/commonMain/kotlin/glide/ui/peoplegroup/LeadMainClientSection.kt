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
import glide.data.ClientStore
import glide.model.Client
import glide.ui.layout.GlideLayout
import glide.ui.leads.DateOfBirthField
import glide.ui.shared.formatPersonLabel
import glide.ui.shared.FormValidationAnchor
import glide.ui.shared.FormValidationState
import glide.ui.shared.ValidatedGlideOutlinedField
import glide.ui.theme.GlideTextButton

@Composable
fun LeadMainClientSection(
    mainClientId: String?,
    clientName: String,
    dateOfBirth: String,
    email: String,
    phone: String,
    onStateChange: (mainClientId: String?, clientName: String, dateOfBirth: String, email: String, phone: String) -> Unit,
    spacing: GlideLayout.Spacing,
    validation: FormValidationState,
) {
    var clientSearchQuery by remember { mutableStateOf("") }

    val clientResults = remember(clientSearchQuery, ClientStore.all) {
        val query = clientSearchQuery.trim()
        ClientStore.all
            .filter { client ->
                listOf(client.name, client.email, client.phone, client.dateOfBirth)
                    .any { it.matchesEntitySearch(query) }
            }
            .map { it.toSearchItem() }
    }

    LeadPanelSection(
        title = "Main client",
        description = "The primary person for this lead — one per lead.",
        spacing = spacing,
        role = LeadPanelSectionRole.Primary,
    ) {
        if (mainClientId != null) {
            LeadMainClientSummaryCard {
                ReadOnlyMainClientSection(clientId = mainClientId, showLabel = false)
            }
            Spacer(modifier = Modifier.height(spacing.field))
            GlideTextButton(
                onClick = {
                    onStateChange(null, "", "", "", "")
                    clientSearchQuery = ""
                },
            ) {
                Text("Choose a different client")
            }
        } else {
            LeadActionSubsection(
                title = "Link existing client",
                description = "Search clients already saved from previous customers.",
                spacing = spacing,
            ) {
                FormValidationAnchor(validation = validation, fieldKey = "clientLink") {
                    EntitySearchPicker(
                        label = "Search saved clients",
                        placeholder = "Type name, email, or phone…",
                        query = clientSearchQuery,
                        onQueryChange = {
                            clientSearchQuery = it
                            validation.clearKey("clientLink")
                            validation.clearKey("clientName")
                        },
                        results = clientResults,
                        onSelect = { id ->
                            onStateChange(id, "", "", "", "")
                            validation.clearKey("clientLink")
                            validation.clearKey("clientName")
                        },
                        noResultsText = "No saved clients match. Create a new client below instead.",
                        isError = validation.isInvalid("clientLink"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.section))

            LeadActionSubsection(
                title = "Create new client",
                description = "For prospects who are not saved yet. They become a client when you make a customer.",
                spacing = spacing,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    validation.ValidatedGlideOutlinedField(
                        fieldKey = "clientName",
                        value = clientName,
                        onValueChange = { onStateChange(null, it, dateOfBirth, email, phone) },
                        label = "New client name",
                        required = true,
                    )
                    Spacer(modifier = Modifier.height(spacing.field))
                    DateOfBirthField(
                        value = dateOfBirth,
                        onValueChange = { onStateChange(null, clientName, it, email, phone) },
                    )
                    Spacer(modifier = Modifier.height(spacing.field))
                    validation.ValidatedGlideOutlinedField(
                        fieldKey = "clientEmail",
                        value = email,
                        onValueChange = { onStateChange(null, clientName, dateOfBirth, it, phone) },
                        label = "New client email",
                        required = true,
                    )
                    Spacer(modifier = Modifier.height(spacing.field))
                    validation.ValidatedGlideOutlinedField(
                        fieldKey = "clientPhone",
                        value = phone,
                        onValueChange = { onStateChange(null, clientName, dateOfBirth, email, it) },
                        label = "New client phone",
                    )
                }
            }
        }
    }
}

private fun Client.toSearchItem() = SearchResultItem(
    id = id,
    primaryLabel = formatPersonLabel(name, dateOfBirth),
    secondaryLabel = listOf(email, phone).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() },
)
