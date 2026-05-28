package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.SoldPlan
import glide.model.Client
import glide.model.Lead
import glide.model.LeadStatus
import glide.model.parseIsoLocalDate
import java.time.LocalDate

object LeadStore {
    private val _leads = mutableStateListOf<Lead>()

    val all: List<Lead> get() = _leads

    private fun Lead.normalizedReferences(): Lead =
        copy(
            mainClientId = mainClientId?.takeIf { ClientStore.findById(it) != null },
            studentIds = studentIds.distinct().filter { StudentStore.findById(it) != null },
        )

    fun create(lead: Lead) {
        val normalized = lead.normalizedReferences()
        require(normalized.hasResolvableMainClient()) {
            "Link a client or enter a main client name."
        }
        _leads.add(normalized)
        persistAppData()
    }

    internal fun replaceAll(leads: List<Lead>) {
        _leads.clear()
        _leads.addAll(leads)
    }

    fun update(lead: Lead): Boolean {
        val index = _leads.indexOfFirst { it.id == lead.id }
        if (index < 0) return false
        val normalized = lead.normalizedReferences()
        _leads[index] = normalized.copy(
            clientName = if (normalized.mainClientId != null) "" else normalized.clientName,
            dateOfBirth = if (normalized.mainClientId != null) "" else normalized.dateOfBirth,
            email = if (normalized.mainClientId != null) "" else normalized.email,
            phone = if (normalized.mainClientId != null) "" else normalized.phone,
        )
        persistAppData()
        return true
    }

    fun delete(id: String): Boolean {
        val removed = _leads.removeAll { it.id == id }
        if (removed) persistAppData()
        return removed
    }

    fun findById(id: String): Lead? = _leads.find { it.id == id }

    fun removeStudentFromAllLeads(personId: String) {
        _leads.forEachIndexed { index, lead ->
            if (personId in lead.studentIds) {
                _leads[index] = lead.copy(
                    studentIds = lead.studentIds.filter { it != personId },
                )
            }
        }
        persistAppData()
    }

    /** Converts a lead to a sold plan: creates a [Client] if needed, removes lead, adds sold plan. */
    fun convertToSoldPlan(id: String): Boolean {
        val lead = findById(id) ?: return false
        if (lead.planId.isNullOrBlank()) return false
        if (lead.planStartDate.isBlank() || parseIsoLocalDate(lead.planStartDate) == null) return false
        if (!lead.hasResolvableMainClient()) return false
        if (!lead.hasClassParticipant()) return false

        val clientId = lead.mainClientId ?: run {
            val resolved = lead.resolveMainClient()
            val client = Client(
                name = resolved.name.trim(),
                dateOfBirth = resolved.dateOfBirth.trim(),
                email = resolved.email.trim(),
                phone = resolved.phone.trim(),
            )
            ClientStore.createFromLeadConversion(client)
            client.id
        }

        val soldPlan = SoldPlan(
            id = lead.id,
            mainClientId = clientId,
            studentIds = lead.studentIds,
            planId = lead.planId,
            planStartDate = lead.planStartDate,
            mainClientAttendsClass = lead.mainClientAttendsClass,
            notes = lead.notes,
            createdAtMillis = lead.createdAtMillis,
        )
        if (!delete(id)) return false
        SoldPlanStore.create(soldPlan)
        BillingService.onCustomerConverted(id)
        persistAppData()
        return true
    }
}
