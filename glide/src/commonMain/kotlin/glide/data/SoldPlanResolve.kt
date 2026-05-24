package glide.data

import glide.model.SoldPlan
import glide.model.Student

fun SoldPlan.resolveMainClient(): ResolvedMainClient? =
    ClientStore.findById(mainClientId)?.toResolved()

fun SoldPlan.resolveStudents(): List<Student> =
    studentIds.mapNotNull { StudentStore.findById(it) }

fun SoldPlan.hasResolvableMainClient(): Boolean = mainClientId.isNotBlank()

/** Household size (main client plus students). */
fun SoldPlan.memberCount(): Int = 1 + studentIds.size

/** People who take the class: students always; main client only when [mainClientAttendsClass]. */
fun SoldPlan.classAttendeeCount(): Int {
    val main = if (mainClientAttendsClass) 1 else 0
    return main + studentIds.size
}

fun SoldPlan.hasClassParticipant(): Boolean = classAttendeeCount() > 0

/** Names shown on the class calendar (attending members only). */
fun SoldPlan.rosterNameLabels(): List<String> {
    val names = mutableListOf<String>()
    if (mainClientAttendsClass) {
        resolveMainClient()?.name?.trim()?.takeIf { it.isNotBlank() }?.let { names.add(it) }
    }
    resolveStudents().forEach { person ->
        if (person.name.isNotBlank()) {
            names.add(person.name.trim())
        }
    }
    return names
}

private fun glide.model.Client.toResolved() = ResolvedMainClient(
    name = name,
    dateOfBirth = dateOfBirth,
    email = email,
    phone = phone,
)
