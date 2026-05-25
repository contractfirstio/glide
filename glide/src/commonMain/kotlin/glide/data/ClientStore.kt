package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Client

object ClientStore {
    private val _clients = mutableStateListOf<Client>()

    val all: List<Client> get() = _clients

    /**
     * Clients for the Clients panel — all clients, the main client for [soldPlanId],
     * main clients on groups with [planId], or on groups that include [studentId].
     */
    fun forClientsPanel(
        soldPlanId: String? = null,
        planId: String? = null,
        studentId: String? = null,
    ): List<Client> =
        when {
            soldPlanId != null ->
                findSoldPlanById(soldPlanId)
                    
                    ?.mainClientId
                    ?.let { findById(it) }
                    ?.let { listOf(it) }
                    ?: emptyList()
            planId != null ->
                SoldPlanStore.all
                    .filter { it.planId == planId }
                    .mapNotNull { it.mainClientId }
                    .distinct()
                    .mapNotNull { findById(it) }
            studentId != null ->
                SoldPlanStore.all
                    .filter { studentId in it.studentIds }
                    .mapNotNull { it.mainClientId }
                    .distinct()
                    .mapNotNull { findById(it) }
            else -> all
        }

    /** Only call from lead conversion; callers persist after the full conversion completes. */
    fun createFromLeadConversion(client: Client) {
        _clients.add(client)
    }

    internal fun replaceAll(clients: List<Client>) {
        _clients.clear()
        _clients.addAll(clients)
    }

    fun update(client: Client) {
        val index = _clients.indexOfFirst { it.id == client.id }
        if (index >= 0) {
            _clients[index] = client
            persistAppData()
        }
    }

    fun isOnSoldPlan(clientId: String): Boolean =
        SoldPlanStore.all.any { it.mainClientId == clientId }

    fun canDelete(clientId: String): Boolean = !isOnSoldPlan(clientId)

    fun soldPlanCount(clientId: String): Int =
        SoldPlanStore.all.count { it.mainClientId == clientId }

    fun delete(id: String) {
        if (isOnSoldPlan(id)) return
        val removed = _clients.removeAll { it.id == id }
        if (removed) persistAppData()
    }

    fun findById(id: String): Client? = _clients.find { it.id == id }

    fun soldPlansFor(clientId: String): List<String> =
        SoldPlanStore.all
            .filter { it.mainClientId == clientId }
            .map { it.id }
}
