package glide.model

import java.util.UUID

enum class PlanEnrollmentStatus(val label: String) {
    ACTIVE("Active"),
    /** Finishing the current plan period; renewal is stopped. */
    CANCELLING("Cancelling"),
    CANCELLED("Cancelled"),
}

fun PlanEnrollmentStatus.isOngoing(): Boolean =
    this == PlanEnrollmentStatus.ACTIVE || this == PlanEnrollmentStatus.CANCELLING

data class PlanEnrollment(
    val id: String = UUID.randomUUID().toString(),
    val peopleGroupId: String,
    val planSnapshot: PlanSnapshot,
    val status: PlanEnrollmentStatus = PlanEnrollmentStatus.ACTIVE,
    val startedAtMillis: Long = System.currentTimeMillis(),
    /** Scheduled class sessions in this plan period count from this time (resets when a plan bill is paid). */
    val planPeriodStartedAtMillis: Long = startedAtMillis,
    /** Set when renewal is cancelled; plan period continues until scheduled complete. */
    val renewalStoppedAtMillis: Long? = null,
    val notes: String = "",
)
