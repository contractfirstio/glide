package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.PeopleGroupType
import glide.model.RelatedPerson

object RelatedPersonStore {
    private val _people = mutableStateListOf<RelatedPerson>()

    val all: List<RelatedPerson> get() = _people

    /** Related people linked to at least one customer (converted) people group. */
    val onCustomerPacks: List<RelatedPerson> get() =
        _people.filter { isOnCustomerPack(it.id) }

    /**
     * Related people for the Related panel — all on customer packs, those in
     * [customerGroupId] when a customer group is selected, or everyone linked to [contactId]
     * across that contact's leads and customer groups.
     */
    fun forRelatedPanel(customerGroupId: String?, contactId: String? = null): List<RelatedPerson> =
        when {
            customerGroupId != null ->
                PeopleGroupStore.findById(customerGroupId)
                    ?.takeIf { it.type == PeopleGroupType.CUSTOMER }
                    ?.resolveRelatedPeople()
                    ?: emptyList()
            contactId != null -> relatedPeopleForMainContact(contactId)
            else -> onCustomerPacks
        }

    private fun relatedPeopleForMainContact(contactId: String): List<RelatedPerson> =
        PeopleGroupStore.all
            .filter { it.mainContactId == contactId }
            .flatMap { it.relatedPersonIds }
            .distinct()
            .mapNotNull { findById(it) }

    fun isOnCustomerPack(personId: String): Boolean =
        PeopleGroupStore.customers.any { personId in it.relatedPersonIds }

    fun customerPackCount(personId: String): Int =
        PeopleGroupStore.customers.count { personId in it.relatedPersonIds }

    fun create(person: RelatedPerson) {
        _people.add(person)
    }

    fun update(person: RelatedPerson) {
        val index = _people.indexOfFirst { it.id == person.id }
        if (index >= 0) {
            _people[index] = person
        }
    }

    fun delete(id: String) {
        if (PeopleGroupStore.customers.any { id in it.relatedPersonIds }) return
        _people.removeAll { it.id == id }
        PeopleGroupStore.removeRelatedPersonFromAllGroups(id)
    }

    fun findById(id: String): RelatedPerson? = _people.find { it.id == id }

    fun peopleGroupsFor(personId: String): List<String> =
        PeopleGroupStore.all
            .filter { personId in it.relatedPersonIds }
            .map { it.id }
}
