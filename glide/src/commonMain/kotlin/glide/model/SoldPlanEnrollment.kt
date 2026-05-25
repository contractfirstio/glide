package glide.model

import java.util.UUID

import kotlinx.serialization.Serializable
@Serializable
enum class SoldPlanEnrollmentStatus(val label: String) {
    ACTIVE("Active"),
    /** Finishing the current plan period; renewal is stopped. */
    CANCELLING("Cancelling"),
    CANCELLED("Cancelled"),
}

fun SoldPlanEnrollmentStatus.isOngoing(): Boolean =
    this == SoldPlanEnrollmentStatus.ACTIVE || this == SoldPlanEnrollmentStatus.CANCELLING

@Serializable
data class SoldPlanEnrollment(
    val id: String = UUID.randomUUID().toString(),
    val soldPlanId: String,
    val planSnapshot: PlanSnapshot,
    val status: SoldPlanEnrollmentStatus = SoldPlanEnrollmentStatus.ACTIVE,
    val startedAtMillis: Long = System.currentTimeMillis(),
    /** Scheduled class sessions in this plan period count from this time (resets when a plan bill is paid). */
    val planPeriodStartedAtMillis: Long = startedAtMillis,
    /** Set when renewal is cancelled; plan period continues until scheduled complete. */
    val renewalStoppedAtMillis: Long? = null,
    val notes: String = "",
)