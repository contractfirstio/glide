package glide.data

import glide.model.AttendanceSessionKey
import glide.model.Class
import glide.model.dateRange
import glide.model.isValidTime24h
import glide.model.occursOn
import glide.model.sessionHasEndedForAttendance
import java.time.LocalDate
import java.time.LocalTime

data class PendingAttendanceSession(
    val session: AttendanceSessionKey,
    val className: String,
    val sessionDate: LocalDate,
    val unmarkedCount: Int,
    val attendeeCount: Int,
)

fun hasOutstandingAttendanceSubmissions(
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now(),
): Boolean = findPastSessionsNeedingAttendance(today, now).isNotEmpty()

fun attendanceBlocksBillIssuanceMessage(sessionCount: Int? = null): String {
    val count = sessionCount ?: findPastSessionsNeedingAttendance().size
    return if (count == 1) {
        "Submit attendance for the outstanding past class before issuing bills."
    } else {
        "Submit attendance for $count outstanding past classes before issuing bills."
    }
}

fun findPastSessionsNeedingAttendance(
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now(),
): List<PendingAttendanceSession> {
    if (TermStore.terms.isEmpty()) return emptyList()
    val pending = mutableListOf<PendingAttendanceSession>()
    for (scheduledClass in ClassStore.classes) {
        if (scheduledClass.soldPlanIds.isEmpty()) continue
        collectPastSessionsNeedingAttendance(scheduledClass, today, now, pending)
    }
    return pending.sortedWith(compareBy({ it.sessionDate }, { it.className.lowercase() }))
}

private fun collectPastSessionsNeedingAttendance(
    scheduledClass: Class,
    today: LocalDate,
    now: LocalTime,
    pending: MutableList<PendingAttendanceSession>,
) {
    val dates = mutableSetOf<LocalDate>()
    for (termId in scheduledClass.termIds) {
        val term = TermStore.findById(termId) ?: continue
        val range = term.dateRange() ?: continue
        var date = range.start
        while (!date.isAfter(range.endInclusive) && !date.isAfter(today)) {
            if (
                scheduledClass.occursOn(date) &&
                scheduledClass.sessionHasEndedForAttendance(sessionDate = date, today = today, now = now)
            ) {
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

internal fun pendingAttendanceForSession(
    scheduledClass: Class,
    sessionDate: LocalDate,
): PendingAttendanceSession? {
    val attendees = attendeesForClass(scheduledClass, sessionDate)
    if (attendees.isEmpty()) return null
    val attendeeKeys = attendees.map { it.key }
    val session = AttendanceSessionKey(
        classId = scheduledClass.id,
        sessionDate = sessionDate.toString(),
    )
    val statusByKey = AttendanceStore.draftForSession(session, attendeeKeys)
    val unmarked = AttendanceStore.unmarkedAttendeeKeys(statusByKey, attendeeKeys)
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
    AttendancePanelState.openForReminder(pending.session.classId, pending.sessionDate)
}

private const val ATTENDANCE_REMINDER_POLL_MS = 30_000L

/** Delay until the next attendance-reminder refresh (class end today, or [ATTENDANCE_REMINDER_POLL_MS]). */
fun millisUntilNextAttendanceReminderCheck(
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now(),
): Long {
    if (TermStore.terms.isEmpty()) return ATTENDANCE_REMINDER_POLL_MS
    var nextWakeMs = ATTENDANCE_REMINDER_POLL_MS
    val nowMs = now.toSecondOfDay() * 1000L + now.nano / 1_000_000
    for (scheduledClass in ClassStore.classes) {
        if (scheduledClass.soldPlanIds.isEmpty()) continue
        if (!scheduledClass.occursOn(today)) continue
        if (!isValidTime24h(scheduledClass.endTime)) continue
        val parts = scheduledClass.endTime.split(":")
        val endMs = (parts[0].toInt() * 3600L + parts[1].toInt() * 60L) * 1000L
        if (nowMs < endMs) {
            nextWakeMs = minOf(nextWakeMs, (endMs - nowMs).coerceAtLeast(1_000L))
        }
    }
    return nextWakeMs
}
