package glide.ui.scheduling

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import glide.data.AttendancePanelState
import glide.data.LocationStore
import glide.data.ScheduledClassStore
import glide.data.TermStore
import java.time.LocalDate
import glide.model.AcademicTerm
import glide.model.ScheduledClass
import glide.model.scheduleLine
import glide.model.timeRangeLine
import glide.ui.layout.GlideLayout
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.theme.GlideOutlinedButton
import glide.ui.theme.GlideTextButton
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MonthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val WeekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun TermCalendarPanel(modifier: Modifier = Modifier) {
    val termsChronological = TermStore.sortedChronologically()
    val allClasses = ScheduledClassStore.classes
    val colorPicker = rememberClassColorPickerState()

    var selectedTermId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(termsChronological.map { it.id }) {
        if (termsChronological.isEmpty()) {
            selectedTermId = null
            return@LaunchedEffect
        }
        if (selectedTermId == null || termsChronological.none { it.id == selectedTermId }) {
            selectedTermId = defaultTermSelectionId(termsChronological)
        }
    }

    val spacing = GlideLayout.comfortable

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.outer),
    ) {
        if (termsChronological.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Create terms in the Terms panel to view the calendar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }
            return
        }

        val selectedTerm = selectedTermId?.let { id ->
            termsChronological.find { it.id == id }
        } ?: return@Column
        val termIndex = termsChronological.indexOfFirst { it.id == selectedTerm.id }
        val termClasses = remember(selectedTerm.id, allClasses) {
            allClasses.filter { selectedTerm.id in it.termIds }
                .sortedWith(compareBy({ it.dayOfWeek.sortOrder }, { it.startTime }, { it.name }))
        }
        val months = remember(selectedTerm) { monthsInTerm(selectedTerm) }

        TermCalendarHeader(
            term = selectedTerm,
            canGoPrevious = termIndex > 0,
            canGoNext = termIndex < termsChronological.lastIndex,
            onPrevious = {
                if (termIndex > 0) selectedTermId = termsChronological[termIndex - 1].id
            },
            onNext = {
                if (termIndex < termsChronological.lastIndex) {
                    selectedTermId = termsChronological[termIndex + 1].id
                }
            },
        )

        Spacer(modifier = Modifier.height(spacing.section))

        if (termClasses.isNotEmpty()) {
            TermCalendarLegend(
                classes = termClasses,
                onPickColor = { scheduledClass ->
                    colorPicker.show(scheduledClass.resolvedCalendarColorArgb()) { newArgb ->
                        ScheduledClassStore.update(
                            scheduledClass.copy(calendarColorArgb = newArgb),
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(spacing.section))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.section))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing.section),
        ) {
            items(months, key = { it.toString() }) { yearMonth ->
                TermCalendarMonthSection(
                    yearMonth = yearMonth,
                    term = selectedTerm,
                    classes = allClasses,
                )
            }
        }
    }

    colorPicker.dialog()
}

@Composable
private fun TermCalendarHeader(
    term: AcademicTerm,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val dateLine = buildString {
        if (term.startDate.isNotBlank()) append(formatIsoDateForDisplay(term.startDate))
        if (term.startDate.isNotBlank() && term.endDate.isNotBlank()) append(" – ")
        if (term.endDate.isNotBlank()) append(formatIsoDateForDisplay(term.endDate))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlideOutlinedButton(onClick = onPrevious, enabled = canGoPrevious) {
            Text("◀")
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = term.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (dateLine.isNotBlank()) {
                Text(
                    text = dateLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        GlideOutlinedButton(onClick = onNext, enabled = canGoNext) {
            Text("▶")
        }
    }
}

@Composable
private fun TermCalendarLegend(
    classes: List<ScheduledClass>,
    onPickColor: (ScheduledClass) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Classes",
            style = MaterialTheme.typography.labelLarge,
        )
        classes.forEach { scheduledClass ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onPickColor(scheduledClass) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ClassCalendarColorSwatch(
                    colorArgb = scheduledClass.resolvedCalendarColorArgb(),
                    onClick = { onPickColor(scheduledClass) },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scheduledClass.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = scheduledClass.scheduleLine(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                GlideTextButton(onClick = { onPickColor(scheduledClass) }) {
                    Text("Color")
                }
            }
        }
    }
}

@Composable
private fun TermCalendarMonthSection(
    yearMonth: YearMonth,
    term: AcademicTerm,
    classes: List<ScheduledClass>,
) {
    val cells = remember(yearMonth, term.id, classes) {
        buildMonthGrid(yearMonth, term, classes)
    }

    Column {
        Text(
            text = MonthTitleFormatter.format(yearMonth),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            WeekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, gridLineColor, RoundedCornerShape(6.dp))
                .background(gridLineColor)
                .padding(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    week.forEach { cell ->
                        TermCalendarDayCellView(
                            cell = cell,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TermCalendarDayCellView(
    cell: TermCalendarDayCell,
    modifier: Modifier = Modifier,
) {
    val date = cell.date
    val showGrey = date == null || !cell.inTerm
    val backgroundColor = when {
        date == null -> Color.Transparent
        showGrey -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(backgroundColor)
            .padding(3.dp),
    ) {
        if (date != null) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = if (showGrey) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.align(Alignment.TopStart),
            )
            if (cell.classes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = 11.dp, start = 1.dp, end = 1.dp, bottom = 1.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    cell.classes.forEach { scheduledClass ->
                        TermCalendarClassBlock(
                            scheduledClass = scheduledClass,
                            sessionDate = date,
                            onOpenAttendance = { classId, sessionDate ->
                                AttendancePanelState.open(classId, sessionDate)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TermCalendarClassBlock(
    scheduledClass: ScheduledClass,
    sessionDate: LocalDate,
    onOpenAttendance: (scheduledClassId: String, sessionDate: LocalDate) -> Unit,
) {
    val locationName = scheduledClass.locationId?.let { LocationStore.findById(it)?.name }
    val rosterLines = rosterLinesForClass(scheduledClass)
    val blockTextColor = Color(0xFF0A1018)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .clickable {
                onOpenAttendance(scheduledClass.id, sessionDate)
            }
            .background(scheduledClass.resolvedCalendarColor())
            .padding(horizontal = 3.dp, vertical = 3.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = scheduledClass.timeRangeLine(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = blockTextColor,
                    maxLines = 1,
                )
                if (!locationName.isNullOrBlank()) {
                    Text(
                        text = locationName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = blockTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                    )
                }
            }
            rosterLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Medium,
                    color = blockTextColor.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 8.sp,
                )
            }
        }
    }
}
