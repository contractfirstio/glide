package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.ScheduledClass
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

    fun delete(id: String) {
        _classes.removeAll { it.id == id }
        ClassGroupAssignmentStore.clearForClass(id)
    }

    fun findById(id: String): ScheduledClass? = _classes.find { it.id == id }

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

    fun forSchedulePanel(termFilterId: String? = null): List<ScheduledClass> {
        val filtered = if (termFilterId != null) {
            _classes.filter { it.spansTerm(termFilterId) }
        } else {
            _classes
        }
        return filtered.sortedWith(
            compareBy<ScheduledClass> { it.dayOfWeek.sortOrder }
                .thenBy { it.startTime }
                .thenBy { it.name },
        )
    }

    fun countForTerm(termId: String): Int = _classes.count { it.spansTerm(termId) }

    fun countForLocation(locationId: String): Int = _classes.count { it.usesLocation(locationId) }
}
