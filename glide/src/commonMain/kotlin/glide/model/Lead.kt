package glide.model

import java.util.UUID

import kotlinx.serialization.Serializable
@Serializable
enum class LeadStatus(val label: String) {
    New("New"),
    Contacted("Contacted"),
    WaitingReply("Waiting Reply"),
}

/**
 * A lead is a draft household / package before conversion to a [SoldPlan].
 * Keeps draft main-client fields until [mainClientId] is linked at conversion.
 */
@Serializable
data class Lead(
    val id: String = UUID.randomUUID().toString(),
    val mainClientId: String? = null,
    /** Draft main client while [mainClientId] is null. */
    val clientName: String = "",
    val dateOfBirth: String = "",
    val email: String = "",
    val phone: String = "",
    val studentIds: List<String> = emptyList(),
    val status: LeadStatus = LeadStatus.New,
    val planId: String? = null,
    /** ISO date (yyyy-MM-dd) when the selected plan starts; required before marking a lead as sold. */
    val planStartDate: String = "",
    /** When false, the main client is not counted on classes; students always attend. */
    val mainClientAttendsClass: Boolean = false,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)