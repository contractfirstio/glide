package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Contact

object ContactStore {
    private val _contacts = mutableStateListOf<Contact>()

    val all: List<Contact> get() = _contacts

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
