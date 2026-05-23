package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Contact
import glide.model.PeopleGroupType

object ContactStore {
    private val _contacts = mutableStateListOf<Contact>()

    val all: List<Contact> get() = _contacts

    /**
     * Contacts for the Contacts panel — all contacts, the main contact for [customerGroupId],
     * main contacts on groups with [planId], or on groups that include [relatedPersonId].
     */
    fun forContactsPanel(
        customerGroupId: String? = null,
        planId: String? = null,
        relatedPersonId: String? = null,
    ): List<Contact> =
        when {
            customerGroupId != null ->
                PeopleGroupStore.findById(customerGroupId)
                    ?.takeIf { it.type == PeopleGroupType.CUSTOMER }
                    ?.mainContactId
                    ?.let { findById(it) }
                    ?.let { listOf(it) }
                    ?: emptyList()
            planId != null ->
                PeopleGroupStore.all
                    .filter { it.planId == planId }
                    .mapNotNull { it.mainContactId }
                    .distinct()
                    .mapNotNull { findById(it) }
            relatedPersonId != null ->
                PeopleGroupStore.all
                    .filter { relatedPersonId in it.relatedPersonIds }
                    .mapNotNull { it.mainContactId }
                    .distinct()
                    .mapNotNull { findById(it) }
            else -> all
        }

    /** Only call from lead conversion — contacts are not created elsewhere. */
    fun createFromLeadConversion(contact: Contact) {
        _contacts.add(contact)
    }

    /** Sample / dev data only. */
    internal fun seed(contact: Contact) {
        _contacts.add(contact)
    }

    fun update(contact: Contact) {
        val index = _contacts.indexOfFirst { it.id == contact.id }
        if (index >= 0) {
            _contacts[index] = contact
        }
    }

    fun delete(id: String) {
        _contacts.removeAll { it.id == id }
    }

    fun findById(id: String): Contact? = _contacts.find { it.id == id }

    fun peopleGroupsFor(contactId: String): List<String> =
        PeopleGroupStore.all
            .filter { it.mainContactId == contactId }
            .map { it.id }
}
