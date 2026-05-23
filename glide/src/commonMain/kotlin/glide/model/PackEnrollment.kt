package glide.model

import java.util.UUID

enum class PackEnrollmentStatus(val label: String) {
    ACTIVE("Active"),
    /** Finishing the current pack period; renewal is stopped. */
    CANCELLING("Cancelling"),
    CANCELLED("Cancelled"),
}

fun PackEnrollmentStatus.isOngoing(): Boolean =
    this == PackEnrollmentStatus.ACTIVE || this == PackEnrollmentStatus.CANCELLING

data class PackEnrollment(
    val id: String = UUID.randomUUID().toString(),
    val peopleGroupId: String,
    val planSnapshot: PlanSnapshot,
    val status: PackEnrollmentStatus = PackEnrollmentStatus.ACTIVE,
    val startedAtMillis: Long = System.currentTimeMillis(),
    /** Scheduled class sessions in this pack period count from this time (resets when a pack bill is paid). */
    val packPeriodStartedAtMillis: Long = startedAtMillis,
    /** Set when renewal is cancelled; pack period continues until scheduled complete. */
    val renewalStoppedAtMillis: Long? = null,
    val notes: String = "",
)
