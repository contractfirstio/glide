package glide.model

import java.util.UUID

/** Main contact — can be linked to multiple customer people groups, each with its own pack. */
data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dateOfBirth: String = "",
    val email: String = "",
    val phone: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)
