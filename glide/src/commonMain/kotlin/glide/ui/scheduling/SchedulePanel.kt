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
import glide.data.LocationStore
import glide.data.enrolledHeadcount
import glide.data.headcountForCustomerGroups
import glide.data.AddCustomerGroupResult
import glide.data.tryAddCustomerGroup
import glide.data.PeopleGroupStore
import glide.data.ScheduledClassStore
import glide.data.TermStore
import glide.data.classAttendeeCount
import glide.data.memberCount
import glide.data.resolveMainContact
import glide.data.toUserMessage
import glide.model.ClassLocation
import glide.model.ClassScheduleKind
import glide.model.DayOfWeek
import glide.model.ScheduledClass
import glide.model.compareTime24h
import glide.model.isValidTime24h
import glide.model.parseScheduleIsoDate
import glide.model.scheduleKind
import glide.model.scheduleLine
import glide.model.toModelDayOfWeek
import glide.ui.shared.IsoDateField
import glide.ui.leads.parseIsoDateToMillis
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
    val customerGroupIds: Set<String> = emptySet(),
    val locationId: String? = null,
    val scheduleKind: ClassScheduleKind = ClassScheduleKind.RECURRING,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val singleDate: String = "",
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val notes: String = "",
    val calendarColorArgb: Int? = null,
    val classId: String? = null,
) {
    fun isValid(): Boolean {
        if (name.isBlank()) return false
        if (!isValidTime24h(startTime) || !isValidTime24h(endTime)) return false
        if (compareTime24h(startTime, endTime) >= 0) return false
        return when (scheduleKind) {
            ClassScheduleKind.RECURRING -> true
            ClassScheduleKind.SINGLE_DAY -> parseIsoDateToMillis(singleDate) != null
        }
    }

    fun toScheduledClass(
        existingId: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): ScheduledClass {
        val resolvedSingleDate = when (scheduleKind) {
            ClassScheduleKind.RECURRING -> null
            ClassScheduleKind.SINGLE_DAY -> singleDate.trim().takeIf { it.isNotBlank() }
        }
        val resolvedDayOfWeek = resolvedSingleDate?.let { iso ->
            parseScheduleIsoDate(iso)?.dayOfWeek?.toModelDayOfWeek()
        } ?: dayOfWeek
        return ScheduledClass(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name.trim(),
            termIds = termIds.toList(),
            customerGroupIds = customerGroupIds.toList(),
            locationId = locationId,
            dayOfWeek = resolvedDayOfWeek,
            singleDate = resolvedSingleDate,
            startTime = startTime,
            endTime = endTime,
            notes = notes.trim(),
            calendarColorArgb = calendarColorArgb,
            createdAtMillis = createdAtMillis,
        )
    }

    fun withAutoTermForSingleDay(terms: List<glide.model.AcademicTerm>): ClassFormState {
        if (scheduleKind != ClassScheduleKind.SINGLE_DAY) return this
        if (singleDate.isBlank()) return copy(termIds = emptySet())
        val term = findTermContainingIsoDate(terms, singleDate)
        return copy(termIds = term?.let { setOf(it.id) } ?: emptySet())
    }

    companion object {
        fun defaultForCreate(terms: List<glide.model.AcademicTerm>): ClassFormState =
            ClassFormState(termIds = defaultRecurringClassTermIds(terms))
    }
}

@Composable
fun SchedulePanel(modifier: Modifier = Modifier) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val terms = TermStore.sortedForPanel()
    var formState by remember { mutableStateOf(ClassFormState.defaultForCreate(terms)) }
    var isCreating by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val locations = LocationStore.sortedForPanel()
    val classes = ScheduledClassStore.forSchedulePanel()

    fun clearSelection() {
        selectedId = null
        isCreating = true
        formState = ClassFormState.defaultForCreate(terms)
        formError = null
    }

    fun resetFormForCreate() {
        selectedId = null
        isCreating = true
        formState = ClassFormState.defaultForCreate(terms)
        formError = null
    }

    fun loadIntoForm(scheduledClass: ScheduledClass) {
        selectedId = scheduledClass.id
        isCreating = false
        formState = ClassFormState(
            name = scheduledClass.name,
            termIds = scheduledClass.termIds.toSet(),
            customerGroupIds = scheduledClass.customerGroupIds.toSet(),
            locationId = scheduledClass.locationId,
            scheduleKind = scheduledClass.scheduleKind(),
            dayOfWeek = scheduledClass.dayOfWeek,
            singleDate = scheduledClass.singleDate.orEmpty(),
            startTime = scheduledClass.startTime,
            endTime = scheduledClass.endTime,
            notes = scheduledClass.notes,
            calendarColorArgb = scheduledClass.calendarColorArgb,
            classId = scheduledClass.id,
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
                            "Create terms and locations in their panels, then add weekly or single-day classes here."
                        terms.isEmpty() ->
                            "Create terms in the Terms panel, then add weekly or single-day classes here."
                        locations.isEmpty() ->
                            "Add weekly or one-off classes. Assign locations in the Locations panel."
                        else ->
                            "Add weekly recurring classes or single-day classes with one date."
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
                            isCreating = isCreating,
                        )

                        if (!isCreating && selectedId != null) {
                            Spacer(modifier = Modifier.height(spacing.field))
                            ClassCustomerGroupsSection(
                                customerGroupIds = formState.customerGroupIds.toList(),
                                locationId = formState.locationId,
                                onCustomerGroupIdsChange = { ids ->
                                    formState = formState.copy(customerGroupIds = ids.toSet())
                                },
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
                                    formError = when (formState.scheduleKind) {
                                        ClassScheduleKind.RECURRING ->
                                            "Name, valid start/end times (HH:MM), and end after start are required."
                                        ClassScheduleKind.SINGLE_DAY ->
                                            "Name, class date, valid start/end times (HH:MM), and end after start are required."
                                    }
                                    return@GlideButton
                                }
                                formError = null
                                val stateToSave = formState.withAutoTermForSingleDay(terms)
                                if (stateToSave != formState) {
                                    formState = stateToSave
                                }
                                if (isCreating) {
                                    val scheduledClass = stateToSave.toScheduledClass()
                                    ScheduledClassStore.create(scheduledClass)
                                    loadIntoForm(scheduledClass)
                                } else {
                                    val existing = selectedId?.let { ScheduledClassStore.findById(it) }
                                    if (existing != null) {
                                        val updated = stateToSave.toScheduledClass(
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
    val groupCount = scheduledClass.customerGroupIds.size
    val enrolledCount = scheduledClass.enrolledHeadcount()
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
    customerGroupIds: List<String>,
    locationId: String?,
    onCustomerGroupIdsChange: (List<String>) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    var searchQuery by remember { mutableStateOf("") }
    var enrollmentMessage by remember { mutableStateOf<String?>(null) }

    val assignedIds = customerGroupIds
    val location = locationId?.let { LocationStore.findById(it) }
    val headcount = headcountForCustomerGroups(customerGroupIds)

    val searchResults = remember(searchQuery, assignedIds) {
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
                    secondaryLabel = "${group.classAttendeeCount()} attending",
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
                val result = tryAddCustomerGroup(
                    currentGroupIds = customerGroupIds,
                    groupId = groupId,
                    locationId = locationId,
                )
                if (result == AddCustomerGroupResult.Success) {
                    onCustomerGroupIdsChange(customerGroupIds + groupId)
                }
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
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatPersonLabel(main.name, main.dateOfBirth),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                append("${group.classAttendeeCount()} attending · ${group.memberCount()} in group")
                                if (!group.mainContactAttendsClass) {
                                    append(" · main contact not attending")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    GlideTextButton(
                        onClick = {
                            onCustomerGroupIdsChange(customerGroupIds.filter { it != groupId })
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

@Composable
private fun CollapsibleFormSection(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    summary: String? = null,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "▾" else "▸",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.padding(end = 4.dp),
            )
            GlideFieldLabel(title)
            if (!expanded && summary != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(2.dp))
            content()
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
    isCreating: Boolean,
) {
    var scheduleKindExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var termsExpanded by remember { mutableStateOf(false) }
    var locationSectionExpanded by remember { mutableStateOf(false) }
    val colorPicker = rememberClassColorPickerState()
    val previewColorArgb = state.calendarColorArgb
        ?: state.classId?.let { defaultCalendarColorArgb(it) }
        ?: state.name.takeIf { it.isNotBlank() }?.let { defaultCalendarColorArgb(it) }
        ?: ClassCalendarPalette.first()

    val termsSummary = when {
        state.termIds.isEmpty() -> "None selected"
        state.termIds.size == 1 -> terms.find { it.id in state.termIds }?.name ?: "1 term"
        else -> "${state.termIds.size} terms"
    }
    val locationSummary = state.locationId?.let { id ->
        locations.find { it.id == id }?.name
    } ?: "None"

    GlideOutlinedField(
        value = state.name,
        onValueChange = { onStateChange(state.copy(name = it)) },
        label = "Class name",
        placeholder = "e.g. Tuesday Beginner Ballet",
    )
    Spacer(modifier = Modifier.height(spacing.field))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ClassCalendarColorSwatch(
            colorArgb = previewColorArgb,
            onClick = {
                colorPicker.show(previewColorArgb) { newArgb ->
                    onStateChange(state.copy(calendarColorArgb = newArgb))
                }
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Calendar color",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Shown on the term calendar for each scheduled day.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        GlideTextButton(
            onClick = {
                colorPicker.show(previewColorArgb) { newArgb ->
                    onStateChange(state.copy(calendarColorArgb = newArgb))
                }
            },
        ) {
            Text("Change")
        }
    }
    colorPicker.dialog()
    Spacer(modifier = Modifier.height(spacing.field))

    CollapsibleFormSection(
        title = "Terms",
        expanded = termsExpanded,
        onExpandedChange = { termsExpanded = it },
        summary = termsSummary,
    ) {
        if (terms.isEmpty()) {
            Text(
                text = "Create terms in the Terms panel, then select which terms this class runs in.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = if (isCreating && state.scheduleKind == ClassScheduleKind.RECURRING) {
                    "Current and future terms are selected by default for new weekly classes. Adjust as needed."
                } else {
                    "Select all terms this class spans (e.g. rolling classes across seasons)."
                },
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

    CollapsibleFormSection(
        title = "Location",
        expanded = locationSectionExpanded,
        onExpandedChange = { locationSectionExpanded = it },
        summary = locationSummary,
    ) {
        if (locations.isEmpty()) {
            Text(
                text = "Create locations in the Locations panel, then assign a room to this class.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = it },
            ) {
                OutlinedTextField(
                    value = locationSummary,
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
        GlideFieldLabel("Schedule")
        Spacer(modifier = Modifier.height(2.dp))
        ExposedDropdownMenuBox(
            expanded = scheduleKindExpanded,
            onExpandedChange = { scheduleKindExpanded = it },
        ) {
            OutlinedTextField(
                value = state.scheduleKind.label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scheduleKindExpanded) },
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
                expanded = scheduleKindExpanded,
                onDismissRequest = { scheduleKindExpanded = false },
            ) {
                ClassScheduleKind.entries.forEach { kind ->
                    DropdownMenuItem(
                        text = { Text(kind.label, style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            onStateChange(
                                when (kind) {
                                    ClassScheduleKind.RECURRING -> {
                                        val recurring = state.copy(
                                            scheduleKind = kind,
                                            singleDate = "",
                                        )
                                        if (isCreating) {
                                            recurring.copy(termIds = defaultRecurringClassTermIds(terms))
                                        } else {
                                            recurring
                                        }
                                    }
                                    ClassScheduleKind.SINGLE_DAY ->
                                        state.copy(scheduleKind = kind).withAutoTermForSingleDay(terms)
                                },
                            )
                            scheduleKindExpanded = false
                        },
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(spacing.field))

    when (state.scheduleKind) {
        ClassScheduleKind.RECURRING -> {
            Column {
                GlideFieldLabel("Day of week")
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
        }
        ClassScheduleKind.SINGLE_DAY -> {
            IsoDateField(
                label = "Class date",
                value = state.singleDate,
                onValueChange = { isoDate ->
                    val day = parseScheduleIsoDate(isoDate)?.dayOfWeek?.toModelDayOfWeek()
                    onStateChange(
                        state.copy(
                            singleDate = isoDate,
                            dayOfWeek = day ?: state.dayOfWeek,
                        ).withAutoTermForSingleDay(terms),
                    )
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            val autoTerm = state.termIds.singleOrNull()?.let { id -> terms.find { it.id == id } }
            when {
                state.singleDate.isBlank() -> {
                    Text(
                        text = "Pick a date — the term that includes that day is selected automatically.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                autoTerm != null -> {
                    Text(
                        text = "Term set automatically: ${autoTerm.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                else -> {
                    Text(
                        text = "No term includes this date. Adjust term dates in the Terms panel.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
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
