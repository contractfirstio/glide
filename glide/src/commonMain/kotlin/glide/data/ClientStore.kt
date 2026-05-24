package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Client
import glide.model.PeopleGroupType

object ClientStore {
    private val _clients = mutableStateListOf<Client>()

    val all: List<Client> get() = _clients

    /**
     * Clients for the Clients panel — all clients, the main client for [customerGroupId],
     * main clients on groups with [planId], or on groups that include [relatedPersonId].
     */
    fun forClientsPanel(
        customerGroupId: String? = null,
        planId: String? = null,
        relatedPersonId: String? = null,
    ): List<Client> =
        when {
            customerGroupId != null ->
                PeopleGroupStore.findById(customerGroupId)
                    ?.takeIf { it.type == PeopleGroupType.CUSTOMER }
                    ?.mainClientId
                    ?.let { findById(it) }
                    ?.let { listOf(it) }
                    ?: emptyList()
            planId != null ->
                PeopleGroupStore.all
                    .filter { it.planId == planId }
                    .mapNotNull { it.mainClientId }
                    .distinct()
                    .mapNotNull { findById(it) }
            relatedPersonId != null ->
                PeopleGroupStore.all
                    .filter { relatedPersonId in it.relatedPersonIds }
                    .mapNotNull { it.mainClientId }
                    .distinct()
                    .mapNotNull { findById(it) }
            else -> all
        }

    /** Only call from lead conversion — clients are not created elsewhere. */
    fun createFromLeadConversion(client: Client) {
        _clients.add(client)
    }

    /** Sample / dev data only. */
    internal fun seed(client: Client) {
        _clients.add(client)
    }

    fun update(client: Client) {
        val index = _clients.indexOfFirst { it.id == client.id }
        if (index >= 0) {
            _clients[index] = client
        }
    }

    fun isOnSoldPlan(clientId: String): Boolean =
        PeopleGroupStore.customers.any { it.mainClientId == clientId }

    fun canDelete(clientId: String): Boolean = !isOnSoldPlan(clientId)

    fun soldPlanCount(clientId: String): Int =
        PeopleGroupStore.customers.count { it.mainClientId == clientId }

    fun delete(id: String) {
        if (isOnSoldPlan(id)) return
        _clients.removeAll { it.id == id }
    }

    fun findById(id: String): Client? = _clients.find { it.id == id }

    fun peopleGroupsFor(clientId: String): List<String> =
        PeopleGroupStore.all
            .filter { it.mainClientId == clientId }
            .map { it.id }
}
