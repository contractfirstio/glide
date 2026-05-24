package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.ScheduledClass
import glide.model.compareScheduledClasses
import glide.model.hasCustomerGroup
import glide.model.spansTerm
import glide.model.usesLocation

object ScheduledClassStore {
    private val _classes = mutableStateListOf<ScheduledClass>()
    val classes: List<ScheduledClass> get() = _classes

    fun create(scheduledClass: ScheduledClass) {
        _classes.add(scheduledClass)
    }

    fun update(scheduledClass: ScheduledClass) {
        val index = _classes.indexOfFirst { it.id == scheduledClass.id }
        if (index >= 0) {
            _classes[index] = scheduledClass
        }
    }

    fun soldPlanCount(classId: String): Int =
        findById(classId)?.customerGroupIds?.size ?: 0

    fun canEditTerms(classId: String): Boolean = soldPlanCount(classId) == 0

    fun canDelete(classId: String): Boolean = soldPlanCount(classId) == 0

    fun classTermsEditBlockReason(classId: String): String? {
        val soldPlans = soldPlanCount(classId)
        return if (soldPlans == 0) {
            null
        } else {
            "This class has $soldPlans sold plan${if (soldPlans == 1) "" else "s"} " +
                "and its term${if (soldPlans == 1) "" else "s"} cannot be changed."
        }
    }

    fun delete(id: String): Boolean {
        if (!canDelete(id)) return false
        _classes.removeAll { it.id == id }
        ClassAttendanceStore.clearForClass(id)
        PackClassScheduleStore.clearForClass(id)
        return true
    }

    fun findById(id: String): ScheduledClass? = _classes.find { it.id == id }

    fun findClassContainingCustomerGroup(
        groupId: String,
        excludeClassId: String? = null,
    ): ScheduledClass? =
        _classes.firstOrNull { groupId in it.customerGroupIds && it.id != excludeClassId }

    fun clearTermReference(termId: String) {
        for (index in _classes.indices) {
            val item = _classes[index]
            if (item.spansTerm(termId)) {
                _classes[index] = item.copy(termIds = item.termIds - termId)
            }
        }
    }

    fun clearLocationReference(locationId: String) {
        for (index in _classes.indices) {
            val item = _classes[index]
            if (item.usesLocation(locationId)) {
                _classes[index] = item.copy(locationId = null)
            }
        }
    }

    fun clearCustomerGroupReference(groupId: String) {
        for (index in _classes.indices) {
            val item = _classes[index]
            if (item.hasCustomerGroup(groupId)) {
                _classes[index] = item.copy(customerGroupIds = item.customerGroupIds - groupId)
                PackClassScheduleStore.remove(groupId, item.id)
            }
        }
    }

    fun forSchedulePanel(termFilterId: String? = null): List<ScheduledClass> {
        val filtered = if (termFilterId != null) {
            _classes.filter { it.spansTerm(termFilterId) }
        } else {
            _classes
        }
        return filtered.sortedWith(compareScheduledClasses())
    }

    fun forSchedulingPanel(
        soldPlanId: String? = null,
        termId: String? = null,
        locationId: String? = null,
    ): List<ScheduledClass> {
        soldPlanId?.let { groupId ->
            val scheduledClass = findClassContainingCustomerGroup(groupId)
            return listOfNotNull(scheduledClass)
        }
        var filtered: List<ScheduledClass> = _classes
        termId?.let { id -> filtered = filtered.filter { it.spansTerm(id) } }
        locationId?.let { id -> filtered = filtered.filter { it.usesLocation(id) } }
        return filtered.sortedWith(compareScheduledClasses())
    }

    fun forSchedulePanelFromSoldPlan(soldPlanId: String?): List<ScheduledClass> =
        forSchedulingPanel(soldPlanId = soldPlanId)

    fun customerGroupIdsForTerm(termId: String): Set<String> =
        _classes.filter { it.spansTerm(termId) }
            .flatMap { it.customerGroupIds }
            .toSet()

    fun customerGroupIdsForLocation(locationId: String): Set<String> =
        _classes.filter { it.usesLocation(locationId) }
            .flatMap { it.customerGroupIds }
            .toSet()

    fun termIdsForLocation(locationId: String): Set<String> =
        _classes.filter { it.usesLocation(locationId) }
            .flatMap { it.termIds }
            .toSet()

    fun locationIdsForTerm(termId: String): Set<String> =
        _classes.filter { it.spansTerm(termId) }
            .mapNotNull { it.locationId }
            .toSet()

    fun countForTerm(termId: String): Int = _classes.count { it.spansTerm(termId) }

    fun countForLocation(locationId: String): Int = _classes.count { it.usesLocation(locationId) }
}
