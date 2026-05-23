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
import glide.data.LocationStore
import glide.data.ScheduledClassStore
import glide.model.ClassLocation
import glide.ui.layout.GlideLayout
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.FormPanelSectionsDivider
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
        notes = notes.trim(),
        createdAtMillis = createdAtMillis,
    )
}

@Composable
fun LocationsPanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var formState by remember { mutableStateOf(LocationFormState()) }
    var isCreating by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val locations = LocationStore.sortedForPanel()

    fun clearSelection() {
        selectedId = null
        isCreating = true
        formState = LocationFormState()
        formError = null
    }

    fun resetFormForCreate() {
        selectedId = null
        isCreating = true
        formState = LocationFormState()
        formError = null
    }

    fun loadIntoForm(location: ClassLocation) {
        selectedId = location.id
        isCreating = false
        formState = LocationFormState(
            name = location.name,
            maxCapacityText = location.maxCapacity?.toString() ?: "",
            notes = location.notes,
        )
        formError = null
    }

    LaunchedEffect(locations, selectedId) {
        if (selectedId != null && locations.none { it.id == selectedId }) {
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
                    text = "Define rooms, studios, and capacity, then assign locations to classes in the Schedule panel.",
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
                        GlideButton(onClick = { resetFormForCreate() }) {
                            Text(if (compact) "New" else "New location")
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
                                text = "No locations yet.",
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
                                    formError = "Location name is required. Capacity must be a positive number if set."
                                    return@GlideButton
                                }
                                formError = null
                                if (isCreating) {
                                    val location = formState.toLocation()
                                    LocationStore.create(location)
                                    loadIntoForm(location)
                                } else {
                                    val existing = selectedId?.let { LocationStore.findById(it) }
                                    if (existing != null) {
                                        val updated = formState.toLocation(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                        )
                                        LocationStore.update(updated)
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
        val linkedClasses = ScheduledClassStore.countForLocation(selectedId!!)
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete location?") },
            text = {
                Text(
                    if (linkedClasses > 0) {
                        "This location will be removed. $linkedClasses class${if (linkedClasses == 1) "" else "es"} " +
                            "will be unlinked from this location."
                    } else {
                        "This location will be removed permanently."
                    },
                )
            },
            confirmButton = {
                GlideTextButton(
                    onClick = {
                        LocationStore.delete(selectedId!!)
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
