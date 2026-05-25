package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Location

object LocationStore {
    private val _locations = mutableStateListOf<Location>()
    val locations: List<Location> get() = _locations

    fun create(location: Location) {
        _locations.add(location)
        persistAppData()
    }

    internal fun replaceAll(locations: List<Location>) {
        _locations.clear()
        _locations.addAll(locations)
    }

    fun update(location: Location) {
        val index = _locations.indexOfFirst { it.id == location.id }
        if (index >= 0) {
            _locations[index] = location
            persistAppData()
        }
    }

    fun classCount(locationId: String): Int =
        ClassStore.countForLocation(locationId)

    fun canDelete(locationId: String): Boolean = classCount(locationId) == 0

    fun delete(id: String): Boolean {
        if (!canDelete(id)) return false
        _locations.removeAll { it.id == id }
        persistAppData()
        return true
    }

    fun findById(id: String): Location? = _locations.find { it.id == id }

    fun sortedForPanel(): List<Location> =
        _locations.sortedBy { it.name.lowercase() }
}
