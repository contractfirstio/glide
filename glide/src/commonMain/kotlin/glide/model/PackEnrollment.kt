package glide.model

import java.util.UUID

enum class PackEnrollmentStatus(val label: String) {
    ACTIVE("Active"),
    CANCELLED("Cancelled"),
}

data class PackEnrollment(
    val id: String = UUID.randomUUID().toString(),
    val peopleGroupId: String,
    val planSnapshot: PlanSnapshot,
    val status: PackEnrollmentStatus = PackEnrollmentStatus.ACTIVE,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
)
