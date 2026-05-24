package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.AttendanceStatus
import glide.model.ClassAttendanceRecord
import glide.model.ClassSessionKey
import glide.model.canTakeAttendance
import glide.model.parseIsoLocalDate

object ClassAttendanceStore {
    private val _records = mutableStateListOf<ClassAttendanceRecord>()
    val records: List<ClassAttendanceRecord> get() = _records

    private val _submittedSessions = mutableStateListOf<ClassSessionKey>()

    fun isSessionSubmitted(session: ClassSessionKey): Boolean =
        _submittedSessions.any {
            it.scheduledClassId == session.scheduledClassId && it.sessionDate == session.sessionDate
        }

    fun markSessionSubmitted(session: ClassSessionKey) {
        if (isSessionSubmitted(session)) return
        _submittedSessions.add(session)
    }

    fun statusFor(session: ClassSessionKey, attendeeKey: String): AttendanceStatus? =
        _records.find {
            it.scheduledClassId == session.scheduledClassId &&
                it.sessionDate == session.sessionDate &&
                it.attendeeKey == attendeeKey
        }?.status

    fun setStatus(
        session: ClassSessionKey,
        attendeeKey: String,
        status: AttendanceStatus,
    ) {
        if (isSessionSubmitted(session)) return
        val index = _records.indexOfFirst {
            it.scheduledClassId == session.scheduledClassId &&
                it.sessionDate == session.sessionDate &&
                it.attendeeKey == attendeeKey
        }
        val record = ClassAttendanceRecord(
            scheduledClassId = session.scheduledClassId,
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

    fun clearStatus(session: ClassSessionKey, attendeeKey: String) {
        if (isSessionSubmitted(session)) return
        _records.removeAll {
            it.scheduledClassId == session.scheduledClassId &&
                it.sessionDate == session.sessionDate &&
                it.attendeeKey == attendeeKey
        }
    }

    fun recordsForSession(session: ClassSessionKey): List<ClassAttendanceRecord> =
        _records.filter {
            it.scheduledClassId == session.scheduledClassId && it.sessionDate == session.sessionDate
        }

    fun presentCount(session: ClassSessionKey): Int =
        recordsForSession(session).count { it.status == AttendanceStatus.PRESENT }

    fun absentCount(session: ClassSessionKey): Int =
        recordsForSession(session).count { it.status == AttendanceStatus.ABSENT }

    fun draftForSession(
        session: ClassSessionKey,
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
        session: ClassSessionKey,
        statusByAttendeeKey: Map<String, AttendanceStatus?>,
        requiredAttendeeKeys: List<String>,
    ): Boolean {
        if (isSessionSubmitted(session)) return false
        val scheduledClass = ScheduledClassStore.findById(session.scheduledClassId) ?: return false
        val sessionDate = parseIsoLocalDate(session.sessionDate) ?: return false
        if (!scheduledClass.canTakeAttendance(sessionDate)) return false
        if (unmarkedAttendeeKeys(statusByAttendeeKey, requiredAttendeeKeys).isNotEmpty()) {
            return false
        }
        val savedAtMillis = System.currentTimeMillis()
        requiredAttendeeKeys.forEach { attendeeKey ->
            val status = statusByAttendeeKey[attendeeKey] ?: return false
            val index = _records.indexOfFirst {
                it.scheduledClassId == session.scheduledClassId &&
                    it.sessionDate == session.sessionDate &&
                    it.attendeeKey == attendeeKey
            }
            val record = ClassAttendanceRecord(
                scheduledClassId = session.scheduledClassId,
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

    fun clearForClass(scheduledClassId: String) {
        _records.removeAll { it.scheduledClassId == scheduledClassId }
        _submittedSessions.removeAll { it.scheduledClassId == scheduledClassId }
    }
}
