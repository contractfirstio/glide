package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.AttendanceStatus
import glide.model.AttendanceRecord
import glide.model.AttendanceSessionKey
import glide.model.canTakeAttendance
import glide.model.parseIsoLocalDate

object AttendanceStore {
    private val _records = mutableStateListOf<AttendanceRecord>()
    val records: List<AttendanceRecord> get() = _records

    private val _submittedSessions = mutableStateListOf<AttendanceSessionKey>()

    fun isSessionSubmitted(session: AttendanceSessionKey): Boolean =
        _submittedSessions.any {
            it.classId == session.classId && it.sessionDate == session.sessionDate
        }

    fun markSessionSubmitted(session: AttendanceSessionKey) {
        if (isSessionSubmitted(session)) return
        _submittedSessions.add(session)
    }

    fun statusFor(session: AttendanceSessionKey, attendeeKey: String): AttendanceStatus? =
        _records.find {
            it.classId == session.classId &&
                it.sessionDate == session.sessionDate &&
                it.attendeeKey == attendeeKey
        }?.status

    fun setStatus(
        session: AttendanceSessionKey,
        attendeeKey: String,
        status: AttendanceStatus,
    ) {
        if (isSessionSubmitted(session)) return
        val index = _records.indexOfFirst {
            it.classId == session.classId &&
                it.sessionDate == session.sessionDate &&
                it.attendeeKey == attendeeKey
        }
        val record = AttendanceRecord(
            classId = session.classId,
            sessionDate = session.sessionDate,
            attendeeKey = attendeeKey,
            status = status,
        )
        if (index >= 0) {
            _records[index] = record
        } else {
            _records.add(record)
        }
    }

    fun clearStatus(session: AttendanceSessionKey, attendeeKey: String) {
        if (isSessionSubmitted(session)) return
        _records.removeAll {
            it.classId == session.classId &&
                it.sessionDate == session.sessionDate &&
                it.attendeeKey == attendeeKey
        }
    }

    fun recordsForSession(session: AttendanceSessionKey): List<AttendanceRecord> =
        _records.filter {
            it.classId == session.classId && it.sessionDate == session.sessionDate
        }

    fun presentCount(session: AttendanceSessionKey): Int =
        recordsForSession(session).count { it.status == AttendanceStatus.PRESENT }

    fun absentCount(session: AttendanceSessionKey): Int =
        recordsForSession(session).count { it.status == AttendanceStatus.ABSENT }

    fun draftForSession(
        session: AttendanceSessionKey,
        attendeeKeys: List<String>,
    ): Map<String, AttendanceStatus?> =
        attendeeKeys.associateWith { key -> statusFor(session, key) }

    fun unmarkedAttendeeKeys(
        statusByAttendeeKey: Map<String, AttendanceStatus?>,
        requiredAttendeeKeys: List<String>,
    ): List<String> = requiredAttendeeKeys.filter { statusByAttendeeKey[it] == null }

    /**
     * Replaces all attendance for [session]. Returns false if any [requiredAttendeeKeys]
     * lack a present/absent mark.
     */
    fun saveSession(
        session: AttendanceSessionKey,
        statusByAttendeeKey: Map<String, AttendanceStatus?>,
        requiredAttendeeKeys: List<String>,
    ): Boolean {
        if (isSessionSubmitted(session)) return false
        val cls = ClassStore.findById(session.classId) ?: return false
        val sessionDate = parseIsoLocalDate(session.sessionDate) ?: return false
        if (!cls.canTakeAttendance(sessionDate)) return false
        if (unmarkedAttendeeKeys(statusByAttendeeKey, requiredAttendeeKeys).isNotEmpty()) {
            return false
        }
        val savedAtMillis = System.currentTimeMillis()
        requiredAttendeeKeys.forEach { attendeeKey ->
            val status = statusByAttendeeKey[attendeeKey] ?: return false
            val index = _records.indexOfFirst {
                it.classId == session.classId &&
                    it.sessionDate == session.sessionDate &&
                    it.attendeeKey == attendeeKey
            }
            val record = AttendanceRecord(
                classId = session.classId,
                sessionDate = session.sessionDate,
                attendeeKey = attendeeKey,
                status = status,
                recordedAtMillis = savedAtMillis,
            )
            if (index >= 0) {
                _records[index] = record
            } else {
                _records.add(record)
            }
        }
        markSessionSubmitted(session)
        return true
    }

    fun clearForClass(classId: String) {
        _records.removeAll { it.classId == classId }
        _submittedSessions.removeAll { it.classId == classId }
    }
}
