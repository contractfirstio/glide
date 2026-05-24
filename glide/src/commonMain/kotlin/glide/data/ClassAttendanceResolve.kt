package glide.data

import glide.model.ClassAttendee
import glide.model.PeopleGroup
import glide.model.ScheduledClass
import java.time.LocalDate

fun attendeesForClass(scheduledClass: ScheduledClass, sessionDate: LocalDate): List<ClassAttendee> =
    scheduledClass.customerGroupIds
        .filter { groupId -> isPeopleGroupOnClassSession(groupId, scheduledClass, sessionDate) }
        .mapNotNull { groupId -> PeopleGroupStore.findById(groupId) }
        .flatMap { group -> group.attendeesForClassRoster() }

private fun PeopleGroup.attendeesForClassRoster(): List<ClassAttendee> {
    val household = clientName.ifBlank { "Household" }
    val attendees = mutableListOf<ClassAttendee>()
    if (mainClientAttendsClass && hasResolvableMainClient()) {
        val main = resolveMainClient()
        val name = main.name.trim().ifBlank { "Main client" }
        val key = mainClientId?.let { "client:$it" } ?: "legacy-main:$id"
        attendees.add(
            ClassAttendee(
                key = key,
                displayName = name,
                peopleGroupId = id,
                householdLabel = household,
            ),
        )
    }
    resolveStudents().forEach { person ->
        val name = person.name.trim().ifBlank { "Student" }
        attendees.add(
            ClassAttendee(
                key = "student:${person.id}",
                displayName = name,
                peopleGroupId = id,
                householdLabel = household,
            ),
        )
    }
    return attendees
}
