package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Contact
import glide.model.parseIsoLocalDate
import glide.model.PeopleGroup
import glide.model.PeopleGroupStatus
import glide.model.PeopleGroupType
import java.time.LocalDate

object PeopleGroupStore {
    private val _groups = mutableStateListOf<PeopleGroup>()

    val all: List<PeopleGroup> get() = _groups
    val leads: List<PeopleGroup> get() = _groups.filter { it.type == PeopleGroupType.LEAD }
    val customers: List<PeopleGroup> get() = _groups.filter { it.type == PeopleGroupType.CUSTOMER }

    /**
     * Customer groups for the Customers panel — all groups, those on [planId], whose main
     * contact is [contactId], or those that include [relatedPersonId].
     */
    fun forCustomersPanel(
        contactId: String? = null,
        relatedPersonId: String? = null,
        planId: String? = null,
    ): List<PeopleGroup> =
        when {
            relatedPersonId != null ->
                customers.filter { relatedPersonId in it.relatedPersonIds }
            contactId != null -> customers.filter { it.mainContactId == contactId }
            planId != null -> customers.filter { it.planId == planId }
            else -> customers
        }

    /** Sold plans enrolled on classes matching the active scheduling filter. */
    fun forSchedulingSoldPlans(
        classId: String? = null,
        termId: String? = null,
        locationId: String? = null,
    ): List<PeopleGroup> {
        val enrolledIds = when {
            classId != null ->
                ScheduledClassStore.findById(classId)?.customerGroupIds?.toSet()
            termId != null ->
                ScheduledClassStore.customerGroupIdsForTerm(termId)
            locationId != null ->
                ScheduledClassStore.customerGroupIdsForLocation(locationId)
            else -> null
        }
        return when {
            enrolledIds == null -> customers
            enrolledIds.isEmpty() -> emptyList()
            else -> customers.filter { it.id in enrolledIds }
        }
    }

    /** Sample / dev data only — inserts an already-converted customer group. */
    internal fun seedCustomer(group: PeopleGroup) {
        require(group.type == PeopleGroupType.CUSTOMER) {
            "Seed customer groups must have type CUSTOMER."
        }
        require(group.mainContactId != null) { "Seed customer groups need a main contact." }
        require(ContactStore.findById(group.mainContactId) != null) {
            "Seed customer main contact must exist."
        }
        _groups.add(group)
    }

    fun create(group: PeopleGroup) {
        require(group.type == PeopleGroupType.LEAD) {
            "People groups must be created as leads first."
        }
        if (group.mainContactId != null) {
            require(ContactStore.findById(group.mainContactId) != null) {
                "Linked contact not found."
            }
        }
        require(group.hasResolvableMainContact()) {
            "Link a contact or enter a main contact name."
        }
        _groups.add(group)
    }

    fun update(group: PeopleGroup): Boolean {
        val index = _groups.indexOfFirst { it.id == group.id }
        if (index < 0) return false
        val existing = _groups[index]
        if (existing.type == PeopleGroupType.CUSTOMER) return false
        _groups[index] = group.copy(
            contactName = if (group.mainContactId != null) "" else group.contactName,
            dateOfBirth = if (group.mainContactId != null) "" else group.dateOfBirth,
            email = if (group.mainContactId != null) "" else group.email,
            phone = if (group.mainContactId != null) "" else group.phone,
        )
        return true
    }

    fun delete(id: String): Boolean {
        val group = findById(id) ?: return false
        return when (group.type) {
            PeopleGroupType.CUSTOMER -> revertSoldPlanToLead(id)
            PeopleGroupType.LEAD -> {
                _groups.removeAll { it.id == id }
                true
            }
        }
    }

    /**
     * Removes sold-plan billing and class data, then turns the customer group back into a lead.
     * Same record id is kept so contacts and related people stay linked.
     */
    fun revertSoldPlanToLead(id: String): Boolean {
        val index = _groups.indexOfFirst { it.id == id }
        if (index < 0) return false
        val group = _groups[index]
        if (group.type != PeopleGroupType.CUSTOMER) return false
        if (!canDeleteSoldPlan(id)) return false

        purgeSoldPlanData(id)

        _groups[index] = group.copy(
            type = PeopleGroupType.LEAD,
            status = PeopleGroupStatus.New,
            contactName = "",
            dateOfBirth = "",
            email = "",
            phone = "",
        )
        return true
    }

    fun findById(id: String): PeopleGroup? = _groups.find { it.id == id }

    fun removeRelatedPersonFromAllGroups(personId: String) {
        _groups.forEachIndexed { index, group ->
            if (group.type == PeopleGroupType.CUSTOMER) return@forEachIndexed
            if (personId in group.relatedPersonIds) {
                _groups[index] = group.copy(
                    relatedPersonIds = group.relatedPersonIds.filter { it != personId },
                )
            }
        }
    }

    /** Converts a lead: creates a [Contact], links it, and flips type to CUSTOMER. */
    fun convertToCustomer(id: String): Boolean {
        val group = findById(id) ?: return false
        if (group.type == PeopleGroupType.CUSTOMER) return false
        if (group.planId.isNullOrBlank()) return false
        if (group.planStartDate.isBlank() || parseIsoLocalDate(group.planStartDate) == null) return false
        if (!group.hasResolvableMainContact()) return false

        val contactId = group.mainContactId ?: run {
            val resolved = group.resolveMainContact()
            val contact = Contact(
                name = resolved.name.trim(),
                dateOfBirth = resolved.dateOfBirth.trim(),
                email = resolved.email.trim(),
                phone = resolved.phone.trim(),
            )
            ContactStore.createFromLeadConversion(contact)
            contact.id
        }

        update(
            group.copy(
                type = PeopleGroupType.CUSTOMER,
                mainContactId = contactId,
                contactName = "",
                dateOfBirth = "",
                email = "",
                phone = "",
            ),
        ) || return false
        BillingService.onCustomerConverted(id)
        return true
    }

    /** Copies a locked customer group into a new editable lead (same contact, related people, and pack). */
    fun cloneToLead(customerGroupId: String): PeopleGroup? {
        val source = findById(customerGroupId) ?: return null
        if (source.type != PeopleGroupType.CUSTOMER) return null
        if (source.mainContactId == null) return null

        val lead = PeopleGroup(
            type = PeopleGroupType.LEAD,
            mainContactId = source.mainContactId,
            relatedPersonIds = source.relatedPersonIds,
            status = PeopleGroupStatus.New,
            planId = source.planId,
            planStartDate = LocalDate.now().toString(),
            mainContactAttendsClass = source.mainContactAttendsClass,
            notes = source.notes,
        )
        create(lead)
        return lead
    }
}
