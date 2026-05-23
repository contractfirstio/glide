package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.AcademicTerm
import glide.model.findOverlappingTerm

object TermStore {
    private val _terms = mutableStateListOf<AcademicTerm>()
    val terms: List<AcademicTerm> get() = _terms

    fun overlappingTerm(term: AcademicTerm, excludeTermId: String? = term.id): AcademicTerm? =
        findOverlappingTerm(_terms, term, excludeTermId)

    fun create(term: AcademicTerm): Boolean {
        if (overlappingTerm(term, excludeTermId = null) != null) return false
        _terms.add(term)
        return true
    }

    fun update(term: AcademicTerm): Boolean {
        if (overlappingTerm(term, excludeTermId = term.id) != null) return false
        val index = _terms.indexOfFirst { it.id == term.id }
        if (index >= 0) {
            _terms[index] = term
            return true
        }
        return false
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
