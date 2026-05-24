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
import glide.data.LocationStore
import glide.data.ScheduledClassStore
import glide.data.SchedulePanelState
import glide.data.TermStore
import glide.model.ClassLocation
import glide.ui.layout.GlideLayout
import glide.ui.shared.DeleteConfirmDialog
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.rememberFormDirtyTracker
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton
import glide.ui.theme.glideListItemTitleColor
import java.util.UUID

private data class LocationFormState(
    val name: String = "",
    val maxCapacityText: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val notes: String = "",
) {
    fun parsedMaxCapacity(): Int? {
        val trimmed = maxCapacityText.trim()
        if (trimmed.isEmpty()) return null
        val value = trimmed.toIntOrNull() ?: return null
        return if (value > 0) value else null
    }

    fun isValid(): Boolean {
        if (name.isBlank()) return false
        if (maxCapacityText.isNotBlank() && parsedMaxCapacity() == null) return false
        return true
    }

    fun toLocation(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): ClassLocation = ClassLocation(
        id = existingId ?: UUID.randomUUID().toString(),
        name = name.trim(),
        maxCapacity = parsedMaxCapacity(),
        addressLine1 = addressLine1.trim(),
        addressLine2 = addressLine2.trim(),
        city = city.trim(),
        notes = notes.trim(),
        createdAtMillis = createdAtMillis,
    )
}

@Composable
fun LocationsPanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val form = rememberFormDirtyTracker(LocationFormState())
    var isCreating by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val allLocations = LocationStore.sortedForPanel()
    val termFilterId = SchedulePanelState.selectedTermFilterId
    val locationFilterId = SchedulePanelState.selectedLocationFilterId
    val classSyncId = SchedulePanelState.selectedClassId.takeIf {
        termFilterId == null && locationFilterId == null
    }
    val locations = if (termFilterId != null) {
        val locationIds = ScheduledClassStore.locationIdsForTerm(termFilterId)
        allLocations.filter { it.id in locationIds }
    } else {
        allLocations
    }
    val termFilterLabel = termFilterId?.let {
        TermStore.findById(it)?.name?.takeIf { name -> name.isNotBlank() }
    }

    fun clearLocalSelection() {
        selectedId = null
        isCreating = true
        form.load(LocationFormState())
        formError = null
    }

    fun clearSelection() {
        clearLocalSelection()
        SchedulePanelState.onLocationCleared()
    }

    fun resetFormForCreate() {
        clearLocalSelection()
        SchedulePanelState.onLocationCleared()
    }

    fun syncLocationIntoForm(location: ClassLocation) {
        selectedId = location.id
        isCreating = false
        form.load(
            location.toFormState(),
        )
        formError = null
    }

    fun loadIntoForm(location: ClassLocation) {
        selectedId = location.id
        isCreating = false
        SchedulePanelState.onLocationSelected(location.id)
        form.load(
            location.toFormState(),
        )
        formError = null
    }

    LaunchedEffect(allLocations, selectedId, termFilterId) {
        if (selectedId != null && allLocations.none { it.id == selectedId }) {
            clearSelection()
        }
    }

    LaunchedEffect(classSyncId, allLocations) {
        val classId = classSyncId ?: return@LaunchedEffect
        val locationId = ScheduledClassStore.findById(classId)?.locationId ?: return@LaunchedEffect
        allLocations.find { it.id == locationId }?.let { syncLocationIntoForm(it) }
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
                        termFilterId != null -> {
                            val label = termFilterLabel ?: "this term"
                            "Showing locations used in $label. Use Clear filter in Terms to reset."
                        }
                        else ->
                            "Define rooms, studios, and capacity, then assign locations to classes in the Schedule panel."
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
                            text = "${locations.size} location${if (locations.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.field)) {
                            if (termFilterId != null) {
                                GlideTextButton(onClick = { SchedulePanelState.clearTermFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            GlideButton(onClick = { resetFormForCreate() }) {
                                Text(if (compact) "New" else "New location")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.field))

                    if (locations.isEmpty()) {
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
                                    termFilterId != null -> "No locations used in this term."
                                    else -> "No locations yet."
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
                            items(locations, key = { it.id }) { location ->
                                LocationListItem(
                                    location = location,
                                    selected = location.id == selectedId,
                                    compact = compact,
                                    onClick = { loadIntoForm(location) },
                                )
                            }
                        }
                    }
                }
            }

            val formSection: @Composable (Modifier) -> Unit = { formModifier ->
                Column(modifier = formModifier.fillMaxHeight()) {
                    Text(
                        text = if (isCreating) "Create location" else "Edit location",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(spacing.section))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        LocationForm(
                            state = form.draft,
                            onStateChange = { form.draft = it },
                            spacing = spacing,
                        )

                        if (!isCreating && selectedId != null) {
                            val classCount = LocationStore.classCount(selectedId!!)
                            if (classCount > 0) {
                                Spacer(modifier = Modifier.height(spacing.field))
                                Text(
                                    text = "Used by $classCount class${if (classCount == 1) "" else "es"}. " +
                                        "Remove this location from those classes before deleting.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                                    formError = "Location name is required. Capacity must be a positive number if set."
                                    return@GlideButton
                                }
                                formError = null
                                if (isCreating) {
                                    val location = form.draft.toLocation()
                                    LocationStore.create(location)
                                    loadIntoForm(location)
                                } else {
                                    val existing = selectedId?.let { LocationStore.findById(it) }
                                    if (existing != null) {
                                        val updated = form.draft.toLocation(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                        )
                                        LocationStore.update(updated)
                                        loadIntoForm(updated)
                                    }
                                }
                            },
                            enabled = form.isDirty,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(saveLabel)
                        }

                        if (!isCreating) {
                            GlideOutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                enabled = selectedId?.let { LocationStore.canDelete(it) } == true,
                            ) {
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
        val linkedClasses = LocationStore.classCount(selectedId!!)
        val attachedToClass = linkedClasses > 0
        DeleteConfirmDialog(
            title = "Delete location?",
            message = if (attachedToClass) {
                "This location is used by $linkedClasses class${if (linkedClasses == 1) "" else "es"} " +
                    "and cannot be deleted. Remove it from those classes first."
            } else {
                "This location will be removed permanently."
            },
            onDismiss = { showDeleteConfirm = false },
            onContinue = {
                if (LocationStore.delete(selectedId!!)) {
                    showDeleteConfirm = false
                    clearSelection()
                } else {
                    showDeleteConfirm = false
                    formError = "This location is attached to a class and cannot be deleted."
                }
            },
            continueEnabled = !attachedToClass,
        )
    }
}

@Composable
private fun LocationListItem(
    location: ClassLocation,
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
            text = location.name,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = glideListItemTitleColor(selected),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        location.maxCapacity?.let { max ->
            Text(
                text = "Max $max",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val addressPreview = location.formattedAddressLines().joinToString(", ")
        if (addressPreview.isNotBlank()) {
            Text(
                text = addressPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (location.notes.isNotBlank()) {
            Text(
                text = location.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val classCount = ScheduledClassStore.countForLocation(location.id)
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
private fun LocationForm(
    state: LocationFormState,
    onStateChange: (LocationFormState) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    FormPanelSection(
        title = "Location",
        description = "Room or venue name used on classes.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
        GlideOutlinedField(
            value = state.name,
            onValueChange = { onStateChange(state.copy(name = it)) },
            label = "Location name",
            placeholder = "e.g. Studio A",
        )
    }

    FormPanelSectionsDivider(label = "Address", spacing = spacing)

    FormPanelSection(
        title = "Venue address",
        description = "Printed on invoices so customers know where classes take place.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        GlideOutlinedField(
            value = state.addressLine1,
            onValueChange = { onStateChange(state.copy(addressLine1 = it)) },
            label = "Address line 1",
            placeholder = "e.g. 12 High Street",
        )
        Spacer(modifier = Modifier.height(spacing.field))
        GlideOutlinedField(
            value = state.addressLine2,
            onValueChange = { onStateChange(state.copy(addressLine2 = it)) },
            label = "Address line 2",
            placeholder = "e.g. Unit 3",
        )
        Spacer(modifier = Modifier.height(spacing.field))
        GlideOutlinedField(
            value = state.city,
            onValueChange = { onStateChange(state.copy(city = it)) },
            label = "District / area",
            placeholder = "e.g. Central",
        )
    }

    FormPanelSectionsDivider(label = "Capacity", spacing = spacing)

    FormPanelSection(
        title = "Room capacity",
        description = "Maximum students for classes at this location.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        GlideOutlinedField(
            value = state.maxCapacityText,
            onValueChange = { onStateChange(state.copy(maxCapacityText = it)) },
            label = "Max capacity",
            placeholder = "e.g. 20",
        )
    }

    FormPanelSectionsDivider(label = "Notes", spacing = spacing)

    FormPanelSection(
        title = "Notes",
        description = "Internal notes about this location.",
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

private fun ClassLocation.toFormState(): LocationFormState = LocationFormState(
    name = name,
    maxCapacityText = maxCapacity?.toString() ?: "",
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    city = city,
    notes = notes,
)
