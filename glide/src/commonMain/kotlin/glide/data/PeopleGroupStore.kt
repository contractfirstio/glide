package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Client
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
     * client is [clientId], or those that include [studentId].
     */
    fun forCustomersPanel(
        clientId: String? = null,
        studentId: String? = null,
        planId: String? = null,
    ): List<PeopleGroup> =
        when {
            studentId != null ->
                customers.filter { studentId in it.studentIds }
            clientId != null -> customers.filter { it.mainClientId == clientId }
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
        require(group.mainClientId != null) { "Seed customer groups need a main client." }
        require(ClientStore.findById(group.mainClientId) != null) {
            "Seed customer main client must exist."
        }
        _groups.add(group)
    }

    fun create(group: PeopleGroup) {
        require(group.type == PeopleGroupType.LEAD) {
            "People groups must be created as leads first."
        }
        if (group.mainClientId != null) {
            require(ClientStore.findById(group.mainClientId) != null) {
                "Linked client not found."
            }
        }
        require(group.hasResolvableMainClient()) {
            "Link a client or enter a main client name."
        }
        _groups.add(group)
    }

    fun update(group: PeopleGroup): Boolean {
        val index = _groups.indexOfFirst { it.id == group.id }
        if (index < 0) return false
        val existing = _groups[index]
        if (existing.type == PeopleGroupType.CUSTOMER) return false
        _groups[index] = group.copy(
            clientName = if (group.mainClientId != null) "" else group.clientName,
            dateOfBirth = if (group.mainClientId != null) "" else group.dateOfBirth,
            email = if (group.mainClientId != null) "" else group.email,
            phone = if (group.mainClientId != null) "" else group.phone,
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
     * Same record id is kept so clients and students stay linked.
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
            clientName = "",
            dateOfBirth = "",
            email = "",
            phone = "",
        )
        return true
    }

    fun findById(id: String): PeopleGroup? = _groups.find { it.id == id }

    fun removeStudentFromAllGroups(personId: String) {
        _groups.forEachIndexed { index, group ->
            if (group.type == PeopleGroupType.CUSTOMER) return@forEachIndexed
            if (personId in group.studentIds) {
                _groups[index] = group.copy(
                    studentIds = group.studentIds.filter { it != personId },
                )
            }
        }
    }

    /** Converts a lead: creates a [Client], links it, and flips type to CUSTOMER. */
    fun convertToCustomer(id: String): Boolean {
        val group = findById(id) ?: return false
        if (group.type == PeopleGroupType.CUSTOMER) return false
        if (group.planId.isNullOrBlank()) return false
        if (group.planStartDate.isBlank() || parseIsoLocalDate(group.planStartDate) == null) return false
        if (!group.hasResolvableMainClient()) return false

        val clientId = group.mainClientId ?: run {
            val resolved = group.resolveMainClient()
            val client = Client(
                name = resolved.name.trim(),
                dateOfBirth = resolved.dateOfBirth.trim(),
                email = resolved.email.trim(),
                phone = resolved.phone.trim(),
            )
            ClientStore.createFromLeadConversion(client)
            client.id
        }

        update(
            group.copy(
                type = PeopleGroupType.CUSTOMER,
                mainClientId = clientId,
                clientName = "",
                dateOfBirth = "",
                email = "",
                phone = "",
            ),
        ) || return false
        BillingService.onCustomerConverted(id)
        return true
    }

    /** Copies a locked customer group into a new editable lead (same client, students, and plan). */
    fun cloneToLead(customerGroupId: String): PeopleGroup? {
        val source = findById(customerGroupId) ?: return null
        if (source.type != PeopleGroupType.CUSTOMER) return null
        if (source.mainClientId == null) return null

        val lead = PeopleGroup(
            type = PeopleGroupType.LEAD,
            mainClientId = source.mainClientId,
            studentIds = source.studentIds,
            status = PeopleGroupStatus.New,
            planId = source.planId,
            planStartDate = LocalDate.now().toString(),
            mainClientAttendsClass = source.mainClientAttendsClass,
            notes = source.notes,
        )
        create(lead)
        return lead
    }
}
