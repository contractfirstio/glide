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
import glide.data.PackClassScheduleStore
import glide.data.RollingPackBillingService
import glide.data.assignPackClassSchedule
import glide.data.isCustomerGroupAvailableForClass
import glide.data.tryAddCustomerGroup
import glide.data.validateCustomerGroupsForClass
import glide.data.validatePackSchedulesForClass
import glide.data.validateRollingPackEnrollmentsForClass
import glide.data.peopleGroupHasRollingPack
import glide.data.canAcceptRollingPackEnrollments
import glide.data.PackEnrollmentStore
import glide.data.PeopleGroupStore
import glide.data.PlanStore
import glide.data.ScheduledClassStore
import glide.data.SchedulePanelState
import glide.data.TermStore
import glide.data.resolveMainContact
import glide.data.toUserMessage
import glide.model.ClassLocation
import glide.model.PeopleGroup
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
import glide.ui.leads.millisToIsoDate
import glide.ui.leads.parseIsoDateToMillis
import glide.ui.layout.GlideLayout
import glide.ui.shared.FormPanelLinkedBox
import glide.ui.shared.FormPanelSection
import glide.ui.shared.FormPanelSectionRole
import glide.ui.shared.FormPanelSectionsDivider
import glide.ui.shared.rememberFormDirtyTracker
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
    val form = rememberFormDirtyTracker(ClassFormState.defaultForCreate(terms))
    var isCreating by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val locations = LocationStore.sortedForPanel()
    val soldPlanFilterId = SchedulePanelState.selectedSoldPlanId
    val classes = ScheduledClassStore.forSchedulePanelFromSoldPlan(soldPlanFilterId)
    val soldPlanFilterLabel = soldPlanFilterId?.let { groupId ->
        PeopleGroupStore.findById(groupId)?.let { group ->
            group.resolveMainContact().name.takeIf { it.isNotBlank() }
                ?: group.planId?.let { PlanStore.findById(it)?.name }?.takeIf { it.isNotBlank() }
        }
    }

    fun clearLocalSelection() {
        selectedId = null
        isCreating = true
        form.load(ClassFormState.defaultForCreate(terms))
        formError = null
    }

    fun clearSelection() {
        clearLocalSelection()
        SchedulePanelState.onClassCleared()
    }

    fun resetFormForCreate() {
        clearLocalSelection()
        SchedulePanelState.onClassCleared()
    }

    fun syncClassIntoForm(scheduledClass: ScheduledClass) {
        selectedId = scheduledClass.id
        isCreating = false
        SchedulePanelState.syncClassContext(scheduledClass.id)
        form.load(
            ClassFormState(
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
            ),
        )
        formError = null
    }

    fun loadIntoForm(scheduledClass: ScheduledClass) {
        selectedId = scheduledClass.id
        isCreating = false
        SchedulePanelState.onClassSelected(scheduledClass.id)
        form.load(
            ClassFormState(
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
            ),
        )
        formError = null
    }

    LaunchedEffect(soldPlanFilterId, classes) {
        val soldPlanId = soldPlanFilterId ?: return@LaunchedEffect
        val scheduledClass = ScheduledClassStore.findClassContainingCustomerGroup(soldPlanId)
        if (scheduledClass == null) {
            if (selectedId != null) clearLocalSelection()
            return@LaunchedEffect
        }
        if (selectedId != scheduledClass.id) {
            syncClassIntoForm(scheduledClass)
        }
    }

    LaunchedEffect(classes, selectedId, soldPlanFilterId) {
        if (soldPlanFilterId != null) return@LaunchedEffect
        if (selectedId != null && classes.none { it.id == selectedId }) {
            clearSelection()
        }
    }

    LaunchedEffect(locations, form.draft.locationId) {
        val locationId = form.draft.locationId ?: return@LaunchedEffect
        if (locations.none { it.id == locationId }) {
            form.draft = form.draft.copy(locationId = null)
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
                        soldPlanFilterId != null -> {
                            val label = soldPlanFilterLabel ?: "this sold plan"
                            "Showing class for $label. Use Clear filter to reset."
                        }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.field)) {
                            if (soldPlanFilterId != null) {
                                GlideTextButton(onClick = { SchedulePanelState.clearSoldPlanFilter() }) {
                                    Text("Clear filter")
                                }
                            }
                            GlideButton(onClick = { resetFormForCreate() }) {
                                Text(if (compact) "New" else "New class")
                            }
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
                                text = when {
                                    soldPlanFilterId != null ->
                                        "This sold plan is not assigned to a class."
                                    else -> "No classes yet."
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
                            state = form.draft,
                            onStateChange = { form.draft = it },
                            terms = terms,
                            locations = locations,
                            spacing = spacing,
                            isCreating = isCreating,
                        )

                        if (!isCreating && selectedId != null) {
                            FormPanelSectionsDivider(label = "Enrollment", spacing = spacing)
                            ClassCustomerGroupsSection(
                                classId = selectedId,
                                termIds = form.draft.termIds.toList(),
                                customerGroupIds = form.draft.customerGroupIds.toList(),
                                locationId = form.draft.locationId,
                                onCustomerGroupIdsChange = { ids ->
                                    form.draft = form.draft.copy(customerGroupIds = ids.toSet())
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
                                if (!form.draft.isValid()) {
                                    formError = when (form.draft.scheduleKind) {
                                        ClassScheduleKind.RECURRING ->
                                            "Name, valid start/end times (HH:MM), and end after start are required."
                                        ClassScheduleKind.SINGLE_DAY ->
                                            "Name, class date, valid start/end times (HH:MM), and end after start are required."
                                    }
                                    return@GlideButton
                                }
                                formError = null
                                val stateToSave = form.draft.withAutoTermForSingleDay(terms)
                                if (stateToSave != form.draft) {
                                    form.draft = stateToSave
                                }
                                if (isCreating) {
                                    val scheduledClass = stateToSave.toScheduledClass()
                                    ScheduledClassStore.create(scheduledClass)
                                    loadIntoForm(scheduledClass)
                                } else {
                                    val existing = selectedId?.let { ScheduledClassStore.findById(it) }
                                    if (existing != null) {
                                        validateCustomerGroupsForClass(
                                            stateToSave.customerGroupIds.toList(),
                                            classId = existing.id,
                                        )?.let { message ->
                                            formError = message
                                            return@GlideButton
                                        }
                                        val updated = stateToSave.toScheduledClass(
                                            existingId = existing.id,
                                            createdAtMillis = existing.createdAtMillis,
                                        )
                                        validatePackSchedulesForClass(
                                            updated.customerGroupIds,
                                            updated,
                                        )?.let { message ->
                                            formError = message
                                            return@GlideButton
                                        }
                                        validateRollingPackEnrollmentsForClass(
                                            updated.customerGroupIds,
                                            updated,
                                        )?.let { message ->
                                            formError = message
                                            return@GlideButton
                                        }
                                        ScheduledClassStore.update(updated)
                                        updated.customerGroupIds.forEach { groupId ->
                                            assignPackClassSchedule(groupId, updated)
                                            RollingPackBillingService.syncRollingPackBilling(groupId)
                                        }
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
    classId: String?,
    termIds: List<String>,
    customerGroupIds: List<String>,
    locationId: String?,
    onCustomerGroupIdsChange: (List<String>) -> Unit,
    spacing: GlideLayout.Spacing,
) {
    var searchQuery by remember { mutableStateOf("") }
    var enrollmentMessage by remember { mutableStateOf<String?>(null) }

    val scheduledClass = classId?.let { ScheduledClassStore.findById(it) }
    val classForValidation = scheduledClass?.copy(termIds = termIds)
    val assignedIds = customerGroupIds
    val location = locationId?.let { LocationStore.findById(it) }
    val headcount = headcountForCustomerGroups(customerGroupIds)

    val searchResults = remember(searchQuery, assignedIds, classId, termIds, classForValidation) {
        PeopleGroupStore.customers
            .filter { it.id !in assignedIds }
            .filter { group -> isCustomerGroupAvailableForClass(group.id, classId) }
            .filter { group ->
                if (!peopleGroupHasRollingPack(group.id)) return@filter true
                classForValidation?.canAcceptRollingPackEnrollments() == true
            }
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
                    secondaryLabel = soldPackSearchSecondaryLabel(group),
                )
            }
    }

    FormPanelSection(
        title = "Customer groups",
        description = "Customer packs enrolled on this class. Each group can only be on one class.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
    ) {
        ClassCapacityGraphic(
            occupiedCount = headcount,
            maxCapacity = location?.maxCapacity,
        )
        Spacer(modifier = Modifier.height(spacing.section))
        EntitySearchPicker(
            label = "Sold Packs",
            placeholder = "Search by name or email…",
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            results = searchResults,
            onSelect = { groupId ->
                val result = tryAddCustomerGroup(
                    currentGroupIds = customerGroupIds,
                    groupId = groupId,
                    locationId = locationId,
                    classId = classId,
                    scheduledClass = classForValidation,
                )
                if (result == AddCustomerGroupResult.Success) {
                    onCustomerGroupIdsChange(customerGroupIds + groupId)
                    classForValidation?.let { cls -> assignPackClassSchedule(groupId, cls) }
                    RollingPackBillingService.syncRollingPackBilling(groupId)
                    enrollmentMessage = result.toUserMessage()
                } else {
                    enrollmentMessage = result.toUserMessage()
                }
            },
            noResultsText = "No available sold packs (each pack can only be on one class).",
        )
        enrollmentMessage?.let { message ->
            Spacer(modifier = Modifier.height(spacing.field))
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = if (
                    message.contains("exceeded", ignoreCase = true) ||
                    message.contains("already", ignoreCase = true) ||
                    message.contains("Create a new term", ignoreCase = true) ||
                    message.contains("rolling pack", ignoreCase = true)
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
        if (assignedIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.section))
            FormPanelLinkedBox(role = FormPanelSectionRole.Secondary) {
                GlideFieldLabel("In this class (${assignedIds.size})")
                Spacer(modifier = Modifier.height(spacing.field))
                assignedIds.forEachIndexed { index, groupId ->
                    val group = PeopleGroupStore.findById(groupId) ?: return@forEachIndexed
                    val main = group.resolveMainContact()
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatPersonLabel(main.name, main.dateOfBirth),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = soldPackSearchSecondaryLabel(group),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        GlideTextButton(
                            onClick = {
                                if (classId != null) {
                                    PackClassScheduleStore.remove(groupId, classId)
                                }
                                onCustomerGroupIdsChange(customerGroupIds.filter { it != groupId })
                                enrollmentMessage = "Customer group removed from class."
                            },
                        ) {
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (index < assignedIds.lastIndex) {
                        Spacer(modifier = Modifier.height(spacing.field))
                    }
                }
            }
        }
    }
}

private fun soldPackSearchSecondaryLabel(group: PeopleGroup): String {
    val packName = PackEnrollmentStore.forPeopleGroup(group.id)?.planSnapshot?.planName
        ?: group.planId?.let { PlanStore.findById(it)?.name?.takeIf { name -> name.isNotBlank() } }
        ?: "No pack"
    val startDate = group.planStartDate.takeIf { it.isNotBlank() }
        ?.let { formatIsoDateForDisplay(it) }
        ?.takeIf { it.isNotBlank() }
        ?: PackEnrollmentStore.forPeopleGroup(group.id)?.packPeriodStartedAtMillis
            ?.let { formatIsoDateForDisplay(millisToIsoDate(it)) }
            ?.takeIf { it.isNotBlank() }
    return if (startDate != null) {
        "$packName · Starts $startDate"
    } else {
        packName
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
    val colorPicker = rememberClassColorPickerState()
    val previewColorArgb = state.calendarColorArgb
        ?: state.classId?.let { defaultCalendarColorArgb(it) }
        ?: state.name.takeIf { it.isNotBlank() }?.let { defaultCalendarColorArgb(it) }
        ?: ClassCalendarPalette.first()

    val locationSummary = state.locationId?.let { id ->
        locations.find { it.id == id }?.name
    } ?: "None"

    FormPanelSection(
        title = "Class details",
        description = "Name and calendar color for this class.",
        spacing = spacing,
        role = FormPanelSectionRole.Primary,
    ) {
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
    }

    FormPanelSection(
        title = "Terms",
        description = "Which academic terms this class runs in.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
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

    FormPanelSection(
        title = "Location",
        description = "Room or venue for this class.",
        spacing = spacing,
        role = FormPanelSectionRole.Secondary,
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

    FormPanelSectionsDivider(label = "When it runs", spacing = spacing)

    FormPanelSection(
        title = "Schedule",
        description = "Weekly or one-off timing for this class.",
        spacing = spacing,
        role = FormPanelSectionRole.Tertiary,
    ) {
        Column {
            GlideFieldLabel("Schedule type")
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
}
