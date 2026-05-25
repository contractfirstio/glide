package glide.data

import glide.model.Client
import glide.model.Lead
import glide.model.Student

data class ResolvedMainClient(
    val name: String,
    val dateOfBirth: String,
    val email: String,
    val phone: String,
)

fun Lead.resolveMainClient(): ResolvedMainClient {
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

fun Lead.hasResolvableMainClient(): Boolean =
    mainClientId != null || clientName.isNotBlank()

/** Household size (main client plus students). */
fun Lead.memberCount(): Int {
    val main = if (hasResolvableMainClient()) 1 else 0
    return main + studentIds.size
}

/** People who take the class: students always; main client only when [mainClientAttendsClass]. */
fun Lead.classAttendeeCount(): Int {
    val main = if (mainClientAttendsClass && hasResolvableMainClient()) 1 else 0
    return main + studentIds.size
}

fun Lead.hasClassParticipant(): Boolean = classAttendeeCount() > 0

fun Lead.resolveStudents(): List<Student> =
    studentIds.mapNotNull { StudentStore.findById(it) }

private fun Client.toResolved() = ResolvedMainClient(
    name = name,
    dateOfBirth = dateOfBirth,
    email = email,
    phone = phone,
)
