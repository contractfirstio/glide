package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.AcademicTerm

object TermStore {
    private val _terms = mutableStateListOf<AcademicTerm>()
    val terms: List<AcademicTerm> get() = _terms

    fun create(term: AcademicTerm) {
        _terms.add(term)
    }

    fun update(term: AcademicTerm) {
        val index = _terms.indexOfFirst { it.id == term.id }
        if (index >= 0) {
            _terms[index] = term
        }
    }

    fun delete(id: String) {
        _terms.removeAll { it.id == id }
        ScheduledClassStore.clearTermReference(id)
    }

    fun findById(id: String): AcademicTerm? = _terms.find { it.id == id }

    fun sortedForPanel(): List<AcademicTerm> =
        _terms.sortedWith(compareByDescending<AcademicTerm> { it.startDate }.thenBy { it.name })

    /** Chronological order for term calendar navigation (earliest first). */
    fun sortedChronologically(): List<AcademicTerm> =
        _terms.sortedWith(compareBy<AcademicTerm> { it.startDate }.thenBy { it.name })
}
