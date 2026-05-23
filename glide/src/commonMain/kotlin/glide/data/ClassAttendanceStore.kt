package glide.data

import androidx.compose.runtime.mutableStateListOf
import glide.model.AttendanceStatus
import glide.model.ClassAttendanceRecord
import glide.model.ClassSessionKey

object ClassAttendanceStore {
    private val _records = mutableStateListOf<ClassAttendanceRecord>()
    val records: List<ClassAttendanceRecord> get() = _records

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

    /** Replaces all attendance for [session] with non-null entries in [statusByAttendeeKey]. */
    fun saveSession(session: ClassSessionKey, statusByAttendeeKey: Map<String, AttendanceStatus?>) {
        _records.removeAll {
            it.scheduledClassId == session.scheduledClassId && it.sessionDate == session.sessionDate
        }
        statusByAttendeeKey.forEach { (attendeeKey, status) ->
            if (status != null) {
                _records.add(
                    ClassAttendanceRecord(
                        scheduledClassId = session.scheduledClassId,
                        sessionDate = session.sessionDate,
                        attendeeKey = attendeeKey,
                        status = status,
                    ),
                )
            }
        }
    }

    fun clearForClass(scheduledClassId: String) {
        _records.removeAll { it.scheduledClassId == scheduledClassId }
    }

    fun seed(record: ClassAttendanceRecord) {
        if (_records.none {
                it.scheduledClassId == record.scheduledClassId &&
                    it.sessionDate == record.sessionDate &&
                    it.attendeeKey == record.attendeeKey
            }
        ) {
            _records.add(record)
        }
    }
}
