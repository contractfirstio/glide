package glide.data

import glide.model.Client
import glide.model.PeopleGroup
import glide.model.RelatedPerson

data class ResolvedMainClient(
    val name: String,
    val dateOfBirth: String,
    val email: String,
    val phone: String,
)

fun PeopleGroup.resolveMainClient(): ResolvedMainClient {
    mainClientId?.let { id ->
        ClientStore.findById(id)?.let { client ->
            return client.toResolved()
        }
    }
    return ResolvedMainClient(
        name = clientName,
        dateOfBirth = dateOfBirth,
        email = email,
        phone = phone,
    )
}

fun PeopleGroup.resolveRelatedPeople(): List<RelatedPerson> =
    relatedPersonIds.mapNotNull { RelatedPersonStore.findById(it) }

fun PeopleGroup.hasResolvableMainClient(): Boolean =
    mainClientId != null || clientName.isNotBlank()

/** Household size (main client plus related people). */
fun PeopleGroup.memberCount(): Int {
    val main = if (hasResolvableMainClient()) 1 else 0
    return main + relatedPersonIds.size
}

/** People who take the class: related always; main client only when [mainClientAttendsClass]. */
fun PeopleGroup.classAttendeeCount(): Int {
    val main = if (mainClientAttendsClass && hasResolvableMainClient()) 1 else 0
    return main + relatedPersonIds.size
}

/** Names shown on the class calendar (attending members only). */
fun PeopleGroup.rosterNameLabels(): List<String> {
    val names = mutableListOf<String>()
    if (mainClientAttendsClass && hasResolvableMainClient()) {
        val main = resolveMainClient()
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

private fun Client.toResolved() = ResolvedMainClient(
    name = name,
    dateOfBirth = dateOfBirth,
    email = email,
    phone = phone,
)
