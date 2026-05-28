package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Lead
import glide.model.LeadStatus
import glide.model.SoldPlan
import java.time.LocalDate

object SoldPlanStore {
    private val _soldPlans = mutableStateListOf<SoldPlan>()

    val all: List<SoldPlan> get() = _soldPlans

    private fun SoldPlan.normalizedReferences(): SoldPlan? {
        if (ClientStore.findById(mainClientId) == null) return null
        return copy(
            studentIds = studentIds.distinct().filter { StudentStore.findById(it) != null },
            planId = planId?.takeIf { PlanStore.findById(it) != null },
        )
    }

    /**
     * Sold plans for the Sold Plans panel — all plans, those on [planId], whose main
     * client is [clientId], or those that include [studentId].
     */
    fun forSoldPlansPanel(
        clientId: String? = null,
        studentId: String? = null,
        planId: String? = null,
    ): List<SoldPlan> =
        when {
            studentId != null ->
                _soldPlans.filter { studentId in it.studentIds }
            clientId != null -> _soldPlans.filter { it.mainClientId == clientId }
            planId != null -> _soldPlans.filter { it.planId == planId }
            else -> _soldPlans
        }

    /** Sold plans enrolled on classes matching the active scheduling filter. */
    fun forSchedulingSoldPlans(
        classId: String? = null,
        termId: String? = null,
        locationId: String? = null,
    ): List<SoldPlan> {
        val enrolledIds = when {
            classId != null ->
                ClassStore.findById(classId)?.soldPlanIds?.toSet()
            termId != null ->
                ClassStore.soldPlanIdsForTerm(termId)
            locationId != null ->
                ClassStore.soldPlanIdsForLocation(locationId)
            else -> null
        }
        return when {
            enrolledIds == null -> _soldPlans
            enrolledIds.isEmpty() -> emptyList()
            else -> _soldPlans.filter { it.id in enrolledIds }
        }
    }

    fun create(soldPlan: SoldPlan) {
        val normalized = soldPlan.normalizedReferences()
        require(normalized != null) {
            "Linked client not found."
        }
        _soldPlans.add(normalized)
        persistAppData()
    }

    internal fun replaceAll(soldPlans: List<SoldPlan>) {
        _soldPlans.clear()
        _soldPlans.addAll(soldPlans.mapNotNull { it.normalizedReferences() })
    }

    fun findById(id: String): SoldPlan? = _soldPlans.find { it.id == id }

    fun delete(id: String): Boolean = revertToLead(id)

    /**
     * Removes sold-plan billing and class data, then turns the sold plan back into a lead.
     * Same record id is kept so clients and students stay linked.
     */
    fun revertToLead(id: String): Boolean {
        val index = _soldPlans.indexOfFirst { it.id == id }
        if (index < 0) return false
        val soldPlan = _soldPlans[index]
        if (!canDeleteSoldPlan(id)) return false

        purgeSoldPlanData(id)

        val lead = Lead(
            id = soldPlan.id,
            mainClientId = soldPlan.mainClientId,
            studentIds = soldPlan.studentIds,
            status = LeadStatus.New,
            planId = soldPlan.planId,
            planStartDate = soldPlan.planStartDate,
            mainClientAttendsClass = soldPlan.mainClientAttendsClass,
            notes = soldPlan.notes,
            createdAtMillis = soldPlan.createdAtMillis,
        )
        _soldPlans.removeAt(index)
        LeadStore.create(lead)
        persistAppData()
        return true
    }

    /** Copies a sold plan into a new editable lead (same client, students, and plan). */
    fun cloneToLead(soldPlanId: String): Lead? {
        val source = findById(soldPlanId) ?: return null

        val lead = Lead(
            mainClientId = source.mainClientId,
            studentIds = source.studentIds,
            status = LeadStatus.New,
            planId = source.planId,
            planStartDate = LocalDate.now().toString(),
            mainClientAttendsClass = source.mainClientAttendsClass,
            notes = source.notes,
        )
        LeadStore.create(lead)
        return lead
    }
}

fun findSoldPlanById(id: String): SoldPlan? = SoldPlanStore.findById(id)
