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
    val household = contactName.ifBlank { "Household" }
    val attendees = mutableListOf<ClassAttendee>()
    if (mainContactAttendsClass && hasResolvableMainContact()) {
        val main = resolveMainContact()
        val name = main.name.trim().ifBlank { "Main contact" }
        val key = mainContactId?.let { "contact:$it" } ?: "legacy-main:$id"
        attendees.add(
            ClassAttendee(
                key = key,
                displayName = name,
                peopleGroupId = id,
                householdLabel = household,
            ),
        )
    }
    resolveRelatedPeople().forEach { person ->
        val name = person.name.trim().ifBlank { "Related person" }
        attendees.add(
            ClassAttendee(
                key = "related:${person.id}",
                displayName = name,
                peopleGroupId = id,
                householdLabel = household,
            ),
        )
    }
    return attendees
}
