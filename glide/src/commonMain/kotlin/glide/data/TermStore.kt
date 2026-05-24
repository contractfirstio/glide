package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.Term
import glide.model.compareTermsChronological
import glide.model.compareTermsReverseChronological
import glide.model.findOverlappingTerm

object TermStore {
    private val _terms = mutableStateListOf<Term>()
    val terms: List<Term> get() = _terms

    fun overlappingTerm(term: Term, excludeTermId: String? = term.id): Term? =
        findOverlappingTerm(_terms, term, excludeTermId)

    fun create(term: Term): Boolean {
        if (overlappingTerm(term, excludeTermId = null) != null) return false
        _terms.add(term)
        extendClassesWithRollingGroupsForNewTerm(term)
        persistAppData()
        return true
    }

    internal fun replaceAll(terms: List<Term>) {
        _terms.clear()
        _terms.addAll(terms)
    }

    fun update(term: Term): Boolean {
        if (!canEdit(term.id)) return false
        if (overlappingTerm(term, excludeTermId = term.id) != null) return false
        val index = _terms.indexOfFirst { it.id == term.id }
        if (index >= 0) {
            _terms[index] = term
            persistAppData()
            return true
        }
        return false
    }

    fun classCount(termId: String): Int =
        ClassStore.countForTerm(termId)

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
        persistAppData()
        return true
    }

    fun findById(id: String): Term? = _terms.find { it.id == id }

    /** Reverse chronological order for the Terms panel (latest term first). */
    fun sortedForPanel(): List<Term> =
        _terms.sortedWith(compareTermsReverseChronological())

    /** Chronological order for term calendar navigation (earliest first). */
    fun sortedChronologically(): List<Term> =
        _terms.sortedWith(compareTermsChronological())
}
