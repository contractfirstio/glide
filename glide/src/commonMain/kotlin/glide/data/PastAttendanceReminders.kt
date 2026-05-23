package glide.data

import glide.model.ClassSessionKey
import glide.model.ScheduledClass
import glide.model.dateRange
import glide.model.occursOn
import glide.model.parseIsoLocalDate
import java.time.LocalDate

data class PendingAttendanceSession(
    val session: ClassSessionKey,
    val className: String,
    val sessionDate: LocalDate,
    val unmarkedCount: Int,
    val attendeeCount: Int,
)

fun hasOutstandingAttendanceSubmissions(today: LocalDate = LocalDate.now()): Boolean =
    findPastSessionsNeedingAttendance(today).isNotEmpty()

fun attendanceBlocksBillIssuanceMessage(sessionCount: Int? = null): String {
    val count = sessionCount ?: findPastSessionsNeedingAttendance().size
    return if (count == 1) {
        "Submit attendance for the outstanding past class before issuing bills."
    } else {
        "Submit attendance for $count outstanding past classes before issuing bills."
    }
}

fun findPastSessionsNeedingAttendance(today: LocalDate = LocalDate.now()): List<PendingAttendanceSession> {
    if (TermStore.terms.isEmpty()) return emptyList()
    val pending = mutableListOf<PendingAttendanceSession>()
    for (scheduledClass in ScheduledClassStore.classes) {
        if (scheduledClass.customerGroupIds.isEmpty()) continue
        collectPastSessionsNeedingAttendance(scheduledClass, today, pending)
    }
    return pending.sortedWith(compareBy({ it.sessionDate }, { it.className.lowercase() }))
}

private fun collectPastSessionsNeedingAttendance(
    scheduledClass: ScheduledClass,
    today: LocalDate,
    pending: MutableList<PendingAttendanceSession>,
) {
    val dates = mutableSetOf<LocalDate>()
    for (termId in scheduledClass.termIds) {
        val term = TermStore.findById(termId) ?: continue
        val range = term.dateRange() ?: continue
        var date = range.start
        while (!date.isAfter(range.endInclusive) && date.isBefore(today)) {
            if (scheduledClass.occursOn(date)) {
                dates.add(date)
            }
            date = date.plusDays(1)
        }
    }
    for (date in dates) {
        val pendingSession = pendingAttendanceForSession(scheduledClass, date) ?: continue
        pending.add(pendingSession)
    }
}

private fun pendingAttendanceForSession(
    scheduledClass: ScheduledClass,
    sessionDate: LocalDate,
): PendingAttendanceSession? {
    val attendees = attendeesForClass(scheduledClass, sessionDate)
    if (attendees.isEmpty()) return null
    val attendeeKeys = attendees.map { it.key }
    val session = ClassSessionKey(
        scheduledClassId = scheduledClass.id,
        sessionDate = sessionDate.toString(),
    )
    val statusByKey = ClassAttendanceStore.draftForSession(session, attendeeKeys)
    val unmarked = ClassAttendanceStore.unmarkedAttendeeKeys(statusByKey, attendeeKeys)
    if (unmarked.isEmpty()) return null
    return PendingAttendanceSession(
        session = session,
        className = scheduledClass.name,
        sessionDate = sessionDate,
        unmarkedCount = unmarked.size,
        attendeeCount = attendeeKeys.size,
    )
}

fun openPendingAttendanceSession(pending: PendingAttendanceSession) {
    AppViewState.switchTo(AppViewMode.SCHEDULING)
    AttendancePanelState.openForReminder(pending.session.scheduledClassId, pending.sessionDate)
}
