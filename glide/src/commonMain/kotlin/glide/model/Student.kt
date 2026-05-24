package glide.model

import java.util.UUID

/** Student — can belong to multiple people groups via group membership lists. */
data class Student(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dateOfBirth: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)
