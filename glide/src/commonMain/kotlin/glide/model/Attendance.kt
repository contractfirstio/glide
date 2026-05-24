package glide.model

import kotlinx.serialization.Serializable
/** A single class session on a calendar date (class + ISO date). */
@Serializable
data class AttendanceSessionKey(
    val classId: String,
    /** ISO yyyy-MM-dd */
    val sessionDate: String,
)

@Serializable
enum class AttendanceStatus {
    PRESENT,
    ABSENT,
}

/** Per-student attendance for one class session. */
@Serializable
data class AttendanceRecord(
    val classId: String,
    val sessionDate: String,
    /** Stable roster key from [AttendanceAttendee.key]. */
    val attendeeKey: String,
    val status: AttendanceStatus,
    val recordedAtMillis: Long = System.currentTimeMillis(),
)

/** A person enrolled on a class roster (from sold plans). */
@Serializable
data class AttendanceAttendee(
    val key: String,
    val displayName: String,
    val soldPlanId: String,
    val householdLabel: String,
)