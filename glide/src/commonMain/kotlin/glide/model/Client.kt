package glide.model

import java.util.UUID

import kotlinx.serialization.Serializable
/** Main client — can be linked to multiple customer people groups, each with its own plan. */
@Serializable
data class Client(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dateOfBirth: String = "",
    val email: String = "",
    val phone: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)