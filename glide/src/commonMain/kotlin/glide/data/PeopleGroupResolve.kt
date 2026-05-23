package glide.data

import glide.model.Contact
import glide.model.PeopleGroup
import glide.model.RelatedPerson

data class ResolvedMainContact(
    val name: String,
    val dateOfBirth: String,
    val email: String,
    val phone: String,
)

fun PeopleGroup.resolveMainContact(): ResolvedMainContact {
    mainContactId?.let { id ->
        ContactStore.findById(id)?.let { contact ->
            return contact.toResolved()
        }
    }
    return ResolvedMainContact(
        name = contactName,
        dateOfBirth = dateOfBirth,
        email = email,
        phone = phone,
    )
}

fun PeopleGroup.resolveRelatedPeople(): List<RelatedPerson> =
    relatedPersonIds.mapNotNull { RelatedPersonStore.findById(it) }

fun PeopleGroup.hasResolvableMainContact(): Boolean =
    mainContactId != null || contactName.isNotBlank()

/** Household size (main contact plus related people). */
fun PeopleGroup.memberCount(): Int {
    val main = if (hasResolvableMainContact()) 1 else 0
    return main + relatedPersonIds.size
}

/** People who take the class: related always; main contact only when [mainContactAttendsClass]. */
fun PeopleGroup.classAttendeeCount(): Int {
    val main = if (mainContactAttendsClass && hasResolvableMainContact()) 1 else 0
    return main + relatedPersonIds.size
}

/** Names shown on the class calendar (attending members only). */
fun PeopleGroup.rosterNameLabels(): List<String> {
    val names = mutableListOf<String>()
    if (mainContactAttendsClass && hasResolvableMainContact()) {
        val main = resolveMainContact()
        if (main.name.isNotBlank()) {
            names.add(main.name.trim())
        }
    }
    resolveRelatedPeople().forEach { person ->
        if (person.name.isNotBlank()) {
            names.add(person.name.trim())
        }
    }
    return names
}

private fun Contact.toResolved() = ResolvedMainContact(
    name = name,
    dateOfBirth = dateOfBirth,
    email = email,
    phone = phone,
)
