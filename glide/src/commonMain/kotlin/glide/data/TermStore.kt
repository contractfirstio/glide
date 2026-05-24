package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.AcademicTerm
import glide.model.compareAcademicTermsChronological
import glide.model.compareAcademicTermsReverseChronological
import glide.model.findOverlappingTerm

object TermStore {
    private val _terms = mutableStateListOf<AcademicTerm>()
    val terms: List<AcademicTerm> get() = _terms

    fun overlappingTerm(term: AcademicTerm, excludeTermId: String? = term.id): AcademicTerm? =
        findOverlappingTerm(_terms, term, excludeTermId)

    fun create(term: AcademicTerm): Boolean {
        if (overlappingTerm(term, excludeTermId = null) != null) return false
        _terms.add(term)
        extendClassesWithRollingGroupsForNewTerm(term)
        return true
    }

    fun update(term: AcademicTerm): Boolean {
        if (!canEdit(term.id)) return false
        if (overlappingTerm(term, excludeTermId = term.id) != null) return false
        val index = _terms.indexOfFirst { it.id == term.id }
        if (index >= 0) {
            _terms[index] = term
            return true
        }
        return false
    }

    fun classCount(termId: String): Int =
        ScheduledClassStore.countForTerm(termId)

    fun canEdit(termId: String): Boolean = classCount(termId) == 0

    fun canDelete(termId: String): Boolean = canEdit(termId)

    fun termEditBlockReason(termId: String): String? {
        val linkedClasses = classCount(termId)
        return if (linkedClasses == 0) {
            null
        } else {
            "This term is used by $linkedClasses class${if (linkedClasses == 1) "" else "es"} " +
                "and cannot be edited."
        }
    }

    fun delete(id: String): Boolean {
        if (!canDelete(id)) return false
        _terms.removeAll { it.id == id }
        return true
    }

    fun findById(id: String): AcademicTerm? = _terms.find { it.id == id }

    /** Reverse chronological order for the Terms panel (latest term first). */
    fun sortedForPanel(): List<AcademicTerm> =
        _terms.sortedWith(compareAcademicTermsReverseChronological())

    /** Chronological order for term calendar navigation (earliest first). */
    fun sortedChronologically(): List<AcademicTerm> =
        _terms.sortedWith(compareAcademicTermsChronological())
}
