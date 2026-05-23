package glide.model

import java.util.UUID

/** Links a customer [PeopleGroup] to a [ScheduledClass]. */
data class ClassGroupAssignment(
    val id: String = UUID.randomUUID().toString(),
    val classId: String,
    val peopleGroupId: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
