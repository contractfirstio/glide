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

/** People enrolled in a group: main contact plus related people. */
fun PeopleGroup.memberCount(): Int {
    val main = if (hasResolvableMainContact()) 1 else 0
    return main + relatedPersonIds.size
}

private fun Contact.toResolved() = ResolvedMainContact(
    name = name,
    dateOfBirth = dateOfBirth,
    email = email,
    phone = phone,
)
