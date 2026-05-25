package glide.model

import java.util.UUID

import kotlinx.serialization.Serializable
/**
 * A sold plan is the customer unit (household / package) after lead conversion.
 * References [mainClientId] and [studentIds].
 */
@Serializable
data class SoldPlan(
    val id: String = UUID.randomUUID().toString(),
    val mainClientId: String,
    val studentIds: List<String> = emptyList(),
    val planId: String? = null,
    val planStartDate: String = "",
    /** When false, the main client is not counted on classes; students always attend. */
    val mainClientAttendsClass: Boolean = true,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)