package glide.ui.people

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
import glide.data.BillingPanelState
import glide.data.ContactStore
import glide.data.ContactsPanelState
import glide.data.PeopleGroupStore
import glide.data.PlanStore
import glide.data.PlansPanelState
import glide.data.RelatedPanelState
import glide.data.RelatedPersonStore
import glide.data.resolveMainContact
import glide.model.Contact
import glide.ui.layout.GlideLayout
import glide.ui.shared.DeleteConfirmDialog
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.rememberFormDirtyTracker
import glide.ui.leads.DateOfBirthField
import glide.ui.shared.formatPersonLabel
import glide.ui.theme.GlideButton
import glide.ui.theme.glideListItemTitleColor
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton
import java.util.UUID

private data class ContactFormState(
    val name: String = "",
    val dateOfBirth: String = "",
    val email: String = "",
    val phone: String = "",
    val notes: String = "",
) {
    fun isValid(): Boolean = name.isNotBlank()

    fun toContact(existingId: String? = null, createdAtMillis: Long = System.currentTimeMillis()): Contact =
        Contact(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name.trim(),
            dateOfBirth = dateOfBirth.trim(),
            email = email.trim(),
            phone = phone.trim(),
            notes = notes.trim(),
            createdAtMillis = createdAtMillis,
        )
}

@Composable
fun PeoplePanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val form = rememberFormDirtyTracker(ContactFormState())
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val customerGroupId = BillingPanelState.peopleGroupId
    val contactFilterId = ContactsPanelState.selectedContactId
    val planFilterId = PlansPanelState.selectedPlanId
    val relatedFilterId = RelatedPanelState.selectedRelatedPersonId
    val contacts = ContactStore.forContactsPanel(customerGroupId, planFilterId, relatedFilterId)
    val customerGroupLabel = customerGroupId?.let { id ->
        PeopleGroupStore.findById(id)?.resolveMainContact()?.name?.takeIf { it.isNotBlank() }
    }
    val contactFilterLabel = contactFilterId?.let { ContactStore.findById(it)?.name?.takeIf { it.isNotBlank() } }
    val planFilterLabel = planFilterId?.let { PlanStore.findById(it)?.name?.takeIf { it.isNotBlank() } }
    val relatedFilterLabel = relatedFilterId?.let {
        RelatedPersonStore.findById(it)?.name?.takeIf { it.isNotBlank() }
    }

    fun clearLocalSelection() {
        selectedId = null
        form.load(ContactFormState())
        formError = null
    }

    fun clearSelection() {
        clearLocalSelection()
        ContactsPanelState.clearContactFilter()
    }

    LaunchedEffect(customerGroupId, planFilterId, relatedFilterId) {
        if ((customerGroupId != null || planFilterId != null || relatedFilterId != null) && selectedId != null) {
            clearLocalSelection()
        }
    }

    LaunchedEffect(contacts, selectedId) {
        if (selectedId != null && contacts.none { it.id == selectedId }) {
            clearSelection()
        }
    }

    fun loadIntoForm(contact: Contact) {
        selectedId = contact.id
        ContactsPanelState.onContactSelected(contact.id)
        form.load(
            ContactFormState(
                name = contact.name,
                dateOfBirth = contact.dateOfBirth,
                email = contact.email,
                phone = contact.phone,
                notes = contact.notes,
            ),
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
                    text = when {
                        customerGroupId != null -> {
                            val label = customerGroupLabel ?: "this customer group"
                            "Showing main contact for $label. Use Show all to reset."
                        }
                        contactFilterId != null -> {
                            val label = contactFilterLabel ?: "this contact"
                            "Filtering customer groups for $label. Use Clear filter to reset."
                        }
                        planFilterId != null -> {
                            val label = planFilterLabel ?: "this plan"
                            "Showing contacts on groups with $label. Use Clear filter in Plans to reset."
                        }
                        relatedFilterId != null -> {
                            val label = relatedFilterLabel ?: "this related person"
                            "Showing contacts linked to $label. Use Clear filter in Related to reset."
                        }
                        else ->
                            "Edit contacts created from leads. New contacts are added when you make a customer on a lead."
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
                            text = "${contacts.size} contact${if (contacts.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.field)) {
                            if (contactFilterId != null) {
                                GlideTextButton(onClick = { ContactsPanelState.clearContactFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (planFilterId != null) {
                                GlideTextButton(onClick = { PlansPanelState.clearPlanFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (relatedFilterId != null) {
                                GlideTextButton(onClick = { RelatedPanelState.clearRelatedPersonFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            if (customerGroupId != null) {
                                GlideTextButton(onClick = { BillingPanelState.onCustomerGroupCleared() }) {
                                    Text("Show all")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.field))

                    if (contacts.isEmpty()) {
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
                                    customerGroupId != null ->
                                        "No main contact for this customer group."
                                    planFilterId != null ->
                                        "No contacts on groups with this plan."
                                    relatedFilterId != null ->
                                        "No contacts linked to this related person."
                                    else ->
                                        "No contacts yet. Contacts are created when leads become customers."
                                },
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
                            items(contacts, key = { it.id }) { contact ->
                                ContactListItem(
                                    contact = contact,
                                    selected = contact.id == selectedId,
                                    compact = compact,
                                    onClick = { loadIntoForm(contact) },
                                )
                            }
                        }
                    }
                }
            }

            val formSection: @Composable (Modifier) -> Unit = { formModifier ->
                Column(modifier = formModifier.fillMaxHeight()) {
                    Text(
                        text = "Edit contact",
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
                                text = "Select a contact to edit. Contacts are created from the Leads panel.",
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
                            ContactForm(
                                state = form.draft,
                                onStateChange = { form.draft = it },
                                spacing = spacing,
                            )

                            val soldPlanCount = selectedId?.let { ContactStore.soldPlanCount(it) } ?: 0
                            if (soldPlanCount > 0) {
                                Spacer(modifier = Modifier.height(spacing.field))
                                Text(
                                    text = "On $soldPlanCount sold plan${if (soldPlanCount == 1) "" else "s"}.",
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
                                    if (!form.draft.isValid()) {
                                        formError = "Name is required."
                                        return@GlideButton
                                    }
                                    formError = null
                                    val existing = selectedId?.let { ContactStore.findById(it) }
                                    if (existing != null) {
                                        val updated = form.draft.toContact(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                        )
                                        ContactStore.update(updated)
                                        loadIntoForm(updated)
                                    }
                                },
                                enabled = form.isDirty,
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
        val onSoldPlan = ContactStore.isOnSoldPlan(selectedId!!)
        DeleteConfirmDialog(
            title = "Delete contact?",
            message = if (onSoldPlan) {
                "This person is the main contact on one or more sold plans and cannot be deleted."
            } else {
                "This contact will be removed permanently."
            },
            onDismiss = { showDeleteConfirm = false },
            onContinue = {
                if (!onSoldPlan) {
                    ContactStore.delete(selectedId!!)
                    showDeleteConfirm = false
                    clearSelection()
                } else {
                    showDeleteConfirm = false
                    formError = "This person is on a sold plan and cannot be deleted."
                }
            },
            continueEnabled = !onSoldPlan,
        )
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
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
    val soldPlanCount = ContactStore.soldPlanCount(contact.id)
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
            text = formatPersonLabel(contact.name, contact.dateOfBirth),
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = glideListItemTitleColor(selected),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (contact.email.isNotBlank() && !compact) {
            Text(
                text = contact.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (soldPlanCount > 0) {
            Text(
                text = "$soldPlanCount sold plan${if (soldPlanCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ContactForm(
    state: ContactFormState,
    onStateChange: (ContactFormState) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    FormPanelSection(
        title = "Identity",
        description = "Name and date of birth for this contact.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
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
    }

    FormPanelSectionsDivider(label = "Contact details", spacing = spacing)

    FormPanelSection(
        title = "Reach",
        description = "Email and phone for this person.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        GlideOutlinedField(
            value = state.email,
            onValueChange = { onStateChange(state.copy(email = it)) },
            label = "Email",
        )
        Spacer(modifier = Modifier.height(spacing.field))
        GlideOutlinedField(
            value = state.phone,
            onValueChange = { onStateChange(state.copy(phone = it)) },
            label = "Phone",
        )
    }

    FormPanelSectionsDivider(label = "Notes", spacing = spacing)

    FormPanelSection(
        title = "Notes",
        description = "Internal notes about this contact.",
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
        )
    }
}
