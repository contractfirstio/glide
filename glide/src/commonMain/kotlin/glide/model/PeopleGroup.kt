package glide.model

import java.util.UUID

enum class PeopleGroupType {
    LEAD,
    CUSTOMER,
}

enum class PeopleGroupStatus(val label: String) {
    New("New"),
    Contacted("Contacted"),
    WaitingReply("Waiting Reply"),
}

/**
 * A people group is the customer unit (household / package). Leads keep draft main-client
 * fields until conversion; customers reference [mainClientId] and [studentIds].
 */
data class PeopleGroup(
    val id: String = UUID.randomUUID().toString(),
    val type: PeopleGroupType = PeopleGroupType.LEAD,
    val mainClientId: String? = null,
    /** Draft main client while [type] is LEAD and [mainClientId] is null. */
    val clientName: String = "",
    val dateOfBirth: String = "",
    val email: String = "",
    val phone: String = "",
    val studentIds: List<String> = emptyList(),
    val status: PeopleGroupStatus = PeopleGroupStatus.New,
    val planId: String? = null,
    /** ISO date (yyyy-MM-dd) when the selected plan starts; required before marking a lead as sold. */
    val planStartDate: String = "",
    /** When false, the main client is not counted on classes; students always attend. */
    val mainClientAttendsClass: Boolean = true,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)
