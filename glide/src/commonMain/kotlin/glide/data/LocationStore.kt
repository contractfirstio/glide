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

    fun delete(id: String) {
        _locations.removeAll { it.id == id }
        ScheduledClassStore.clearLocationReference(id)
    }

    fun findById(id: String): ClassLocation? = _locations.find { it.id == id }

    fun sortedForPanel(): List<ClassLocation> =
        _locations.sortedBy { it.name.lowercase() }
}
