package glide.data

import glide.model.Class
import glide.model.DayOfWeek
import glide.model.isWeekly
import glide.model.parseIsoLocalDate
import glide.model.toModelDayOfWeek

data class DayCapacity(
    val day: DayOfWeek,
    val occupied: Int,
    val maxCapacity: Int?,
) {
    val available: Int? get() = maxCapacity?.let { (it - occupied).coerceAtLeast(0) }
    val isFull: Boolean get() = maxCapacity != null && occupied >= maxCapacity

    fun wouldExceed(adding: Int): Boolean =
        maxCapacity != null && occupied + adding > maxCapacity

    fun projectedOccupied(adding: Int): Int = occupied + adding
}

fun soldPlanScheduledDaysOnWeeklyClass(soldPlanId: String, scheduledClass: Class): Set<DayOfWeek> {
    if (!scheduledClass.isWeekly()) return emptySet()
    val dates = SoldPlanClassScheduleStore.sessionDatesFor(soldPlanId, scheduledClass.id) ?: return emptySet()
    return dates
        .mapNotNull { iso -> parseIsoLocalDate(iso)?.dayOfWeek?.toModelDayOfWeek() }
        .toSet()
}

fun weeklyHeadcountForDay(
    scheduledClass: Class,
    day: DayOfWeek,
    excludeSoldPlanId: String? = null,
): Int =
    scheduledClass.soldPlanIds
        .asSequence()
        .filter { it != excludeSoldPlanId }
        .filter { soldPlanScheduledDaysOnWeeklyClass(it, scheduledClass).contains(day) }
        .sumOf { findSoldPlanById(it)?.classAttendeeCount() ?: 0 }

fun weeklyHeadcountByDay(scheduledClass: Class): Map<DayOfWeek, Int> =
    scheduledClass.weeklyDays.associateWith { day -> weeklyHeadcountForDay(scheduledClass, day) }

fun weeklyDayCapacity(scheduledClass: Class, day: DayOfWeek): DayCapacity =
    DayCapacity(
        day = day,
        occupied = weeklyHeadcountForDay(scheduledClass, day),
        maxCapacity = scheduledClass.locationId?.let { LocationStore.findById(it)?.maxCapacity },
    )

fun weeklyDayCapacitiesForClass(scheduledClass: Class): List<DayCapacity> =
    scheduledClass.weeklyDays
        .sortedBy { it.sortOrder }
        .map { day -> weeklyDayCapacity(scheduledClass, day) }

fun validateWeeklyDayCapacity(
    scheduledClass: Class,
    addingSoldPlanId: String,
    selectedDays: Set<DayOfWeek>,
    currentSoldPlanIds: List<String> = scheduledClass.soldPlanIds,
): AddSoldPlanResult? {
    val maxCapacity = scheduledClass.locationId?.let { LocationStore.findById(it)?.maxCapacity }
        ?: return null
    val adding = findSoldPlanById(addingSoldPlanId)?.classAttendeeCount() ?: 0
    if (adding == 0) return null

    val classForCounting = scheduledClass.copy(soldPlanIds = currentSoldPlanIds)
    for (day in selectedDays) {
        val current = weeklyHeadcountForDay(classForCounting, day)
        if (current + adding > maxCapacity) {
            return AddSoldPlanResult.DailyCapacityExceeded(
                day = day,
                currentHeadcount = current,
                groupHeadcount = adding,
                maxCapacity = maxCapacity,
            )
        }
    }
    return null
}

fun formatWeeklyCapacityListSummary(scheduledClass: Class): String? {
    val max = scheduledClass.locationId?.let { LocationStore.findById(it)?.maxCapacity } ?: return null
    val byDay = weeklyHeadcountByDay(scheduledClass)
    val parts = scheduledClass.weeklyDays
        .sortedBy { it.sortOrder }
        .mapNotNull { day ->
            val occupied = byDay[day] ?: 0
            if (occupied == 0) null else "${day.shortLabel} $occupied/$max"
        }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
