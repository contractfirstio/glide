package glide.data

import glide.model.AttendanceAttendee
import glide.model.SoldPlan
import glide.model.Class
import java.time.LocalDate

fun attendeesForClass(cls: Class, sessionDate: LocalDate): List<AttendanceAttendee> =
    cls.soldPlanIds
        .filter { soldPlanId -> isSoldPlanOnClassSession(soldPlanId, cls, sessionDate) }
        .mapNotNull { soldPlanId -> findSoldPlanById(soldPlanId) }
        .flatMap { soldPlan -> soldPlan.attendeesForClassRoster() }

private fun SoldPlan.attendeesForClassRoster(): List<AttendanceAttendee> {
    val household = resolveMainClient()?.name?.takeIf { it.isNotBlank() } ?: "Household"
    val attendees = mutableListOf<AttendanceAttendee>()
    if (mainClientAttendsClass) {
        val main = resolveMainClient()
        val name = main?.name?.trim()?.ifBlank { "Main client" } ?: "Main client"
        attendees.add(
            AttendanceAttendee(
                key = "client:$mainClientId",
                displayName = name,
                soldPlanId = id,
                householdLabel = household,
            ),
        )
    }
    resolveStudents().forEach { person ->
        val name = person.name.trim().ifBlank { "Student" }
        attendees.add(
            AttendanceAttendee(
                key = "student:${person.id}",
                displayName = name,
                soldPlanId = id,
                householdLabel = household,
            ),
        )
    }
    return attendees
}
