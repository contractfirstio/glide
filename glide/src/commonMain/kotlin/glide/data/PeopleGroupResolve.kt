package glide.data

import glide.model.Client
import glide.model.PeopleGroup
import glide.model.Student

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

fun PeopleGroup.resolveStudents(): List<Student> =
    studentIds.mapNotNull { StudentStore.findById(it) }

fun PeopleGroup.hasResolvableMainClient(): Boolean =
    mainClientId != null || clientName.isNotBlank()

/** Household size (main client plus students). */
fun PeopleGroup.memberCount(): Int {
    val main = if (hasResolvableMainClient()) 1 else 0
    return main + studentIds.size
}

/** People who take the class: students always; main client only when [mainClientAttendsClass]. */
fun PeopleGroup.classAttendeeCount(): Int {
    val main = if (mainClientAttendsClass && hasResolvableMainClient()) 1 else 0
    return main + studentIds.size
}

fun PeopleGroup.hasClassParticipant(): Boolean = classAttendeeCount() > 0

/** Names shown on the class calendar (attending members only). */
fun PeopleGroup.rosterNameLabels(): List<String> {
    val names = mutableListOf<String>()
    if (mainClientAttendsClass && hasResolvableMainClient()) {
        val main = resolveMainClient()
        if (main.name.isNotBlank()) {
            names.add(main.name.trim())
        }
    }
    resolveStudents().forEach { person ->
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
