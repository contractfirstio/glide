package glide.ui.scheduling

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import glide.data.ClassGroupAssignmentStore
import glide.data.LocationStore
import glide.data.PeopleGroupStore
import glide.data.ScheduledClassStore
import glide.data.TermStore
import glide.data.memberCount
import glide.data.resolveMainContact
import glide.data.toUserMessage
import glide.model.ClassLocation
import glide.model.DayOfWeek
import glide.model.ScheduledClass
import glide.model.compareTime24h
import glide.model.isValidTime24h
import glide.model.scheduleLine
import glide.ui.layout.GlideLayout
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.peoplegroup.EntitySearchPicker
import glide.ui.peoplegroup.SearchResultItem
import glide.ui.peoplegroup.matchesEntitySearch
import glide.ui.shared.formatPersonLabel
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideFieldLabel
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideOutlinedField
import glide.ui.theme.GlideTextButton
import glide.ui.theme.glideListItemTitleColor
import java.util.UUID

private data class ClassFormState(
    val name: String = "",
    val termIds: Set<String> = emptySet(),
    val locationId: String? = null,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val notes: String = "",
) {
    fun isValid(): Boolean {
        if (name.isBlank()) return false
        if (!isValidTime24h(startTime) || !isValidTime24h(endTime)) return false
        return compareTime24h(startTime, endTime) < 0
    }

    fun toScheduledClass(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): ScheduledClass = ScheduledClass(
        id = existingId ?: UUID.randomUUID().toString(),
        name = name.trim(),
        termIds = termIds.toList(),
        locationId = locationId,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        notes = notes.trim(),
        createdAtMillis = createdAtMillis,
    )
}

@Composable
fun SchedulePanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var formState by remember { mutableStateOf(ClassFormState()) }
    var isCreating by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val terms = TermStore.sortedForPanel()
    val locations = LocationStore.sortedForPanel()
    val classes = ScheduledClassStore.forSchedulePanel()

    fun clearSelection() {
        selectedId = null
        isCreating = true
        formState = ClassFormState()
        formError = null
    }

    fun resetFormForCreate() {
        selectedId = null
        isCreating = true
        formState = ClassFormState()
        formError = null
    }

    fun loadIntoForm(scheduledClass: ScheduledClass) {
        selectedId = scheduledClass.id
        isCreating = false
        formState = ClassFormState(
            name = scheduledClass.name,
            termIds = scheduledClass.termIds.toSet(),
            locationId = scheduledClass.locationId,
            dayOfWeek = scheduledClass.dayOfWeek,
            startTime = scheduledClass.startTime,
            endTime = scheduledClass.endTime,
            notes = scheduledClass.notes,
        )
        formError = null
    }

    LaunchedEffect(classes, selectedId) {
        if (selectedId != null && classes.none { it.id == selectedId }) {
            clearSelection()
        }
    }

    LaunchedEffect(locations, formState.locationId) {
        val locationId = formState.locationId ?: return@LaunchedEffect
        if (locations.none { it.id == locationId }) {
            formState = formState.copy(locationId = null)
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
                    text = when {
                        terms.isEmpty() && locations.isEmpty() ->
                            "Create terms and locations in their panels, then add recurring classes here."
                        terms.isEmpty() ->
                            "Create terms in the Terms panel, then add recurring classes here."
                        locations.isEmpty() ->
                            "Define recurring classes. Add locations in the Locations panel to assign rooms."
                        else ->
                            "Define recurring classes and link them to terms and locations."
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
                            text = "${classes.size} class${if (classes.size == 1) "" else "es"}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        GlideButton(onClick = { resetFormForCreate() }) {
                            Text(if (compact) "New" else "New class")
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.field))

                    if (classes.isEmpty()) {
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
                                text = "No classes yet.",
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
                            items(classes, key = { it.id }) { scheduledClass ->
                                ClassListItem(
                                    scheduledClass = scheduledClass,
                                    selected = scheduledClass.id == selectedId,
                                    compact = compact,
                                    onClick = { loadIntoForm(scheduledClass) },
                                )
                            }
                        }
                    }
                }
            }

            val formSection: @Composable (Modifier) -> Unit = { formModifier ->
                Column(modifier = formModifier.fillMaxHeight()) {
                    Text(
                        text = if (isCreating) "Create class" else "Edit class",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(spacing.section))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ClassForm(
                            state = formState,
                            onStateChange = { formState = it },
                            terms = terms,
                            locations = locations,
                            spacing = spacing,
                        )

                        if (!isCreating && selectedId != null) {
                            Spacer(modifier = Modifier.height(spacing.field))
                            ClassCustomerGroupsSection(
                                classId = selectedId!!,
                                spacing = spacing,
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
                                    formError = "Name, valid start/end times (HH:MM), and end after start are required."
                                    return@GlideButton
                                }
                                formError = null
                                if (isCreating) {
                                    val scheduledClass = formState.toScheduledClass()
                                    ScheduledClassStore.create(scheduledClass)
                                    loadIntoForm(scheduledClass)
                                } else {
                                    val existing = selectedId?.let { ScheduledClassStore.findById(it) }
                                    if (existing != null) {
                                        val updated = formState.toScheduledClass(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                        )
                                        ScheduledClassStore.update(updated)
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete class?") },
            text = { Text("This class will be removed permanently.") },
            confirmButton = {
                GlideTextButton(
                    onClick = {
                        ScheduledClassStore.delete(selectedId!!)
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
private fun ClassListItem(
    scheduledClass: ScheduledClass,
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
    val termNames = scheduledClass.termIds.mapNotNull { TermStore.findById(it)?.name }
    val location = scheduledClass.locationId?.let { LocationStore.findById(it) }
    val locationLine = location?.let { loc ->
        listOfNotNull(loc.name, loc.maxCapacity?.let { "Max $it" }).joinToString(" · ")
    }
    val groupCount = ClassGroupAssignmentStore.groupsForClass(scheduledClass.id).size
    val enrolledCount = ClassGroupAssignmentStore.headcountForClass(scheduledClass.id)
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
            text = scheduledClass.name,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = glideListItemTitleColor(selected),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = scheduledClass.scheduleLine(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!locationLine.isNullOrBlank()) {
            Text(
                text = locationLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (termNames.isNotEmpty()) {
            Text(
                text = termNames.joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (groupCount > 0) {
            val capacitySuffix = location?.maxCapacity?.let { max ->
                " · $enrolledCount/$max"
            } ?: if (enrolledCount > 0) " · $enrolledCount enrolled" else ""
            Text(
                text = "$groupCount group${if (groupCount == 1) "" else "s"}$capacitySuffix",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClassCustomerGroupsSection(
    classId: String,
    spacing: GlideLayout.Spacing,
) {
    var searchQuery by remember(classId) { mutableStateOf("") }
    var enrollmentMessage by remember(classId) { mutableStateOf<String?>(null) }

    val assignedIds = ClassGroupAssignmentStore.groupsForClass(classId)
    val scheduledClass = ScheduledClassStore.findById(classId)
    val location = scheduledClass?.locationId?.let { LocationStore.findById(it) }
    val headcount = ClassGroupAssignmentStore.headcountForClass(classId)
    val assignmentRevision = ClassGroupAssignmentStore.assignments.size

    val searchResults = remember(searchQuery, assignedIds, assignmentRevision) {
        PeopleGroupStore.customers
            .filter { it.id !in assignedIds }
            .filter { group ->
                val main = group.resolveMainContact()
                searchQuery.isBlank() ||
                    main.name.matchesEntitySearch(searchQuery) ||
                    main.email.matchesEntitySearch(searchQuery)
            }
            .map { group ->
                val main = group.resolveMainContact()
                SearchResultItem(
                    id = group.id,
                    primaryLabel = formatPersonLabel(main.name, main.dateOfBirth),
                    secondaryLabel = "${group.memberCount()} people",
                )
            }
    }

    Column {
        GlideFieldLabel("Customer groups")
        Spacer(modifier = Modifier.height(2.dp))
        ClassCapacityGraphic(
            occupiedCount = headcount,
            maxCapacity = location?.maxCapacity,
        )
        Spacer(modifier = Modifier.height(spacing.section))
        EntitySearchPicker(
            label = "Add customer group",
            placeholder = "Search by name or email…",
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            results = searchResults,
            onSelect = { groupId ->
                val result = ClassGroupAssignmentStore.assign(classId, groupId)
                enrollmentMessage = result.toUserMessage()
            },
            noResultsText = "No matching customer groups.",
        )
        enrollmentMessage?.let { message ->
            Spacer(modifier = Modifier.height(spacing.field))
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = if (message.contains("exceeded") || message.contains("already")) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
        if (assignedIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.section))
            Text(
                text = "On this class",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(spacing.field))
            assignedIds.forEach { groupId ->
                val group = PeopleGroupStore.findById(groupId) ?: return@forEach
                val main = group.resolveMainContact()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatPersonLabel(main.name, main.dateOfBirth),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${group.memberCount()} people",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    GlideTextButton(
                        onClick = {
                            ClassGroupAssignmentStore.unassign(classId, groupId)
                            enrollmentMessage = "Customer group removed from class."
                        },
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassForm(
    state: ClassFormState,
    onStateChange: (ClassFormState) -> Unit,
    terms: List<glide.model.AcademicTerm>,
    locations: List<ClassLocation>,
    spacing: GlideLayout.Spacing,
) {
    var dayExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }

    GlideOutlinedField(
        value = state.name,
        onValueChange = { onStateChange(state.copy(name = it)) },
        label = "Class name",
        placeholder = "e.g. Tuesday Beginner Ballet",
    )
    Spacer(modifier = Modifier.height(spacing.field))

    Column {
        GlideFieldLabel("Terms")
        Spacer(modifier = Modifier.height(2.dp))
        if (terms.isEmpty()) {
            Text(
                text = "Create terms in the Terms panel, then select which terms this class runs in.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "Select all terms this class spans (e.g. rolling classes across seasons).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(spacing.field))
            terms.forEach { term ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = if (term.id in state.termIds) {
                                state.termIds - term.id
                            } else {
                                state.termIds + term.id
                            }
                            onStateChange(state.copy(termIds = next))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = term.id in state.termIds,
                        onCheckedChange = { checked ->
                            val next = if (checked) {
                                state.termIds + term.id
                            } else {
                                state.termIds - term.id
                            }
                            onStateChange(state.copy(termIds = next))
                        },
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = term.name,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val dateLine = buildString {
                            if (term.startDate.isNotBlank()) append(formatIsoDateForDisplay(term.startDate))
                            if (term.startDate.isNotBlank() && term.endDate.isNotBlank()) append(" – ")
                            if (term.endDate.isNotBlank()) append(formatIsoDateForDisplay(term.endDate))
                        }
                        if (dateLine.isNotBlank()) {
                            Text(
                                text = dateLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(spacing.field))

    Column {
        GlideFieldLabel("Location")
        Spacer(modifier = Modifier.height(2.dp))
        if (locations.isEmpty()) {
            Text(
                text = "Create locations in the Locations panel, then assign a room to this class.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val selectedLocationLabel = state.locationId?.let { id ->
                locations.find { it.id == id }?.name
            } ?: "None"
            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedLocationLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = GlideDimensions.fieldHeight)
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = locationExpanded,
                    onDismissRequest = { locationExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("None", style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            onStateChange(state.copy(locationId = null))
                            locationExpanded = false
                        },
                    )
                    locations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location.name, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onStateChange(state.copy(locationId = location.id))
                                locationExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(spacing.field))

    Column {
        GlideFieldLabel("Day")
        Spacer(modifier = Modifier.height(2.dp))
        ExposedDropdownMenuBox(
            expanded = dayExpanded,
            onExpandedChange = { dayExpanded = it },
        ) {
            OutlinedTextField(
                value = state.dayOfWeek.label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = GlideDimensions.fieldHeight)
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = dayExpanded,
                onDismissRequest = { dayExpanded = false },
            ) {
                DayOfWeek.entries.forEach { day ->
                    DropdownMenuItem(
                        text = { Text(day.label, style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            onStateChange(state.copy(dayOfWeek = day))
                            dayExpanded = false
                        },
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(spacing.field))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.field),
    ) {
        GlideOutlinedField(
            value = state.startTime,
            onValueChange = { onStateChange(state.copy(startTime = it)) },
            label = "Start time",
            placeholder = "09:00",
            modifier = Modifier.weight(1f),
        )
        GlideOutlinedField(
            value = state.endTime,
            onValueChange = { onStateChange(state.copy(endTime = it)) },
            label = "End time",
            placeholder = "10:00",
            modifier = Modifier.weight(1f),
        )
    }
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
