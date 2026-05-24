package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Class
import glide.model.compareClasses
import glide.model.hasSoldPlan
import glide.model.spansTerm
import glide.model.usesLocation

object ClassStore {
    private val _classes = mutableStateListOf<Class>()
    val classes: List<Class> get() = _classes

    fun create(cls: Class) {
        _classes.add(cls)
        persistAppData()
    }

    internal fun replaceAll(classes: List<Class>) {
        _classes.clear()
        _classes.addAll(classes)
    }

    fun update(cls: Class) {
        val index = _classes.indexOfFirst { it.id == cls.id }
        if (index >= 0) {
            _classes[index] = cls
            persistAppData()
        }
    }

    fun soldPlanCount(classId: String): Int =
        findById(classId)?.soldPlanIds?.size ?: 0

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
        AttendanceStore.clearForClass(id)
        SoldPlanClassScheduleStore.clearForClass(id)
        persistAppData()
        return true
    }

    fun findById(id: String): Class? = _classes.find { it.id == id }

    fun findClassContainingSoldPlan(
        soldPlanId: String,
        excludeClassId: String? = null,
    ): Class? =
        _classes.firstOrNull { soldPlanId in it.soldPlanIds && it.id != excludeClassId }

    fun clearTermReference(termId: String) {
        var changed = false
        for (index in _classes.indices) {
            val item = _classes[index]
            if (item.spansTerm(termId)) {
                _classes[index] = item.copy(termIds = item.termIds - termId)
                changed = true
            }
        }
        if (changed) persistAppData()
    }

    fun clearLocationReference(locationId: String) {
        var changed = false
        for (index in _classes.indices) {
            val item = _classes[index]
            if (item.usesLocation(locationId)) {
                _classes[index] = item.copy(locationId = null)
                changed = true
            }
        }
        if (changed) persistAppData()
    }

    fun clearSoldPlanReference(soldPlanId: String) {
        var changed = false
        for (index in _classes.indices) {
            val item = _classes[index]
            if (item.hasSoldPlan(soldPlanId)) {
                _classes[index] = item.copy(soldPlanIds = item.soldPlanIds - soldPlanId)
                SoldPlanClassScheduleStore.remove(soldPlanId, item.id)
                changed = true
            }
        }
        if (changed) persistAppData()
    }

    fun forSchedulePanel(termFilterId: String? = null): List<Class> {
        val filtered = if (termFilterId != null) {
            _classes.filter { it.spansTerm(termFilterId) }
        } else {
            _classes
        }
        return filtered.sortedWith(compareClasses())
    }

    fun forSchedulingPanel(
        soldPlanId: String? = null,
        termId: String? = null,
        locationId: String? = null,
    ): List<Class> {
        soldPlanId?.let { id ->
            val cls = findClassContainingSoldPlan(id)
            return listOfNotNull(cls)
        }
        var filtered: List<Class> = _classes
        termId?.let { id -> filtered = filtered.filter { it.spansTerm(id) } }
        locationId?.let { id -> filtered = filtered.filter { it.usesLocation(id) } }
        return filtered.sortedWith(compareClasses())
    }

    fun forSchedulePanelFromSoldPlan(soldPlanId: String?): List<Class> =
        forSchedulingPanel(soldPlanId = soldPlanId)

    fun soldPlanIdsForTerm(termId: String): Set<String> =
        _classes.filter { it.spansTerm(termId) }
            .flatMap { it.soldPlanIds }
            .toSet()

    fun soldPlanIdsForLocation(locationId: String): Set<String> =
        _classes.filter { it.usesLocation(locationId) }
            .flatMap { it.soldPlanIds }
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
