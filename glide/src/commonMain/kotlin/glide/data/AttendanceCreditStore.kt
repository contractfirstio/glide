package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.AttendanceCredit

object AttendanceCreditStore {
    private val _credits = mutableStateListOf<AttendanceCredit>()

    val all: List<AttendanceCredit> get() = _credits

    fun unappliedForEnrollment(enrollmentId: String): List<AttendanceCredit> =
        _credits.filter { it.enrollmentId == enrollmentId && it.appliedToBillId == null }
            .sortedBy { it.createdAtMillis }

    fun unappliedTotalMinor(enrollmentId: String): Long =
        unappliedForEnrollment(enrollmentId).sumOf { it.amountMinor }

    fun appliedToBill(billId: String): List<AttendanceCredit> =
        _credits.filter { it.appliedToBillId == billId }.sortedBy { it.createdAtMillis }

    fun appliedTotalMinorForBill(billId: String): Long =
        appliedToBill(billId).sumOf { it.amountMinor }

    fun hasCreditForAbsentSession(
        classId: String,
        sessionDate: String,
        attendeeKey: String,
    ): Boolean = _credits.any {
        it.classId == classId &&
            it.sessionDate == sessionDate &&
            it.attendeeKey == attendeeKey
    }

    fun add(credit: AttendanceCredit) {
        if (hasCreditForAbsentSession(credit.classId, credit.sessionDate, credit.attendeeKey)) {
            return
        }
        _credits.add(credit)
        BillStore.applyPendingCreditsToOpenBills(credit.enrollmentId)
    }

    /**
     * Applies whole credits in issue order up to [maxToApplyMinor], then marks them against [billId].
     * Returns the total credit applied.
     */
    fun consumeForBill(enrollmentId: String, billId: String, maxToApplyMinor: Long): Long {
        if (maxToApplyMinor <= 0) return 0L
        var applied = 0L
        val pending = unappliedForEnrollment(enrollmentId).toMutableList()
        val indicesToUpdate = mutableListOf<Pair<Int, AttendanceCredit>>()
        for (credit in pending) {
            if (applied >= maxToApplyMinor) break
            if (applied + credit.amountMinor > maxToApplyMinor) break
            val index = _credits.indexOfFirst { it.id == credit.id }
            if (index >= 0) {
                indicesToUpdate += index to credit.copy(appliedToBillId = billId)
                applied += credit.amountMinor
            }
        }
        indicesToUpdate.forEach { (index, updated) ->
            _credits[index] = updated
        }
        return applied
    }

    fun removeAllForEnrollment(enrollmentId: String) {
        _credits.removeAll { it.enrollmentId == enrollmentId }
    }
}
