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
    /** Scheduled class sessions in this pack period count from this time (resets when a pack bill is paid). */
    val packPeriodStartedAtMillis: Long = startedAtMillis,
    val notes: String = "",
)
