package glide.data

import glide.model.ClassAttendee
import glide.model.PeopleGroup
import glide.model.ScheduledClass

fun attendeesForClass(scheduledClass: ScheduledClass): List<ClassAttendee> =
    scheduledClass.customerGroupIds
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
