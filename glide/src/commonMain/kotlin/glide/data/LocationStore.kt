package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.ClassLocation

object LocationStore {
    private val _locations = mutableStateListOf<ClassLocation>()
    val locations: List<ClassLocation> get() = _locations

    fun create(location: ClassLocation) {
        _locations.add(location)
    }

    fun update(location: ClassLocation) {
        val index = _locations.indexOfFirst { it.id == location.id }
        if (index >= 0) {
            _locations[index] = location
        }
    }

    fun classCount(locationId: String): Int =
        ScheduledClassStore.countForLocation(locationId)

    fun canDelete(locationId: String): Boolean = classCount(locationId) == 0

    fun delete(id: String): Boolean {
        if (!canDelete(id)) return false
        _locations.removeAll { it.id == id }
        return true
    }

    fun findById(id: String): ClassLocation? = _locations.find { it.id == id }

    fun sortedForPanel(): List<ClassLocation> =
        _locations.sortedBy { it.name.lowercase() }
}
