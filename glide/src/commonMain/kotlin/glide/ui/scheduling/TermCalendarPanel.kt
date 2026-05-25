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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.data.AttendancePanelState
import glide.data.LocationStore
import glide.data.SchedulePanelState
import glide.data.ClassStore
import glide.data.TermStore
import glide.data.millisUntilNextAttendanceReminderCheck
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import glide.model.Term
import glide.model.AttendanceSessionKey
import glide.model.Class
import glide.model.compareClasses
import glide.model.parseIsoLocalDate
import glide.model.timeRangeLine
import glide.ui.layout.GlideLayout
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.theme.GlideOutlinedButton
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MonthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val WeekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

private suspend fun LazyListState.scrollToShowDate(months: List<YearMonth>, date: LocalDate) {
    val monthIndex = indexOfMonthContaining(months, date)
    if (monthIndex < 0) return

    snapshotFlow { layoutInfo }
        .first { info ->
            info.totalItemsCount > monthIndex && info.viewportSize.height > 0
        }

    scrollToItem(monthIndex)

    snapshotFlow { layoutInfo }
        .first { info ->
            info.visibleItemsInfo.any { it.index == monthIndex && it.size > 0 }
        }

    val info = layoutInfo
    val monthItem = info.visibleItemsInfo.first { it.index == monthIndex }
    val scrollOffset = scrollOffsetToShowDateInMonthItem(
        monthItemHeightPx = monthItem.size,
        date = date,
        viewportHeightPx = info.viewportSize.height,
    )
    if (scrollOffset > 0) {
        scrollToItem(monthIndex, scrollOffset)
    }
}

@Composable
fun TermCalendarPanel(modifier: Modifier = Modifier) {
    val termsChronological = TermStore.sortedChronologically()
    val allClasses = ClassStore.classes
    val today = remember { LocalDate.now() }
    val viewMode = AppViewState.mode
    val attendanceVisible = AttendancePanelState.visible
    val activeAttendanceSession = if (attendanceVisible) AttendancePanelState.sessionKey else null
    val classSyncId = SchedulePanelState.selectedClassId
    val termFilterId = SchedulePanelState.selectedTermFilterId
    val locationFilterId = SchedulePanelState.selectedLocationFilterId
    val listState = rememberLazyListState()
    var attendanceRefreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilNextAttendanceReminderCheck())
            attendanceRefreshTick++
        }
    }
    attendanceRefreshTick

    val displayTermId = resolveCalendarTermId(
        termsChronological = termsChronological,
        allClasses = allClasses,
        termFilterId = termFilterId,
        locationFilterId = locationFilterId,
        classSyncId = classSyncId,
        attendanceSessionDate = activeAttendanceSession?.sessionDate,
        attendanceVisible = attendanceVisible,
    )

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

        val selectedTerm = displayTermId?.let { id ->
            termsChronological.find { it.id == id }
        } ?: return@Column
        val termIndex = termsChronological.indexOfFirst { it.id == selectedTerm.id }
        val termClasses = remember(selectedTerm.id, allClasses) {
            allClasses.filter { selectedTerm.id in it.termIds }
                .sortedWith(compareClasses())
        }
        val months = remember(selectedTerm) { monthsInTerm(selectedTerm) }

        LaunchedEffect(attendanceVisible, activeAttendanceSession, selectedTerm.id, months) {
            if (!attendanceVisible) return@LaunchedEffect
            val date = activeAttendanceSession?.sessionDate?.let { parseIsoLocalDate(it) } ?: return@LaunchedEffect
            listState.scrollToShowDate(months, date)
        }

        LaunchedEffect(viewMode, selectedTerm.id, months, attendanceVisible) {
            if (viewMode != AppViewMode.SCHEDULING || months.isEmpty() || attendanceVisible) return@LaunchedEffect
            listState.scrollToShowDate(months, today)
        }

        TermCalendarHeader(
            term = selectedTerm,
            canGoPrevious = termIndex > 0,
            canGoNext = termIndex < termsChronological.lastIndex,
            onPrevious = {
                if (termIndex > 0) {
                    SchedulePanelState.onTermSelected(termsChronological[termIndex - 1].id)
                }
            },
            onNext = {
                if (termIndex < termsChronological.lastIndex) {
                    SchedulePanelState.onTermSelected(termsChronological[termIndex + 1].id)
                }
            },
        )

        Spacer(modifier = Modifier.height(spacing.section))

        if (termClasses.isNotEmpty()) {
            TermCalendarLegend(classes = termClasses)
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(4.dp))
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing.section),
        ) {
            items(months, key = { it.toString() }) { yearMonth ->
                TermCalendarMonthSection(
                    yearMonth = yearMonth,
                    term = selectedTerm,
                    classes = allClasses,
                    today = today,
                    activeAttendanceSession = activeAttendanceSession,
                )
            }
        }
    }
}

@Composable
private fun TermCalendarHeader(
    term: Term,
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
private fun TermCalendarLegend(classes: List<Class>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        classes.forEach { scheduledClass ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(start = 3.dp, end = 5.dp, top = 1.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                ClassCalendarColorSwatch(
                    colorArgb = scheduledClass.resolvedCalendarColorArgb(),
                    size = 8.dp,
                )
                Text(
                    text = scheduledClass.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TermCalendarMonthSection(
    yearMonth: YearMonth,
    term: Term,
    classes: List<Class>,
    today: LocalDate,
    activeAttendanceSession: AttendanceSessionKey?,
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
                            today = today,
                            activeAttendanceSession = activeAttendanceSession,
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
    today: LocalDate,
    activeAttendanceSession: AttendanceSessionKey?,
    modifier: Modifier = Modifier,
) {
    val date = cell.date
    val showGrey = date == null || !cell.inTerm
    val isToday = date == today && cell.inTerm
    val isAttendanceDay = activeAttendanceSession != null && date != null &&
        activeAttendanceSession.sessionDate == date.toString()
    val backgroundColor = when {
        date == null -> Color.Transparent
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        isAttendanceDay -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
        showGrey -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }
    val dayBorderColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isAttendanceDay -> MaterialTheme.colorScheme.tertiary
        else -> null
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (dayBorderColor != null) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = dayBorderColor,
                        shape = RoundedCornerShape(2.dp),
                    )
                } else {
                    Modifier
                },
            )
            .background(backgroundColor)
            .padding(2.dp),
    ) {
        if (date != null) {
            val dayNumberEmphasis = isToday || isAttendanceDay
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = if (dayNumberEmphasis) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    isAttendanceDay -> MaterialTheme.colorScheme.onTertiaryContainer
                    showGrey -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.align(Alignment.TopStart),
            )
            if (cell.classes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = 9.dp, start = 1.dp, end = 1.dp, bottom = 0.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    cell.classes.forEach { scheduledClass ->
                        TermCalendarClassBlock(
                            scheduledClass = scheduledClass,
                            sessionDate = date,
                            isActiveAttendance = activeAttendanceSession?.let { session ->
                                session.classId == scheduledClass.id &&
                                    session.sessionDate == date.toString()
                            } == true,
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
    scheduledClass: Class,
    sessionDate: LocalDate,
    isActiveAttendance: Boolean,
    onOpenAttendance: (classId: String, sessionDate: LocalDate) -> Unit,
) {
    val locationName = scheduledClass.locationId?.let { LocationStore.findById(it)?.name }
    val rosterSummary = rosterLinesForClass(scheduledClass, sessionDate)
        .joinToString(", ")
        .takeIf { it.isNotBlank() }
    val blockTextColor = Color(0xFF0A1018)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(1.dp))
            .then(
                if (isActiveAttendance) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(1.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable { onOpenAttendance(scheduledClass.id, sessionDate) }
            .background(scheduledClass.resolvedCalendarColor())
            .padding(horizontal = 2.dp, vertical = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = buildString {
                    append(scheduledClass.timeRangeLine())
                    if (!locationName.isNullOrBlank()) {
                        append(" · ")
                        append(locationName)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                fontSize = 6.sp,
                fontWeight = FontWeight.SemiBold,
                color = blockTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 7.sp,
            )
            if (rosterSummary != null) {
                Text(
                    text = rosterSummary,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Medium,
                    color = blockTextColor.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 7.sp,
                )
            }
        }
    }
}
