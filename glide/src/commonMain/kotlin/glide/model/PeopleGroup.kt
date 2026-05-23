package glide.model

import java.util.UUID

enum class PeopleGroupType {
    LEAD,
    CUSTOMER,
}

enum class PeopleGroupStatus(val label: String) {
    New("New"),
    Contacted("Contacted"),
    Proposal("Proposal"),
    Won("Won"),
    Lost("Lost"),
}

/**
 * A people group is the customer unit (household / package). Leads keep draft main-contact
 * fields until conversion; customers reference [mainContactId] and [relatedPersonIds].
 */
data class PeopleGroup(
    val id: String = UUID.randomUUID().toString(),
    val type: PeopleGroupType = PeopleGroupType.LEAD,
    val mainContactId: String? = null,
    /** Draft main contact while [type] is LEAD and [mainContactId] is null. */
    val contactName: String = "",
    val dateOfBirth: String = "",
    val email: String = "",
    val phone: String = "",
    val relatedPersonIds: List<String> = emptyList(),
    val status: PeopleGroupStatus = PeopleGroupStatus.New,
    val planId: String? = null,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)
