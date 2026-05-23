package glide.model

/** A single class session on a calendar date (recurring class + ISO date). */
data class ClassSessionKey(
    val scheduledClassId: String,
    /** ISO yyyy-MM-dd */
    val sessionDate: String,
)

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
}

/** Per-student attendance for one class session. */
data class ClassAttendanceRecord(
    val scheduledClassId: String,
    val sessionDate: String,
    /** Stable roster key from [ClassAttendee.key]. */
    val attendeeKey: String,
    val status: AttendanceStatus,
    val recordedAtMillis: Long = System.currentTimeMillis(),
)

/** A person enrolled on a class roster (from customer groups). */
data class ClassAttendee(
    val key: String,
    val displayName: String,
    val peopleGroupId: String,
    val householdLabel: String,
)
